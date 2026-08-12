package com.fuckbaiduinput;

import android.net.Uri;

/** Wire constants for the module settings Provider and RemotePreferences group. */
public final class HookSettingsContract {
    public static final String MODULE_PACKAGE = "com.fuckbaiduinput";
    public static final String TARGET_PACKAGE = "com.baidu.input_oppo";
    public static final int TARGET_VERSION_CODE = 7244;
    public static final String TARGET_CERT_SHA256 =
            "A6EF817BFD6C083442A149856E51036F6912C2DB6B6009DB8127CDD641E295A9";

    public static final String PROVIDER_AUTHORITY = MODULE_PACKAGE + ".settings";
    public static final Uri PROVIDER_URI = Uri.parse("content://" + PROVIDER_AUTHORITY);
    public static final String BRIDGE_ACTIVITY = MODULE_PACKAGE + ".MainActivity";
    public static final String ACTION_PREPARE_SETTINGS =
            MODULE_PACKAGE + ".action.PREPARE_SETTINGS";
    public static final String EXTRA_DESTINATION = MODULE_PACKAGE + ".extra.DESTINATION";
    public static final String METHOD_SET_FLAGS = "set_flags";
    public static final String EXTRA_FLAGS = "flags";
    public static final String RESULT_OK = "ok";
    public static final String RESULT_REVISION = "revision";
    public static final String RESULT_ERROR = "error";

    public static final String REMOTE_PREFERENCES_GROUP = "hook_settings_v1";
    public static final int SCHEMA_VERSION = 1;
    public static final String SCHEMA_VERSION_KEY = "schema_version";
    public static final String REVISION_KEY = "revision";

    public static final String ERROR_UNSUPPORTED_METHOD = "unsupported_method";
    public static final String ERROR_INVALID_CALLER = "invalid_caller";
    public static final String ERROR_INVALID_PAYLOAD = "invalid_payload";
    public static final String ERROR_SERVICE_UNAVAILABLE = "service_unavailable";
    public static final String ERROR_REMOTE_UNSUPPORTED = "remote_unsupported";
    public static final String ERROR_SCOPE_UNAVAILABLE = "scope_unavailable";
    public static final String ERROR_CORRUPT_PREFERENCES = "corrupt_preferences";
    public static final String ERROR_COMMIT_FAILED = "commit_failed";

    private HookSettingsContract() {
    }
}
