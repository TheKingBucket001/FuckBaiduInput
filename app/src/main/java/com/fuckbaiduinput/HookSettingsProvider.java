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
    private final ModuleDiagnostics diagnostics =
            new ModuleDiagnostics((priority, message) -> Log.println(priority, TAG, message));
    private final FeatureConfiguration.Transactions transactions = new FeatureConfiguration.Transactions();
    private XposedService transactionService;

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

        Map<HookFeature, Boolean> flags = readFlags(extras);
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

    private Bundle commitFlags(Map<HookFeature, Boolean> flags) {
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
                if (transactionService != service) {
                    transactions.newConnection();
                    transactionService = service;
                }
                SharedPreferences preferences = service.getRemotePreferences(
                        HookSettingsContract.REMOTE_PREFERENCES_GROUP);
                if (preferences == null) {
                    return failure(HookSettingsContract.ERROR_SERVICE_UNAVAILABLE);
                }
                Map<String, ?> current = preferences.getAll();
                FeatureConfiguration.WritePlan plan;
                try {
                    plan = transactions.plan(current, flags);
                } catch (IllegalArgumentException invalidState) {
                    transactions.uncertain();
                    diagnostics.error("settings", "metadata_validation", invalidState);
                    return failure(HookSettingsContract.ERROR_CORRUPT_PREFERENCES);
                }
                if (!plan.requiresCommit) {
                    return success(plan.configuration);
                }

                SharedPreferences.Editor editor = preferences.edit();
                if (editor == null) {
                    transactions.failed(plan);
                    diagnostics.warning("settings", "editor_unavailable");
                    return failure(HookSettingsContract.ERROR_COMMIT_FAILED);
                }
                if (plan.clearFirst) {
                    editor.clear();
                }
                editor.putInt(HookSettingsContract.SCHEMA_VERSION_KEY,
                        HookSettingsContract.SCHEMA_VERSION);
                editor.putLong(HookSettingsContract.GENERATION_KEY, plan.configuration.generation());
                editor.putLong(HookSettingsContract.REVISION_KEY, plan.configuration.revision());
                for (HookFeature feature : HookFeature.values()) {
                    editor.putBoolean(feature.key(), plan.configuration.isEnabled(feature));
                }
                try {
                    if (!editor.commit()) {
                        transactions.failed(plan);
                        diagnostics.warning("settings", "commit_returned_false");
                        return failure(HookSettingsContract.ERROR_COMMIT_FAILED);
                    }
                } catch (RuntimeException commitFailure) {
                    transactions.failed(plan);
                    diagnostics.error("settings", "commit", commitFailure);
                    return failure(HookSettingsContract.ERROR_COMMIT_FAILED);
                }
                transactions.committed(plan.configuration);
                return success(plan.configuration);
            }
        } catch (RuntimeException failure) {
            synchronized (WRITE_LOCK) {
                // An old in-flight request cannot invalidate a newer connection's state.
                if (transactionService == service) {
                    transactions.uncertain();
                }
            }
            diagnostics.error("settings", "service_or_read", failure);
            return failure(HookSettingsContract.ERROR_SERVICE_UNAVAILABLE);
        }
    }

    private static Bundle success(FeatureConfiguration configuration) {
        Bundle result = new Bundle();
        result.putBoolean(HookSettingsContract.RESULT_OK, true);
        result.putLong(HookSettingsContract.RESULT_REVISION, configuration.revision());
        result.putBundle(HookSettingsContract.RESULT_SNAPSHOT,
                FeatureSnapshot.acknowledgement(configuration));
        return result;
    }

    private Map<HookFeature, Boolean> readFlags(Bundle extras) {
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
            java.util.LinkedHashMap<HookFeature, Boolean> result = new java.util.LinkedHashMap<>();
            for (String key : bundle.keySet()) {
                if (!HookFeature.isFeatureKey(key)) {
                    return null;
                }
                Object value = bundle.get(key);
                if (!(value instanceof Boolean)) {
                    return null;
                }
                result.put(HookFeature.fromKey(key), (Boolean) value);
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
            return TargetIdentityVerifier.isExpectedTarget(context, callingUid, packages);
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
