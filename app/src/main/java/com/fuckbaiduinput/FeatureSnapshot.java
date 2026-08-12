package com.fuckbaiduinput;

import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Immutable, fail-closed view of a RemotePreferences snapshot. */
public final class FeatureSnapshot {
    private static final FeatureSnapshot DISABLED = new FeatureSnapshot(
            false, 0L, new boolean[HookFeature.values().length]);

    private final boolean schemaValid;
    private final long revision;
    private final boolean[] enabled;

    private FeatureSnapshot(boolean schemaValid, long revision, boolean[] enabled) {
        this.schemaValid = schemaValid;
        this.revision = revision;
        this.enabled = enabled;
    }

    public static FeatureSnapshot disabled() {
        return DISABLED;
    }

    public static FeatureSnapshot from(SharedPreferences preferences) {
        if (preferences == null) {
            return DISABLED;
        }
        try {
            Map<String, ?> values = preferences.getAll();
            Object schema = values.get(HookSettingsContract.SCHEMA_VERSION_KEY);
            Object revision = values.get(HookSettingsContract.REVISION_KEY);
            if (!(schema instanceof Integer)
                    || ((Integer) schema) != HookSettingsContract.SCHEMA_VERSION
                    || !(revision instanceof Long)
                    || ((Long) revision) < 0L) {
                return DISABLED;
            }

            boolean[] enabled = new boolean[HookFeature.values().length];
            HookFeature[] features = HookFeature.values();
            for (int i = 0; i < features.length; i++) {
                Object value = values.get(features[i].key());
                // Missing or malformed feature values are intentionally disabled.
                enabled[i] = value instanceof Boolean && (Boolean) value;
            }
            return new FeatureSnapshot(true, (Long) revision, enabled);
        } catch (RuntimeException ignored) {
            return DISABLED;
        }
    }

    public boolean isSchemaValid() {
        return schemaValid;
    }

    public long revision() {
        return revision;
    }

    public boolean isEnabled(HookFeature feature) {
        return feature != null && enabled[feature.ordinal()];
    }

    public Set<HookFeature> enabledFeatures() {
        EnumSet<HookFeature> result = EnumSet.noneOf(HookFeature.class);
        HookFeature[] features = HookFeature.values();
        for (int i = 0; i < features.length; i++) {
            if (enabled[i]) {
                result.add(features[i]);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    /** Applies a Provider-acknowledged commit without creating another persistent store. */
    public FeatureSnapshot withAcknowledgedChanges(
            Map<HookFeature, Boolean> changes,
            long acknowledgedRevision
    ) {
        if (changes == null || changes.isEmpty() || acknowledgedRevision < 0L) {
            return this;
        }
        if (schemaValid && revision > acknowledgedRevision) {
            return this;
        }
        boolean[] updated = schemaValid
                ? enabled.clone()
                : new boolean[HookFeature.values().length];
        for (Map.Entry<HookFeature, Boolean> entry : changes.entrySet()) {
            HookFeature feature = entry.getKey();
            Boolean value = entry.getValue();
            if (feature != null && value != null) {
                updated[feature.ordinal()] = value;
            }
        }
        return new FeatureSnapshot(true, acknowledgedRevision, updated);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FeatureSnapshot)) {
            return false;
        }
        FeatureSnapshot that = (FeatureSnapshot) other;
        return schemaValid == that.schemaValid
                && revision == that.revision
                && Arrays.equals(enabled, that.enabled);
    }

    @Override
    public int hashCode() {
        int result = Boolean.hashCode(schemaValid);
        result = 31 * result + Long.hashCode(revision);
        result = 31 * result + Arrays.hashCode(enabled);
        return result;
    }
}
