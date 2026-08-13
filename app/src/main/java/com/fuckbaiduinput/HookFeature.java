package com.fuckbaiduinput;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Stable feature identifiers shared by the settings page and the hook process. */
public enum HookFeature {
    CLIPBOARD_CAPACITY("clipboard_capacity"),
    CLIPBOARD_LONG_TEXT("clipboard_long_text"),
    CLIPBOARD_NO_RECOGNITION("clipboard_no_recognition"),

    ACCOUNT_ISOLATION("account_isolation"),
    CLOUD_BACKUP_SYNC("cloud_backup_sync"),
    CLOUD_OPTIMIZATION("cloud_optimization"),
    CLOUD_INPUT("cloud_input"),

    WISDOM_RECOMMENDATION("wisdom_recommendation"),
    SCENARIO_RECOMMENDATION("scenario_recommendation"),

    AD_SDK_BLOCK("ad_sdk_block"),
    SHOP_PROMOTION_CLEANUP("shop_promotion_cleanup"),
    ACTIVITY_RECOMMENDATION("activity_recommendation"),

    PRIVACY_TELEMETRY("privacy_telemetry"),
    FEEDBACK_BLOCK("feedback_block"),
    BACKGROUND_UPDATE_CHECK("background_update_check"),

    ONLINE_SKIN_SHOP_CLEANUP("offline_shop_cleanup"),
    EMOTION_SHOP_CLEANUP("emotion_shop_cleanup"),
    FONT_SHOP_CLEANUP("font_shop_cleanup"),
    REMOTE_SKIN_UPGRADE("remote_skin_upgrade"),
    HIDE_SETTINGS_SKIN_ENTRY("hide_settings_skin_entry"),

    HIDE_SEARCH("hide_search"),
    HIDE_MECHANICAL_KEYBOARD("hide_mechanical_keyboard"),
    HIDE_FONT_SETTING("hide_font_setting"),
    HIDE_AI_WRITER("hide_ai_writer"),
    CUSTOM_KEYBOARD_LOGO("custom_keyboard_logo");

    private static final Map<String, HookFeature> BY_KEY;

    static {
        Map<String, HookFeature> values = new LinkedHashMap<>();
        for (HookFeature feature : values()) {
            values.put(feature.key, feature);
        }
        BY_KEY = Collections.unmodifiableMap(values);
    }

    private final String key;

    HookFeature(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static HookFeature fromKey(String key) {
        return BY_KEY.get(key);
    }

    public static boolean isFeatureKey(String key) {
        return key != null && BY_KEY.containsKey(key);
    }
}
