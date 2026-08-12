package com.fuckbaiduinput;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/** Shared identity gate for host-originated settings IPC. */
final class TargetIdentityVerifier {
    private TargetIdentityVerifier() {
    }

    @SuppressWarnings("deprecation")
    static boolean isExpectedTarget(Context context, int expectedUid) {
        if (context == null || expectedUid < 0) {
            return false;
        }
        try {
            PackageManager packageManager = context.getPackageManager();
            String[] packages = packageManager.getPackagesForUid(expectedUid);
            if (packages == null
                    || packages.length != 1
                    || !HookSettingsContract.TARGET_PACKAGE.equals(packages[0])) {
                return false;
            }

            PackageInfo packageInfo;
            if (Build.VERSION.SDK_INT >= 33) {
                packageInfo = packageManager.getPackageInfo(
                        HookSettingsContract.TARGET_PACKAGE,
                        PackageManager.PackageInfoFlags.of(
                                PackageManager.GET_SIGNING_CERTIFICATES));
            } else if (Build.VERSION.SDK_INT >= 28) {
                packageInfo = packageManager.getPackageInfo(
                        HookSettingsContract.TARGET_PACKAGE,
                        PackageManager.GET_SIGNING_CERTIFICATES);
            } else {
                packageInfo = packageManager.getPackageInfo(
                        HookSettingsContract.TARGET_PACKAGE,
                        PackageManager.GET_SIGNATURES);
            }
            long versionCode = Build.VERSION.SDK_INT >= 28
                    ? packageInfo.getLongVersionCode()
                    : packageInfo.versionCode;
            if (packageInfo.applicationInfo == null
                    || packageInfo.applicationInfo.uid != expectedUid
                    || versionCode != HookSettingsContract.TARGET_VERSION_CODE) {
                return false;
            }

            Signature[] signatures;
            if (Build.VERSION.SDK_INT >= 28 && packageInfo.signingInfo != null) {
                signatures = packageInfo.signingInfo.getApkContentsSigners();
            } else {
                signatures = packageInfo.signatures;
            }
            return signatures != null
                    && signatures.length == 1
                    && HookSettingsContract.TARGET_CERT_SHA256.equals(
                            sha256(signatures[0].toByteArray()));
        } catch (PackageManager.NameNotFoundException | RuntimeException ignored) {
            return false;
        }
    }

    private static String sha256(byte[] value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value);
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(String.format(Locale.ROOT, "%02X", item & 0xff));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException ignored) {
            return "";
        }
    }
}
