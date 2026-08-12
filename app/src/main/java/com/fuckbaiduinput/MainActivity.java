package com.fuckbaiduinput;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

/** One-shot authenticated bridge that makes the settings Provider visible to the host. */
public final class MainActivity extends Activity {
    private static final String TAG = "FuckBaiduInput";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        try {
            Intent intent = getIntent();
            if (intent == null
                    || !HookSettingsContract.ACTION_PREPARE_SETTINGS.equals(intent.getAction())
                    || !HookSettingsContract.TARGET_PACKAGE.equals(getCallingPackage())) {
                return;
            }

            PendingIntent destination;
            if (Build.VERSION.SDK_INT >= 33) {
                destination = intent.getParcelableExtra(
                        HookSettingsContract.EXTRA_DESTINATION,
                        PendingIntent.class
                );
            } else {
                @SuppressWarnings("deprecation")
                PendingIntent legacy = intent.getParcelableExtra(
                        HookSettingsContract.EXTRA_DESTINATION
                );
                destination = legacy;
            }
            if (destination == null
                    || !HookSettingsContract.TARGET_PACKAGE.equals(
                            destination.getCreatorPackage())
                    || !TargetIdentityVerifier.isExpectedTarget(
                            this,
                            destination.getCreatorUid())) {
                return;
            }
            if (Build.VERSION.SDK_INT >= 31
                    && (!destination.isActivity() || !destination.isImmutable())) {
                return;
            }

            int grants = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
            grantUriPermission(
                    HookSettingsContract.TARGET_PACKAGE,
                    HookSettingsContract.PROVIDER_URI,
                    grants
            );
            Log.i(TAG, "settings Provider visibility prepared");
            startIntentSenderForResult(
                    destination.getIntentSender(),
                    -1,
                    null,
                    0,
                    0,
                    0
            );
            setResult(RESULT_OK);
        } catch (IntentSender.SendIntentException | RuntimeException failure) {
            Log.w(TAG, "settings Provider visibility preparation failed", failure);
        } finally {
            finish();
        }
    }
}
