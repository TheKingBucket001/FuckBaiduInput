package com.fuckbaiduinput;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** The single persisted configuration format; independent of Android IPC. */
final class FeatureConfiguration {
    static final int SCHEMA_VERSION = 1;
    static final String SCHEMA_KEY = "schema_version";
    static final String REVISION_KEY = "revision";
    static final String GENERATION_KEY = "configuration_generation";
    private static final HookFeature[] FEATURES = HookFeature.values();
    private static final FeatureConfiguration DISABLED = new FeatureConfiguration(
            false, 0L, 0L, new boolean[FEATURES.length]);

    private final boolean valid;
    private final long generation;
    private final long revision;
    private final boolean[] enabled;

    private FeatureConfiguration(boolean valid, long generation, long revision, boolean[] enabled) {
        this.valid = valid;
        this.generation = generation;
        this.revision = revision;
        this.enabled = enabled;
    }

    static FeatureConfiguration disabled() {
        return DISABLED;
    }

    static FeatureConfiguration from(Map<String, ?> values) {
        if (values == null || values.isEmpty() || invalidReason(values) != null) {
            return DISABLED;
        }
        Object revision = values.get(REVISION_KEY);
        Object generation = values.get(GENERATION_KEY);
        boolean[] enabled = new boolean[FEATURES.length];
        for (HookFeature feature : FEATURES) {
            enabled[feature.ordinal()] = Boolean.TRUE.equals(values.get(feature.key()));
        }
        // Absence is the legacy schema-1 format, not a migration failure.
        return new FeatureConfiguration(true, generation == null ? 0L : (Long) generation,
                (Long) revision, enabled);
    }

    /** Fixed diagnostic categories only; no configuration values or feature names escape. */
    static String invalidReason(Map<String, ?> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        Object schema = values.get(SCHEMA_KEY);
        if (!(schema instanceof Integer) || (Integer) schema != SCHEMA_VERSION) {
            return "invalid_schema";
        }
        Object revision = values.get(REVISION_KEY);
        if (!(revision instanceof Long) || (Long) revision < 0L) {
            return "invalid_revision";
        }
        if (!validGeneration(values)) {
            return "invalid_generation";
        }
        for (HookFeature feature : FEATURES) {
            Object value = values.get(feature.key());
            if (value != null && !(value instanceof Boolean)) {
                return "invalid_feature_type";
            }
        }
        return null;
    }

    private static boolean validGeneration(Map<String, ?> values) {
        if (values == null || !values.containsKey(GENERATION_KEY)) {
            return true;
        }
        Object generation = values.get(GENERATION_KEY);
        return generation instanceof Long && (Long) generation >= 0L;
    }

    boolean isValid() {
        return valid;
    }

    long generation() {
        return generation;
    }

    long revision() {
        return revision;
    }

    boolean isEnabled(HookFeature feature) {
        return feature != null && enabled[feature.ordinal()];
    }

    Set<HookFeature> enabledFeatures() {
        EnumSet<HookFeature> result = EnumSet.noneOf(HookFeature.class);
        for (HookFeature feature : FEATURES) {
            if (isEnabled(feature)) {
                result.add(feature);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    FeatureConfiguration disabledKeepingOrder() {
        return new FeatureConfiguration(false, generation, revision, new boolean[FEATURES.length]);
    }

    /** Invalid reads turn features off but never erase the accepted ordering watermark. */
    FeatureConfiguration accept(FeatureConfiguration incoming) {
        if (!incoming.valid) {
            return disabledKeepingOrder();
        }
        if (incoming.generation < generation
                || (incoming.generation == generation && incoming.revision < revision)) {
            return this;
        }
        return incoming;
    }

    Map<String, Object> values() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(SCHEMA_KEY, SCHEMA_VERSION);
        values.put(GENERATION_KEY, generation);
        values.put(REVISION_KEY, revision);
        for (HookFeature feature : FEATURES) {
            values.put(feature.key(), enabled[feature.ordinal()]);
        }
        return Collections.unmodifiableMap(values);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof FeatureConfiguration)) {
            return false;
        }
        FeatureConfiguration that = (FeatureConfiguration) other;
        return valid == that.valid && generation == that.generation && revision == that.revision
                && Arrays.equals(enabled, that.enabled);
    }

    @Override
    public int hashCode() {
        int result = Boolean.hashCode(valid);
        result = 31 * result + Long.hashCode(generation);
        result = 31 * result + Long.hashCode(revision);
        return 31 * result + Arrays.hashCode(enabled);
    }

    /** Confirms no-op writes only after a successful commit on this service connection. */
    static final class Transactions {
        private FeatureConfiguration lastConfirmed;
        private FeatureConfiguration failedBaseline;
        private boolean needsConfirmation;

        WritePlan plan(Map<String, ?> values, Map<HookFeature, Boolean> changes) {
            if (changes == null || changes.isEmpty()) {
                throw new IllegalArgumentException("empty configuration changes");
            }
            Object rawGeneration = values == null ? null : values.get(GENERATION_KEY);
            if (!validGeneration(values)) {
                throw new IllegalArgumentException("invalid configuration generation");
            }
            FeatureConfiguration current = from(values);
            long generation = rawGeneration == null ? 0L : (Long) rawGeneration;
            boolean reset = !current.valid;
            if (lastConfirmed != null && (generation < lastConfirmed.generation
                    || (generation == lastConfirmed.generation && current.valid
                    && current.revision < lastConfirmed.revision))) {
                generation = lastConfirmed.generation;
                reset = true;
            }
            // A failed Editor may already have changed its local map. Never inherit those
            // unconfirmed feature values into a later request for a different switch.
            FeatureConfiguration baseline = failedBaseline == null ? current : failedBaseline;
            boolean[] updated = reset || !baseline.valid
                    ? new boolean[FEATURES.length] : baseline.enabled.clone();
            for (Map.Entry<HookFeature, Boolean> entry : changes.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    throw new IllegalArgumentException("invalid configuration change");
                }
                updated[entry.getKey().ordinal()] = entry.getValue();
            }
            if (!reset && !needsConfirmation && current.equals(lastConfirmed)
                    && Arrays.equals(updated, current.enabled)) {
                return new WritePlan(current, baseline, false, false);
            }
            long nextRevision;
            if (reset || current.revision == Long.MAX_VALUE) {
                // Empty storage on first use starts generation zero. A repair advances it.
                boolean firstUse = lastConfirmed == null && failedBaseline == null
                        && (values == null || values.isEmpty());
                if (!firstUse && generation == Long.MAX_VALUE) {
                    throw new IllegalArgumentException("configuration generation exhausted");
                }
                generation = firstUse ? 0L : generation + 1L;
                nextRevision = 1L;
            } else {
                nextRevision = current.revision + 1L;
            }
            return new WritePlan(new FeatureConfiguration(true, generation, nextRevision, updated),
                    baseline, true, reset);
        }

        void committed(FeatureConfiguration configuration) {
            lastConfirmed = configuration;
            failedBaseline = null;
            needsConfirmation = false;
        }

        void failed(WritePlan plan) {
            failedBaseline = plan.baseline;
            needsConfirmation = true;
        }

        void uncertain() {
            needsConfirmation = true;
        }

        /** Only a new service instance may discard the previous connection's baseline. */
        void newConnection() {
            lastConfirmed = null;
            failedBaseline = null;
            needsConfirmation = true;
        }
    }

    static final class WritePlan {
        final FeatureConfiguration configuration;
        final FeatureConfiguration baseline;
        final boolean requiresCommit;
        final boolean clearFirst;

        private WritePlan(FeatureConfiguration configuration, FeatureConfiguration baseline,
                boolean requiresCommit, boolean clearFirst) {
            this.configuration = configuration;
            this.baseline = baseline;
            this.requiresCommit = requiresCommit;
            this.clearFirst = clearFirst;
        }
    }
}
