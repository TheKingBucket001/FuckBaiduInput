package com.fuckbaiduinput;

import android.content.SharedPreferences;
import android.os.Bundle;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Immutable, fail-closed view of a RemotePreferences snapshot. */
public final class FeatureSnapshot {
    private static final FeatureSnapshot DISABLED = new FeatureSnapshot(FeatureConfiguration.disabled());
    private final FeatureConfiguration configuration;

    private FeatureSnapshot(FeatureConfiguration configuration) {
        this.configuration = configuration;
    }

    public static FeatureSnapshot disabled() {
        return DISABLED;
    }

    public static FeatureSnapshot from(SharedPreferences preferences) {
        return preferences == null ? DISABLED : fromValues(preferences.getAll());
    }

    static FeatureSnapshot fromValues(Map<String, ?> values) {
        return new FeatureSnapshot(FeatureConfiguration.from(values));
    }

    @SuppressWarnings("deprecation")
    static FeatureSnapshot fromAcknowledgement(Bundle values) {
        if (values == null || values.size() != HookFeature.values().length + 3) {
            return DISABLED;
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (String key : values.keySet()) {
            normalized.put(key, values.get(key));
        }
        for (HookFeature feature : HookFeature.values()) {
            if (!(normalized.get(feature.key()) instanceof Boolean)) {
                return DISABLED;
            }
        }
        if (!(normalized.get(FeatureConfiguration.GENERATION_KEY) instanceof Long)) {
            return DISABLED;
        }
        return fromValues(normalized);
    }

    static Bundle acknowledgement(FeatureConfiguration configuration) {
        Bundle result = new Bundle();
        result.putInt(HookSettingsContract.SCHEMA_VERSION_KEY, HookSettingsContract.SCHEMA_VERSION);
        result.putLong(HookSettingsContract.GENERATION_KEY, configuration.generation());
        result.putLong(HookSettingsContract.REVISION_KEY, configuration.revision());
        for (HookFeature feature : HookFeature.values()) {
            result.putBoolean(feature.key(), configuration.isEnabled(feature));
        }
        return result;
    }

    FeatureSnapshot accept(FeatureSnapshot incoming) {
        FeatureConfiguration accepted = configuration.accept(incoming.configuration);
        return accepted == configuration ? this : new FeatureSnapshot(accepted);
    }

    public boolean isSchemaValid() {
        return configuration.isValid();
    }

    public long generation() {
        return configuration.generation();
    }

    public long revision() {
        return configuration.revision();
    }

    public boolean isEnabled(HookFeature feature) {
        return configuration.isEnabled(feature);
    }

    public Set<HookFeature> enabledFeatures() {
        return configuration.enabledFeatures();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FeatureSnapshot)) {
            return false;
        }
        return configuration.equals(((FeatureSnapshot) other).configuration);
    }

    @Override
    public int hashCode() {
        return configuration.hashCode();
    }
}
