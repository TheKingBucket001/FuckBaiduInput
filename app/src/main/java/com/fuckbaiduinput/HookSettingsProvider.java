package com.fuckbaiduinput;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;

import java.util.List;
import java.util.Map;

import io.github.libxposed.service.XposedService;

/** Narrow, authenticated write bridge from the host settings page to RemotePreferences. */
public final class HookSettingsProvider extends ContentProvider {
    private static final String TAG = "FuckBaiduInput";
    private static final Object WRITE_LOCK = new Object();

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (!HookSettingsContract.METHOD_SET_FLAGS.equals(method) || arg != null) {
            return failure(HookSettingsContract.ERROR_UNSUPPORTED_METHOD);
        }
        if (!isCallerAllowed()) {
            return failure(HookSettingsContract.ERROR_INVALID_CALLER);
        }

        Map<String, Boolean> flags = readFlags(extras);
        if (flags == null || flags.isEmpty()) {
            return failure(HookSettingsContract.ERROR_INVALID_PAYLOAD);
        }

        long token = Binder.clearCallingIdentity();
        try {
            return commitFlags(flags);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private Bundle commitFlags(Map<String, Boolean> flags) {
        XposedService service = HookSettingsApplication.getService();
        if (service == null) {
            return failure(HookSettingsContract.ERROR_SERVICE_UNAVAILABLE);
        }

        try {
            if (service.getApiVersion() < 101) {
                return failure(HookSettingsContract.ERROR_SERVICE_UNAVAILABLE);
            }
            if ((service.getFrameworkProperties() & XposedService.PROP_CAP_REMOTE) == 0L) {
                return failure(HookSettingsContract.ERROR_REMOTE_UNSUPPORTED);
            }
            List<String> scope = service.getScope();
            if (scope == null || !scope.contains(HookSettingsContract.TARGET_PACKAGE)) {
                return failure(HookSettingsContract.ERROR_SCOPE_UNAVAILABLE);
            }

            synchronized (WRITE_LOCK) {
                SharedPreferences preferences = service.getRemotePreferences(
                        HookSettingsContract.REMOTE_PREFERENCES_GROUP);
                if (preferences == null) {
                    return failure(HookSettingsContract.ERROR_SERVICE_UNAVAILABLE);
                }
                Map<String, ?> current = preferences.getAll();
                PreferenceState state = inspectCurrentPreferences(current);
                if (state.revision == Long.MAX_VALUE) {
                    return failure(HookSettingsContract.ERROR_CORRUPT_PREFERENCES);
                }

                SharedPreferences.Editor editor = preferences.edit();
                if (editor == null) {
                    return failure(HookSettingsContract.ERROR_COMMIT_FAILED);
                }
                if (state.resetRequired) {
                    editor.clear();
                }
                editor.putInt(HookSettingsContract.SCHEMA_VERSION_KEY,
                        HookSettingsContract.SCHEMA_VERSION);
                long nextRevision = state.revision + 1L;
                editor.putLong(HookSettingsContract.REVISION_KEY, nextRevision);
                for (HookFeature feature : HookFeature.values()) {
                    Object currentValue = current.get(feature.key());
                    boolean value = currentValue instanceof Boolean && (Boolean) currentValue;
                    Boolean requestedValue = flags.get(feature.key());
                    editor.putBoolean(feature.key(), requestedValue != null ? requestedValue : value);
                }
                try {
                    if (!editor.commit()) {
                        return failure(HookSettingsContract.ERROR_COMMIT_FAILED);
                    }
                } catch (RuntimeException commitFailure) {
                    return failure(HookSettingsContract.ERROR_COMMIT_FAILED);
                }
                Log.i(TAG, "feature settings revision " + nextRevision + " committed");
                Bundle result = new Bundle();
                result.putBoolean(HookSettingsContract.RESULT_OK, true);
                result.putLong(HookSettingsContract.RESULT_REVISION, nextRevision);
                return result;
            }
        } catch (RuntimeException ignored) {
            return failure(HookSettingsContract.ERROR_SERVICE_UNAVAILABLE);
        }
    }

    /** Invalid non-empty state is atomically replaced with an all-off revision-zero baseline. */
    private static PreferenceState inspectCurrentPreferences(Map<String, ?> values) {
        if (values == null || values.isEmpty()) {
            return new PreferenceState(0L, false);
        }
        Object schema = values.get(HookSettingsContract.SCHEMA_VERSION_KEY);
        Object revision = values.get(HookSettingsContract.REVISION_KEY);
        if (!(schema instanceof Integer)
                || ((Integer) schema) != HookSettingsContract.SCHEMA_VERSION
                || !(revision instanceof Long)
                || ((Long) revision) < 0L) {
            return new PreferenceState(0L, true);
        }
        for (HookFeature feature : HookFeature.values()) {
            Object value = values.get(feature.key());
            if (value != null && !(value instanceof Boolean)) {
                return new PreferenceState(0L, true);
            }
        }
        return new PreferenceState((Long) revision, false);
    }

    private static final class PreferenceState {
        final long revision;
        final boolean resetRequired;

        PreferenceState(long revision, boolean resetRequired) {
            this.revision = revision;
            this.resetRequired = resetRequired;
        }
    }

    private Map<String, Boolean> readFlags(Bundle extras) {
        try {
            if (extras == null
                    || extras.size() != 1
                    || !extras.containsKey(HookSettingsContract.EXTRA_FLAGS)) {
                return null;
            }
            Bundle bundle = extras.getBundle(HookSettingsContract.EXTRA_FLAGS);
            if (bundle == null || bundle.isEmpty() || bundle.size() > HookFeature.values().length) {
                return null;
            }
            java.util.LinkedHashMap<String, Boolean> result = new java.util.LinkedHashMap<>();
            for (String key : bundle.keySet()) {
                if (!HookFeature.isFeatureKey(key)) {
                    return null;
                }
                Object value = bundle.get(key);
                if (!(value instanceof Boolean)) {
                    return null;
                }
                result.put(key, (Boolean) value);
            }
            return result;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private boolean isCallerAllowed() {
        Context context = getContext();
        if (context == null) {
            return false;
        }
        int callingUid = Binder.getCallingUid();
        String[] packages;
        try {
            PackageManager packageManager = context.getPackageManager();
            packages = packageManager.getPackagesForUid(callingUid);
            if (packages == null || packages.length != 1) {
                return false;
            }
            if (callingUid == Process.myUid()) {
                return context.getPackageName().equals(packages[0]);
            }
            // UID/package ownership is authoritative here; getCallingPackage() may be null
            // for a ContentResolver.call() issued through an older attribution path.
            if (!HookSettingsContract.TARGET_PACKAGE.equals(packages[0])) {
                return false;
            }
            return TargetIdentityVerifier.isExpectedTarget(context, callingUid);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static Bundle failure(String error) {
        Bundle result = new Bundle();
        result.putBoolean(HookSettingsContract.RESULT_OK, false);
        result.putString(HookSettingsContract.RESULT_ERROR, error);
        return result;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
