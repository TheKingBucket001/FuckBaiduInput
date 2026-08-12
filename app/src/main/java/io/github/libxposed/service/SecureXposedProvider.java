package io.github.libxposed.service;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;

/** Accepts the framework service Binder only from root or system_server. */
public final class SecureXposedProvider extends ContentProvider {
    private static final String TAG = "FuckBaiduInput";
    private static final String METHOD_SEND_BINDER = "SendBinder";
    private static final String EXTRA_BINDER = "binder";

    @Override
    public boolean onCreate() {
        if (Build.VERSION.SDK_INT >= 30
                && getContext() != null
                && getContext().getApplicationInfo().targetSdkVersion >= 30) {
            RemotePreferences.shouldNotifyCleared = true;
        }
        return true;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (!METHOD_SEND_BINDER.equals(method) || arg != null || extras == null) {
            return null;
        }
        int callingUid = Binder.getCallingUid();
        if (callingUid != Process.ROOT_UID && callingUid != Process.SYSTEM_UID) {
            Log.w(TAG, "rejected Xposed service Binder from uid " + callingUid);
            return null;
        }
        IBinder binder = extras.getBinder(EXTRA_BINDER);
        if (binder == null) {
            return null;
        }
        XposedServiceHelper.onBinderReceived(binder);
        Log.i(TAG, "accepted Xposed service Binder from uid " + callingUid);
        return new Bundle();
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
