package com.fuckbaiduinput;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.security.MessageDigest;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import io.github.libxposed.api.XposedInterface.ExceptionMode;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public final class HookEntry extends XposedModule {
    private static final String TAG = "FuckBaiduInput";
    private static final String TARGET_PACKAGE = "com.baidu.input_oppo";
    private static final String SETTINGS_FEEDBACK_KEY = "FEEDBACK";
    private static final String SETTINGS_CIKU_KEY = "ciku";
    private static final String SETTINGS_GENERAL_KEY = "general_setting";
    private static final String SETTINGS_INTENT_RECOMMEND_KEY = "intent_recommend";
    private static final String SETTINGS_SKIN_KEY = "skin";
    private static final String CLOUD_OPTIMIZATION_KEY = "CLOUDOPTIMIZATION";
    private static final String CLOUD_BACKUP_KEY = "cloud_backup";
    private static final String CLOUD_USER_CIKU_ROOT_KEY = "cloud_user_ciku_root";
    private static final String CLOUD_BACKUP_SETTINGS_ROOT_KEY = "cloud_backup_settings_root";
    private static final String CLOUD_INPUT_KEY = "YUNSHURU";
    private static final String PROGRAMMATIC_AD_KEY = "pref_key_personalize_ad";
    private static final String UNIQUE_RESULT_KEY = "unique_result_switch";
    private static final String ACTIVITY_RECOMMENDATION_KEY = "activity_recommendation";
    private static final String CANDIDATE_ADVERTISEMENT_KEY = "cand_advertisement";
    private static final String TOOLBOX_SEARCH = "CLICK_INDEX_SEARCH";
    private static final String TOOLBOX_MECHANICAL_KEYBOARD =
            "CLICK_INDEX_MECHANICAL_KEYBOARD";
    private static final String TOOLBOX_FONT_SETTING = "CLICK_INDEX_FONT_SETTING";
    private static final String TOOLBOX_CLOUD_SYNC = "CLICK_INDEX_SYN";
    private static final String TOOLBOX_FEEDBACK = "CLICK_INDEX_FEEDBACK";
    private static final String TOOLBOX_INTENT_RECOMMEND =
            "CLICK_INDEX_INTENT_RECOMMEND";
    // The OPPO skin exposes two distinct panels that are both labelled “斗图”.
    // Remove both instead of leaving a visually identical entry behind.
    private static final int[] EMOTION_TYPE_DOUTU = { 2, 3 };
    private static final String TURBO_CLOUD_INTENT_FUNCTION = "FUNCTION_CLOUD_INTENT";
    private static final String SETTINGS_PAGE_TYPE_EXTRA = "settype";
    private static final String FEEDBACK_URL_EXTRA = "url";
    private static final String BLANK_WEB_URL = "about:blank";
    private static final String SHOP_SKIN_TAB = "skin_shop";
    private static final String SHOP_EMOTION_TAB = "emotion_shop";
    private static final String SHOP_FONT_TAB = "font_shop";
    private static final String SHOP_MY_TAB = "my_center_shop";
    private static final int SHOP_MASK_SKIN = 1;
    private static final int SHOP_MASK_EMOTION = 1 << 1;
    private static final int SHOP_MASK_FONT = 1 << 2;
    private static final String MY_CENTER_DYNAMIC_PAGE_MARK = "my_center_oem_oppo";
    private static final int CIKU_TITLE_RES_ID = 0x7f1201ea;
    private static final int INTENT_RECOMMEND_TITLE_RES_ID = 0x7f120654;
    private static final int USER_EMOTION_TITLE_RES_ID = 0x7f120e5d;
    private static final int UNIQUE_RESULT_SERVICE_TYPE = 201;
    private static final int TURBO_MODE_GUIDE_LAYOUT_RES_ID = 0x7f0d03b8;
    private static final int VIEW_ID_TURBO_CLOUD_INTENT_SWITCH = 0x7f0a067b;
    private static final int VIEW_ID_TURBO_CLOUD_INTENT_ICON = 0x7f0a06bd;
    private static final int CLOUD_BACKUP_PAGE_TYPE = 16;
    private static final int SEARCH_FUNCTION_OPCODE = 82;
    private static final byte CLOUD_OPTIMIZATION_ROUTE = 12;
    private static final byte CLOUD_BACKUP_ROUTE = 31;
    private static final byte FEEDBACK_ROUTE = 58;
    private static final byte FONT_SETTING_ROUTE = 111;
    private static final byte INTENT_RECOMMEND_ROUTE = 112;
    private static final int VIEW_ID_AD_CONTAINER = 0x7f0a007f;
    private static final int VIEW_ID_CL_FEEDBACK = 0x7f0a02bb;
    private static final int VIEW_ID_CL_HEADER = 0x7f0a02bc;
    private static final int VIEW_ID_CL_LOGIN = 0x7f0a02c0;
    private static final int VIEW_ID_EMOTION_SHOP = 0x7f0a0480;
    private static final int VIEW_ID_EMOTION_STORE = 0x7f0a0482;
    private static final int VIEW_ID_LINE_HELP = 0x7f0a07ab;
    private static final int VIEW_ID_MEMBER_BANNER = 0x7f0a0905;
    private static final int VIEW_ID_SKIN_SHARE_LAYOUT = 0x7f0a0d76;
    private static final int DRAWABLE_PURE_MODE_CAND_ICON_DARK = 0x7f080dd1;
    private static final int DRAWABLE_PURE_MODE_CAND_ICON_NORMAL = 0x7f080dd2;
    private static final String HIDDEN_LOCAL_SKIN_NAME = "\u8b66\u6212\u7ebf";
    private static final int HOST_VERSION_CODE = 7244;
    private static final String HOST_VERSION_NAME = "8.5.302.367";
    private static final String HOST_MANIFEST_SHA256 =
            "29BD64DAB35B776DFCD90301A155EA68AC8F880AA61D5E82C75AC02457F867A8";

    private static final int ORIGINAL_CLIP_COUNT = 0x12c;
    private static final int MAX_CLIP_COUNT = 0x1869f;
    private static final int ORIGINAL_PASTE_TRUNCATE_LENGTH = 0x1b58;
    private static final String ORIGINAL_COUNTER_SUFFIX_ASCII = "/" + ORIGINAL_CLIP_COUNT + ")";
    private static final String MAX_COUNTER_SUFFIX_ASCII = "/" + MAX_CLIP_COUNT + ")";
    private static final String ORIGINAL_COUNTER_SUFFIX_FULL_WIDTH = "/" + ORIGINAL_CLIP_COUNT + "\uff09";
    private static final String MAX_COUNTER_SUFFIX_FULL_WIDTH = "/" + MAX_CLIP_COUNT + "\uff09";
    private static final HookProfile[] HOOK_PROFILES = {
            new HookProfile(
                    "8.5.302.367",
                    "com.baidu.uq1",
                    "com.baidu.dr1",
                    "com.baidu.hq1",
                    "com.baidu.dr1$i"
            )
    };

    /*
     * The remote preference object and listener are deliberately retained for
     * the lifetime of the module instance.  RemotePreferences does not keep a
     * strong reference to listeners, so a field is required to receive live
     * changes without introducing a second local settings store.
     */
    private final AtomicReference<FeatureSnapshot> featureSnapshot =
            new AtomicReference<>(FeatureSnapshot.disabled());
    private final Object featureSnapshotLock = new Object();
    private final Object featureWriteLock = new Object();
    private SharedPreferences featurePreferences;
    private final SharedPreferences.OnSharedPreferenceChangeListener featureListener =
            (preferences, key) -> refreshFeatureSnapshot(preferences);
    private HostSettingsUi hostSettingsUi;
    private final Map<Object, ShopTabState> shopTabStates =
            Collections.synchronizedMap(new WeakHashMap<>());
    /* The emotion panel is retained by the host, so configuration changes must
     * update an already-created shop entry instead of waiting for panel rebuild. */
    private final Map<View, EmotionStoreEntryState> emotionStoreEntries =
            Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<TextView, DoutuTabState> doutuTabs =
            Collections.synchronizedMap(new WeakHashMap<>());
    private volatile Bitmap customKeyboardLogo;
    private volatile int customKeyboardLogoWidth;
    private volatile int customKeyboardLogoHeight;
    private final Set<Activity> feedbackActivities = Collections.synchronizedSet(
            Collections.newSetFromMap(new WeakHashMap<>())
    );

    @Override
    public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        if (!TARGET_PACKAGE.equals(param.getPackageName())) {
            return;
        }

        ClassLoader classLoader = param.getClassLoader();
        if (!isSupportedHostVersion(classLoader, param.getApplicationInfo())) {
            logMessage("unsupported host version in " + param.getPackageName());
            return;
        }
        initializeFeatureSnapshot();
        initializeHostSettingsUi(classLoader);
        HookTargets targets = findMatchingTargets(classLoader);
        if (targets == null) {
            logMessage("no matching hook profile in " + param.getPackageName());
        } else {
            logMessage("loading clipboard profile " + targets.profile.versionName);
            hookClipboardConfig(targets);
            hookClipboardPanel(targets);
            hookPasteTruncation(targets);
            hookRecordLengthFilter(targets);
        }

        hookClipboardRecognition(classLoader);
        hookCleanUi(classLoader);
        hookKeyboardLogoReplacement(classLoader);
        hookModuleSettingsPage(classLoader);
        hookBlockedSettingsRoutes(classLoader);
        hookShopStartup(classLoader);
        hookBackgroundUpdateCheck(classLoader);
        hookRemoteSkinUpgradeCheck(classLoader);
        safe("account isolation", () -> hookAccountIsolation(classLoader));
        hookPrivacyTelemetry(classLoader);
        hookAdSdkGates(classLoader);
        hookCandidateAdvertisement(classLoader);
    }

    private void initializeFeatureSnapshot() {
        try {
            if (getApiVersion() < 101
                    || (getFrameworkProperties() & PROP_CAP_REMOTE) == 0L) {
                logMessage("remote preferences unavailable; all features remain disabled");
                return;
            }
            SharedPreferences preferences = getRemotePreferences(
                    HookSettingsContract.REMOTE_PREFERENCES_GROUP);
            if (preferences == null) {
                logMessage("remote preferences returned null; all features remain disabled");
                return;
            }
            if (featurePreferences != preferences) {
                if (featurePreferences != null) {
                    featurePreferences.unregisterOnSharedPreferenceChangeListener(featureListener);
                }
                featurePreferences = preferences;
                preferences.registerOnSharedPreferenceChangeListener(featureListener);
            }
            refreshFeatureSnapshot(preferences);
        } catch (Throwable t) {
            featureSnapshot.set(FeatureSnapshot.disabled());
            logMessage("remote preferences initialization failed: " + t);
        }
    }

    private void refreshFeatureSnapshot(SharedPreferences preferences) {
        try {
            FeatureSnapshot refreshed = FeatureSnapshot.from(preferences);
            if (!refreshed.isSchemaValid()) {
                featureSnapshot.set(FeatureSnapshot.disabled());
                return;
            }
            applyFeatureSnapshot(refreshed);
        } catch (Throwable t) {
            featureSnapshot.set(FeatureSnapshot.disabled());
            logMessage("feature snapshot refresh failed: " + t);
        }
    }

    private void applyFeatureSnapshot(FeatureSnapshot refreshed) {
        boolean finishFeedback;
        long revision;
        synchronized (featureSnapshotLock) {
            FeatureSnapshot current = featureSnapshot.get();
            if (current.isSchemaValid() && refreshed.revision() < current.revision()) {
                return;
            }
            finishFeedback = !current.isEnabled(HookFeature.FEEDBACK_BLOCK)
                    && refreshed.isEnabled(HookFeature.FEEDBACK_BLOCK);
            featureSnapshot.set(refreshed);
            revision = refreshed.revision();
        }
        if (finishFeedback) {
            finishTrackedFeedbackActivities(revision);
        }
        synchronizeEmotionStoreEntries();
        synchronizeDoutuTabs();
    }

    private boolean enabled(HookFeature feature) {
        return featureSnapshot.get().isEnabled(feature);
    }

    private void initializeHostSettingsUi(ClassLoader classLoader) {
        try {
            hostSettingsUi = HostSettingsUi.resolve(
                    classLoader,
                    new HostSettingsUi.FeatureAccess() {
                        @Override
                        public boolean isEnabled(HookFeature feature) {
                            return enabled(feature);
                        }

                        @Override
                        public boolean writeFlags(
                                Context context,
                                Map<HookFeature, Boolean> flags
                        ) {
                            return writeFeatureFlags(context, flags);
                        }
                    },
                    this::logMessage
            );
        } catch (Throwable t) {
            hostSettingsUi = null;
            logMessage("host settings UI resolution failed: " + t);
        }
    }

    private boolean writeFeatureFlags(
            Context context,
            Map<HookFeature, Boolean> flags
    ) {
        if (context == null
                || !TARGET_PACKAGE.equals(context.getPackageName())
                || flags == null
                || flags.isEmpty()) {
            return false;
        }
        synchronized (featureWriteLock) {
            return writeFeatureFlagsLocked(context, flags);
        }
    }

    private boolean writeFeatureFlagsLocked(
            Context context,
            Map<HookFeature, Boolean> flags
    ) {
        try {
            Bundle values = new Bundle();
            for (Map.Entry<HookFeature, Boolean> entry : flags.entrySet()) {
                HookFeature feature = entry.getKey();
                Boolean value = entry.getValue();
                if (feature == null || value == null) {
                    return false;
                }
                values.putBoolean(feature.key(), value);
            }
            Bundle extras = new Bundle();
            extras.putBundle(HookSettingsContract.EXTRA_FLAGS, values);
            Bundle response = context.getContentResolver().call(
                    HookSettingsContract.PROVIDER_URI,
                    HookSettingsContract.METHOD_SET_FLAGS,
                    null,
                    extras
            );
            if (response == null
                    || !response.getBoolean(HookSettingsContract.RESULT_OK, false)) {
                String error = response == null
                        ? "null_response"
                        : response.getString(HookSettingsContract.RESULT_ERROR, "unknown");
                logMessage("feature settings commit rejected: " + error);
                return false;
            }
            long revision = response.getLong(HookSettingsContract.RESULT_REVISION, -1L);
            if (revision < 1L) {
                logMessage("feature settings commit returned invalid revision");
                return false;
            }
            FeatureSnapshot acknowledged;
            synchronized (featureSnapshotLock) {
                acknowledged = featureSnapshot.get().withAcknowledgedChanges(flags, revision);
            }
            applyFeatureSnapshot(acknowledged);
            return true;
        } catch (Throwable t) {
            logMessage("feature settings commit failed: " + t);
            return false;
        }
    }

    private boolean isSupportedHostVersion(
            ClassLoader classLoader,
            ApplicationInfo applicationInfo
    ) {
        try {
            if (!hasExpectedHostManifest(applicationInfo)) {
                return false;
            }
            Class<?> hostInfoClass = findClass(classLoader, "com.baidu.ad6");
            Method getVersionName = findMethod(
                    hostInfoClass,
                    "getVersionName",
                    String.class
            );
            Object hostInfo = hostInfoClass.getDeclaredConstructor().newInstance();
            return HOST_VERSION_NAME.equals(getVersionName.invoke(hostInfo));
        } catch (Throwable t) {
            logMessage("host version gate failed: " + t);
            return false;
        }
    }

    private boolean hasExpectedHostManifest(ApplicationInfo applicationInfo) {
        if (applicationInfo == null
                || !TARGET_PACKAGE.equals(applicationInfo.packageName)
                || applicationInfo.sourceDir == null) {
            return false;
        }
        try (JarFile apk = new JarFile(applicationInfo.sourceDir, false)) {
            JarEntry manifest = apk.getJarEntry("AndroidManifest.xml");
            if (manifest == null) {
                return false;
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            try (InputStream input = apk.getInputStream(manifest)) {
                int read;
                while ((read = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            return HOST_MANIFEST_SHA256.equals(toUpperHex(digest.digest()));
        } catch (Throwable t) {
            logMessage("host manifest gate failed: " + t);
            return false;
        }
    }

    private static String toUpperHex(byte[] value) {
        char[] hex = "0123456789ABCDEF".toCharArray();
        char[] result = new char[value.length * 2];
        for (int i = 0; i < value.length; i++) {
            int item = value[i] & 0xff;
            result[i * 2] = hex[item >>> 4];
            result[i * 2 + 1] = hex[item & 0x0f];
        }
        return new String(result);
    }

    private HookTargets findMatchingTargets(ClassLoader classLoader) {
        for (HookProfile profile : HOOK_PROFILES) {
            try {
                return profile.resolve(classLoader);
            } catch (Throwable ignored) {
                // Try the next known obfuscation profile.
            }
        }
        return null;
    }

    private void hookClipboardConfig(final HookTargets targets) {
        safe(targets.profile.configClassName + ".a", () -> hook(targets.getClipCountMethod)
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object result = chain.proceed();
                    if (enabled(HookFeature.CLIPBOARD_CAPACITY)
                            && shouldLiftClipCount(result)) {
                        return MAX_CLIP_COUNT;
                    }
                    return result;
                }));

        safe(targets.profile.configClassName + ".d", () -> hook(targets.loadConfigMethod)
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object result = chain.proceed();
                    if (enabled(HookFeature.CLIPBOARD_CAPACITY)) {
                        enforceClipCountField(chain.getThisObject(), targets);
                    }
                    return result;
                }));

        safe(targets.profile.configClassName + ".e", () -> hook(targets.setClipCountMethod)
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object result = chain.proceed();
                    if (enabled(HookFeature.CLIPBOARD_CAPACITY)
                            && shouldLiftClipCount(chain.getArg(0))) {
                        // Keep the host setter's original argument so its
                        // persistent key_clip_count value is never rewritten.
                        try {
                            targets.clipCountField.setInt(chain.getThisObject(), MAX_CLIP_COUNT);
                        } catch (Throwable t) {
                            logMessage("failed to lift in-memory clip count after setter: " + t);
                        }
                    }
                    return result;
                }));
    }

    private void hookClipboardPanel(final HookTargets targets) {
        safe(targets.profile.panelClassName + ".O", () -> hook(targets.updatePanelMethod)
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object result = chain.proceed();
                    if (enabled(HookFeature.CLIPBOARD_CAPACITY)) {
                        updateCounterText(chain.getThisObject(), targets);
                    }
                    return result;
                }));
    }

    private void hookPasteTruncation(final HookTargets targets) {
        safe(targets.profile.pasteClassName + ".r", () -> hook(targets.pasteTruncateMethod)
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    String input = (String) chain.getArg(0);
                    if (enabled(HookFeature.CLIPBOARD_LONG_TEXT)
                            && input != null
                            && input.length() > ORIGINAL_PASTE_TRUNCATE_LENGTH) {
                        return input;
                    }
                    return chain.proceed();
                }));
    }

    private void hookRecordLengthFilter(final HookTargets targets) {
        safe(targets.profile.recordFilterClassName + ".q", () -> hook(targets.recordLengthFilterMethod)
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> enabled(HookFeature.CLIPBOARD_LONG_TEXT)
                        ? null
                        : chain.proceed()));
    }

    private void hookClipboardRecognition(ClassLoader classLoader) {
        safe("clipboard semantic recognition", () -> {
            Class<?> recognizerClass = findClass(
                    classLoader,
                    "com.baidu.input.ime.front.recognition.g"
            );
            Class<?> recognitionResultClass = findClass(classLoader, "com.baidu.f7d");
            Method recognizeClipboardText = findMethod(
                    recognizerClass,
                    "d",
                    recognitionResultClass,
                    String.class
            );

            installConstantHook(
                    recognizeClipboardText,
                    HookFeature.CLIPBOARD_NO_RECOGNITION,
                    null
            );
        });
    }

    private void hookCleanUi(ClassLoader classLoader) {
        hookSettingsRootCleanup(classLoader);
        hookCloudDictionarySettings(classLoader);
        hookCloudBackupSubpage(classLoader);
        hookCloudInputSetting(classLoader);
        hookCloudInputCapability(classLoader);
        hookToolboxMenuCleanup(classLoader);
        hookToolboxClickGuard(classLoader);
        hookLegacyToolboxClickGuard(classLoader);
        hookSearchFunctionGuard(classLoader);
        hookMechanicalKeyboardActivityGuard(classLoader);
        hookIntentRecommendation(classLoader);
        hookPureModeDialogCleanup(classLoader);
        hookTurboModeWisdomCleanup(classLoader);
        hookProgrammaticAdSetting(classLoader);
        hookAdvancedRecommendationSettings(classLoader);
        hookScenarioRecommendationCapability(classLoader);
        hookSettingsSearchResults(classLoader);
        hookMyCenterPage(classLoader);
        hookMyCenterDynamicPage(classLoader);
        hookShopTabs(classLoader);
        hookLocalSkinDetailCleanup(classLoader);
        hookLocalSkinListCleanup(classLoader);
        hookEmotionStoreEntryCleanup(classLoader);
        hookDoutuPageCleanup(classLoader);
        hookAiWriterEntryCleanup(classLoader);
    }

    private void hookSettingsRootCleanup(ClassLoader classLoader) {
        safe("settings root cleanup", () -> {
            Class<?> settingsFragmentClass = findClass(classLoader, "com.baidu.y6b");
            Class<?> preferenceFragmentClass = findClass(classLoader, "com.baidu.pn0");
            Class<?> androidXPreferenceFragmentClass = findClass(
                    classLoader,
                    "androidx.preference.d"
            );
            Class<?> preferenceClass = findClass(classLoader, "androidx.preference.Preference");
            Method createPreferencesMethod = findMethod(
                    settingsFragmentClass,
                    "H",
                    void.class,
                    Bundle.class,
                    String.class
            );
            Method removePreferenceMethod = findMethod(
                    preferenceFragmentClass,
                    "i0",
                    void.class,
                    String.class
            );
            Method findPreferenceMethod = findMethod(
                    androidXPreferenceFragmentClass,
                    "r",
                    preferenceClass,
                    CharSequence.class
            );
            Method setTitleResourceMethod = findMethod(
                    preferenceClass,
                    "m1",
                    void.class,
                    int.class
            );
            Method getSummaryMethod = findMethod(
                    preferenceClass,
                    "g0",
                    CharSequence.class
            );
            Method setSummaryMethod = findMethod(
                    preferenceClass,
                    "k1",
                    void.class,
                    CharSequence.class
            );

            hook(createPreferencesMethod)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        Object fragment = chain.getThisObject();
                        if (enabled(HookFeature.FEEDBACK_BLOCK)) {
                            removeHostPreference(
                                    fragment,
                                    SETTINGS_FEEDBACK_KEY,
                                    removePreferenceMethod
                            );
                        }
                        if (enabled(HookFeature.WISDOM_RECOMMENDATION)) {
                            removeHostPreference(
                                    fragment,
                                    SETTINGS_INTENT_RECOMMEND_KEY,
                                    removePreferenceMethod
                            );
                        }
                        if (enabled(HookFeature.HIDE_SETTINGS_SKIN_ENTRY)) {
                            removeHostPreference(
                                    fragment,
                                    SETTINGS_SKIN_KEY,
                                    removePreferenceMethod
                            );
                        }
                        if (enabled(HookFeature.CLOUD_BACKUP_SYNC)
                                || enabled(HookFeature.CLOUD_OPTIMIZATION)
                                || enabled(HookFeature.CLOUD_INPUT)) {
                            updateSettingsRootLabels(
                                    fragment,
                                    findPreferenceMethod,
                                    setTitleResourceMethod,
                                    getSummaryMethod,
                                    setSummaryMethod
                            );
                        }
                        HostSettingsUi settingsUi = hostSettingsUi;
                        if (settingsUi != null) {
                            settingsUi.injectRootEntry(fragment);
                        }
                        return result;
                    });
        });
    }

    private void hookModuleSettingsPage(ClassLoader classLoader) {
        HostSettingsUi settingsUi = hostSettingsUi;
        if (settingsUi == null) {
            logMessage("module settings page hook skipped: UI targets unavailable");
            return;
        }
        safe("module settings page", () -> {
            Class<?> settingsPageClass = findClass(classLoader, "com.baidu.tkb");
            Method createPreferencesMethod = findMethod(
                    settingsPageClass,
                    "H",
                    void.class,
                    Bundle.class,
                    String.class
            );
            hook(createPreferencesMethod)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        settingsUi.populateMarkedPage(chain.getThisObject());
                        return result;
                    });
        });
        Method categoryBindMethod = settingsUi.categoryBindMethod();
        if (categoryBindMethod != null) {
            safe("module settings compact category", () -> hook(categoryBindMethod)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        settingsUi.compactBatchCategory(
                                chain.getThisObject(),
                                chain.getArg(0)
                        );
                        return result;
                    }));
        }
    }

    private void hookCloudDictionarySettings(ClassLoader classLoader) {
        safe("cloud dictionary settings cleanup", () -> {
            Class<?> dictionaryFragmentClass = findClass(classLoader, "com.baidu.ro1");
            Class<?> preferenceFragmentClass = findClass(classLoader, "com.baidu.pn0");
            Class<?> androidXPreferenceFragmentClass = findClass(
                    classLoader,
                    "androidx.preference.d"
            );
            Class<?> preferenceClass = findClass(classLoader, "androidx.preference.Preference");
            Class<?> preferenceGroupClass = findClass(
                    classLoader,
                    "androidx.preference.PreferenceGroup"
            );
            Method createPreferencesMethod = findMethod(
                    dictionaryFragmentClass,
                    "H",
                    void.class,
                    Bundle.class,
                    String.class
            );
            Method removePreferenceMethod = findMethod(
                    preferenceFragmentClass,
                    "i0",
                    void.class,
                    String.class
            );
            Method findPreferenceMethod = findMethod(
                    androidXPreferenceFragmentClass,
                    "r",
                    preferenceClass,
                    CharSequence.class
            );
            Method getParentMethod = findMethod(
                    preferenceClass,
                    "X",
                    preferenceGroupClass
            );
            Method getChildCountMethod = findMethod(
                    preferenceGroupClass,
                    "z1",
                    int.class
            );
            Method getChildMethod = findMethod(
                    preferenceGroupClass,
                    "y1",
                    preferenceClass,
                    int.class
            );
            Method removeChildMethod = findMethod(
                    preferenceGroupClass,
                    "D1",
                    boolean.class,
                    preferenceClass
            );

            hook(createPreferencesMethod)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        Object fragment = chain.getThisObject();
                        if (enabled(HookFeature.CLOUD_BACKUP_SYNC)) {
                            removePreferenceOrOnlyCategory(
                                    fragment,
                                    CLOUD_BACKUP_KEY,
                                    preferenceGroupClass,
                                    findPreferenceMethod,
                                    removePreferenceMethod,
                                    getParentMethod,
                                    getChildCountMethod,
                                    getChildMethod,
                                    removeChildMethod
                            );
                        }
                        if (enabled(HookFeature.CLOUD_OPTIMIZATION)) {
                            removeHostPreference(
                                    fragment,
                                    CLOUD_OPTIMIZATION_KEY,
                                    removePreferenceMethod
                            );
                        }
                        return result;
                    });
        });
    }

    private void hookCloudBackupSubpage(ClassLoader classLoader) {
        safe("cloud backup subpage cleanup", () -> {
            Class<?> cloudFragmentClass = findClass(classLoader, "com.baidu.ss1");
            Class<?> preferenceFragmentClass = findClass(classLoader, "com.baidu.pn0");
            Method createPreferencesMethod = findMethod(
                    cloudFragmentClass,
                    "H",
                    void.class,
                    Bundle.class,
                    String.class
            );
            Method removePreferenceMethod = findMethod(
                    preferenceFragmentClass,
                    "i0",
                    void.class,
                    String.class
            );

            hook(createPreferencesMethod)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        Object fragment = chain.getThisObject();
                        if (enabled(HookFeature.CLOUD_BACKUP_SYNC)) {
                            removeHostPreference(
                                    fragment,
                                    CLOUD_USER_CIKU_ROOT_KEY,
                                    removePreferenceMethod
                            );
                            removeHostPreference(
                                    fragment,
                                    CLOUD_BACKUP_SETTINGS_ROOT_KEY,
                                    removePreferenceMethod
                            );
                        }
                        return result;
                    });
        });
    }

    private void hookCloudInputSetting(ClassLoader classLoader) {
        safe("cloud input setting cleanup", () -> {
            Class<?> generalFragmentClass = findClass(classLoader, "com.baidu.kk4");
            Class<?> androidXPreferenceFragmentClass = findClass(
                    classLoader,
                    "androidx.preference.d"
            );
            Class<?> preferenceClass = findClass(classLoader, "androidx.preference.Preference");
            Class<?> cloudInputPreferenceClass = findClass(
                    classLoader,
                    "com.baidu.input.pref.OppoYunshuruPref"
            );
            Method createPreferencesMethod = findMethod(
                    generalFragmentClass,
                    "H",
                    void.class,
                    Bundle.class,
                    String.class
            );
            Method findPreferenceMethod = findMethod(
                    androidXPreferenceFragmentClass,
                    "r",
                    preferenceClass,
                    CharSequence.class
            );
            Field visibleField = findField(preferenceClass, "y", boolean.class);
            Method notifyHierarchyChangedMethod = findMethod(
                    preferenceClass,
                    "u0",
                    void.class
            );

            hook(createPreferencesMethod)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (enabled(HookFeature.CLOUD_INPUT)) {
                            hideCloudInputPreference(
                                    chain.getThisObject(),
                                    cloudInputPreferenceClass,
                                    findPreferenceMethod,
                                    visibleField,
                                    notifyHierarchyChangedMethod
                            );
                        }
                        return result;
                    });
        });
    }

    private void hookCloudInputCapability(ClassLoader classLoader) {
        safe("cloud input capability", () -> {
            Class<?> cloudInputConfigClass = findClass(classLoader, "com.baidu.ps1");
            Method isCloudInputEnabled = findMethod(
                    cloudInputConfigClass,
                    "j",
                    boolean.class
            );

            installConstantHook(isCloudInputEnabled, HookFeature.CLOUD_INPUT, false);
        });
    }

    private void hookToolboxMenuCleanup(ClassLoader classLoader) {
        safe("toolbox menu cleanup", () -> {
            Class<?> menuPagerClass = findClass(classLoader, "com.baidu.m6e");
            Class<?> menuItemClass = findClass(classLoader, "com.baidu.sq5");
            Class<?> menuFunctionClass = findClass(
                    classLoader,
                    "com.baidu.input.menutoolapi.data.MenuFunction"
            );
            Class<?> callbackClass = findClass(classLoader, "com.baidu.zf4");
            Method buildMenuMethod = findMethod(
                    menuPagerClass,
                    "b",
                    View.class,
                    List.class,
                    boolean.class,
                    callbackClass
            );
            Method getMenuFunctionMethod = findMethod(
                    menuItemClass,
                    "d",
                    menuFunctionClass
            );

            hook(buildMenuMethod)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        if (!hasToolboxFeatureEnabled()) {
                            return chain.proceed();
                        }
                        List<Object> filtered = filterToolboxItems(
                                chain.getArg(0),
                                getMenuFunctionMethod
                        );
                        if (filtered == null) {
                            return chain.proceed();
                        }
                        return chain.proceed(new Object[] {
                                filtered,
                                chain.getArg(1),
                                chain.getArg(2)
                        });
                    });
        });
    }

    private void hookProgrammaticAdSetting(ClassLoader classLoader) {
        safe("programmatic ad setting cleanup", () -> {
            Class<?> privacyFragmentClass = findClass(classLoader, "com.baidu.j55");
            Class<?> preferenceFragmentClass = findClass(classLoader, "com.baidu.pn0");
            Method createPreferencesMethod = findMethod(
                    privacyFragmentClass,
                    "H",
                    void.class,
                    Bundle.class,
                    String.class
            );
            Method removePreferenceMethod = findMethod(
                    preferenceFragmentClass,
                    "i0",
                    void.class,
                    String.class
            );

            hook(createPreferencesMethod)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (enabled(HookFeature.AD_SDK_BLOCK)) {
                            removeHostPreference(
                                    chain.getThisObject(),
                                    PROGRAMMATIC_AD_KEY,
                                    removePreferenceMethod
                            );
                        }
                        return result;
                    });
        });
    }

    private void hookToolboxClickGuard(ClassLoader classLoader) {
        safe("toolbox click guard", () -> {
            Class<?> menuClickListenerClass = findClass(classLoader, "com.baidu.ug8");
            Class<?> menuItemClass = findClass(classLoader, "com.baidu.sq5");
            Class<?> menuFunctionClass = findClass(
                    classLoader,
                    "com.baidu.input.menutoolapi.data.MenuFunction"
            );
            Method handleMenuClick = findMethod(
                    menuClickListenerClass,
                    "a",
                    void.class,
                    menuItemClass,
                    boolean.class
            );
            Method getMenuFunctionMethod = findMethod(
                    menuItemClass,
                    "d",
                    menuFunctionClass
            );

            hook(handleMenuClick)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        if (!hasToolboxFeatureEnabled()) {
                            return chain.proceed();
                        }
                        return shouldBlockToolboxClick(
                                chain.getArg(0),
                                getMenuFunctionMethod
                        ) ? null : chain.proceed();
                    });
        });
    }

    private void hookLegacyToolboxClickGuard(ClassLoader classLoader) {
        safe("legacy toolbox click guard", () -> {
            Class<?> legacyMenuItemClass = findClass(classLoader, "com.baidu.zf8");
            Class<?> menuFunctionClass = findClass(
                    classLoader,
                    "com.baidu.input.menutoolapi.data.MenuFunction"
            );
            Method handleMenuClick = findMethod(
                    legacyMenuItemClass,
                    "g",
                    void.class
            );
            Method getMenuFunctionMethod = findMethod(
                    legacyMenuItemClass,
                    "d",
                    menuFunctionClass
            );

            hook(handleMenuClick)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        if (!hasToolboxFeatureEnabled()) {
                            return chain.proceed();
                        }
                        return shouldBlockToolboxClick(
                                chain.getThisObject(),
                                getMenuFunctionMethod
                        ) ? null : chain.proceed();
                    });
        });
    }

    private void hookSearchFunctionGuard(ClassLoader classLoader) {
        safe("search function opcode guard", () -> {
            Class<?> inputEventHandlerClass = findClass(classLoader, "com.baidu.ry6");
            Method handleFunction = findMethod(
                    inputEventHandlerClass,
                    "l1",
                    boolean.class,
                    int.class
            );

            hook(handleFunction)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        if (!enabled(HookFeature.HIDE_SEARCH)) {
                            return chain.proceed();
                        }
                        Object opcode = chain.getArg(0);
                        if (opcode instanceof Integer
                                && (Integer) opcode == SEARCH_FUNCTION_OPCODE) {
                            return true;
                        }
                        return chain.proceed();
                    });
        });
    }

    private void hookMechanicalKeyboardActivityGuard(ClassLoader classLoader) {
        safe("mechanical keyboard activity restore guard", () -> {
            Class<?> mechanicalKeyboardActivityClass = findClass(
                    classLoader,
                    "com.baidu.input.simulation.MechanicalKeyboardActivity"
            );
            Method createActivity = findMethod(
                    mechanicalKeyboardActivityClass,
                    "onCreate",
                    void.class,
                    Bundle.class
            );

            hook(createActivity)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (enabled(HookFeature.HIDE_MECHANICAL_KEYBOARD)) {
                            ((Activity) chain.getThisObject()).finish();
                        }
                        return result;
                    });
        });
    }

    private void hookIntentRecommendation(ClassLoader classLoader) {
        safe("wisdom recommendation capability", () -> {
            Class<?> cloudIntentManagerClass = findClass(
                    classLoader,
                    "com.baidu.input.ime.intent.CloudIntentManager"
            );
            Method isRecommendationEnabled = findMethod(
                    cloudIntentManagerClass,
                    "t",
                    boolean.class
            );

            installConstantHook(
                    isRecommendationEnabled,
                    HookFeature.WISDOM_RECOMMENDATION,
                    false
            );
        });

        safe("wisdom recommendation activity restore guard", () -> {
            Class<?> recommendationActivityClass = findClass(
                    classLoader,
                    "com.baidu.input.ImeCloudIntentRecommendActivity"
            );
            Method createActivity = findMethod(
                    recommendationActivityClass,
                    "onCreate",
                    void.class,
                    Bundle.class
            );

            hook(createActivity)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (enabled(HookFeature.WISDOM_RECOMMENDATION)) {
                            ((Activity) chain.getThisObject()).finish();
                        }
                        return result;
                    });
        });
    }

    private void hookPureModeDialogCleanup(ClassLoader classLoader) {
        safe("pure-mode wisdom item cleanup", () -> {
            Class<?> functionAdapterClass = findClass(classLoader, "com.baidu.dh4");
            Class<?> functionItemClass = findClass(classLoader, "com.baidu.fh4");
            Constructor<?> functionAdapterConstructor = findConstructor(
                    functionAdapterClass,
                    Context.class,
                    List.class
            );
            Method getTextResource = findMethod(functionItemClass, "b", int.class);

            hook(functionAdapterConstructor)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        if (!enabled(HookFeature.WISDOM_RECOMMENDATION)) {
                            return chain.proceed();
                        }
                        List<Object> filtered = filterPureModeFunctionItems(
                                chain.getArg(1),
                                functionItemClass,
                                getTextResource
                        );
                        if (filtered == null) {
                            return chain.proceed();
                        }
                        return chain.proceed(new Object[] { chain.getArg(0), filtered });
                    });
        });
    }

    private void hookTurboModeWisdomCleanup(ClassLoader classLoader) {
        safe("turbo-mode wisdom row cleanup", () -> {
            Class<?> turboDialogClass = findClass(classLoader, "com.baidu.frd");
            Class<?> aspectJoinPointClass = findClass(classLoader, "com.baidu.md7");
            Method inflateGuideLayout = findMethod(
                    turboDialogClass,
                    "i",
                    View.class,
                    LayoutInflater.class,
                    int.class,
                    ViewGroup.class,
                    aspectJoinPointClass
            );

            hook(inflateGuideLayout)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (!enabled(HookFeature.WISDOM_RECOMMENDATION)) {
                            return result;
                        }
                        Object layout = chain.getArg(1);
                        if (result instanceof View
                                && layout instanceof Integer
                                && (Integer) layout == TURBO_MODE_GUIDE_LAYOUT_RES_ID) {
                            hideTurboModeWisdomRow((View) result);
                        }
                        return result;
                    });
        });

        safe("turbo-mode wisdom settings cleanup", () -> {
            Class<?> turboSettingsAdapterClass = findClass(classLoader, "com.baidu.c5e");
            Constructor<?> turboSettingsAdapterConstructor = findConstructor(
                    turboSettingsAdapterClass,
                    Context.class,
                    ArrayList.class
            );

            hook(turboSettingsAdapterConstructor)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        if (!enabled(HookFeature.WISDOM_RECOMMENDATION)) {
                            return chain.proceed();
                        }
                        ArrayList<Object> filtered = filterTurboModeFunctions(chain.getArg(1));
                        if (filtered == null) {
                            return chain.proceed();
                        }
                        return chain.proceed(new Object[] { chain.getArg(0), filtered });
                    });
        });
    }

    private void hookAdvancedRecommendationSettings(ClassLoader classLoader) {
        safe("advanced recommendation settings cleanup", () -> {
            Class<?> advancedFragmentClass = findClass(classLoader, "com.baidu.xv");
            Class<?> preferenceFragmentClass = findClass(classLoader, "com.baidu.pn0");
            Method createPreferencesMethod = findMethod(
                    advancedFragmentClass,
                    "H",
                    void.class,
                    Bundle.class,
                    String.class
            );
            Method removePreferenceMethod = findMethod(
                    preferenceFragmentClass,
                    "i0",
                    void.class,
                    String.class
            );

            hook(createPreferencesMethod)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (!enabled(HookFeature.SCENARIO_RECOMMENDATION)
                                && !enabled(HookFeature.ACTIVITY_RECOMMENDATION)) {
                            return result;
                        }
                        Object fragment = chain.getThisObject();
                        if (enabled(HookFeature.SCENARIO_RECOMMENDATION)) {
                            removeHostPreference(fragment, UNIQUE_RESULT_KEY, removePreferenceMethod);
                        }
                        if (enabled(HookFeature.ACTIVITY_RECOMMENDATION)) {
                            removeHostPreference(
                                    fragment,
                                    ACTIVITY_RECOMMENDATION_KEY,
                                    removePreferenceMethod
                            );
                        }
                        return result;
                    });
        });
    }

    private void hookScenarioRecommendationCapability(ClassLoader classLoader) {
        safe("scenario recommendation results", () -> {
            Class<?> cloudResultClass = findClass(
                    classLoader,
                    "com.baidu.input.ime.cloudinput.CloudOutputService"
            );
            Class<?> cloudResultArrayClass = Array.newInstance(cloudResultClass, 0).getClass();
            Class<?> suggestionEventClass = findClass(
                    classLoader,
                    "com.baidu.input.ime.searchservice.bean.SuggestEventBean"
            );
            Field resultTypeField = findField(cloudResultClass, "type", int.class);
            Constructor<?> suggestionEventConstructor = findConstructor(
                    suggestionEventClass,
                    int.class,
                    cloudResultArrayClass,
                    boolean.class
            );

            hook(suggestionEventConstructor)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        if (!enabled(HookFeature.SCENARIO_RECOMMENDATION)) {
                            return chain.proceed();
                        }
                        Object filtered = filterScenarioRecommendationResults(
                                chain.getArg(1),
                                cloudResultClass,
                                resultTypeField
                        );
                        if (filtered == null) {
                            return chain.proceed();
                        }
                        return chain.proceed(new Object[] {
                                chain.getArg(0),
                                filtered,
                                chain.getArg(2)
                        });
                    });
        });

        safe("scenario recommendation cards", () -> {
            Class<?> scenarioCardClass = findClass(classLoader, "com.baidu.vm6");
            Class<?> runtimeStateClass = findClass(classLoader, "com.baidu.pc6");
            Field commercePackagesField = findField(
                    scenarioCardClass,
                    "a",
                    Collection.class
            );
            Field appStorePackagesField = findField(
                    scenarioCardClass,
                    "b",
                    Collection.class
            );
            Method getCurrentInputPackage = findMethod(
                    runtimeStateClass,
                    "d",
                    String.class
            );
            Method canShowScenarioCard = findMethod(
                    scenarioCardClass,
                    "a",
                    boolean.class
            );

            hook(canShowScenarioCard)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (!enabled(HookFeature.SCENARIO_RECOMMENDATION)) {
                            return result;
                        }
                        if (!Boolean.TRUE.equals(result)) {
                            return result;
                        }
                        try {
                            String packageName = (String) getCurrentInputPackage.invoke(null);
                            Collection<?> commercePackages =
                                    (Collection<?>) commercePackagesField.get(null);
                            Collection<?> appStorePackages =
                                    (Collection<?>) appStorePackagesField.get(null);
                            if (commercePackages.contains(packageName)
                                    || appStorePackages.contains(packageName)) {
                                return false;
                            }
                        } catch (Throwable t) {
                            logMessage("failed to inspect scenario card package: " + t);
                        }
                        return result;
                    });
        });
    }

    private void hookSettingsSearchResults(ClassLoader classLoader) {
        safe("settings search result cleanup", () -> {
            Class<?> searchActivityClass = findClass(
                    classLoader,
                    "com.baidu.input.ImeSettingsSearchActivity"
            );
            Class<?> searchItemClass = findClass(classLoader, "com.baidu.ulb");
            Class<?> preferenceClass = findClass(classLoader, "android.preference.Preference");
            Class<?> preferenceGroupClass = findClass(
                    classLoader,
                    "android.preference.PreferenceGroup"
            );
            Class<?> preferenceScreenClass = findClass(
                    classLoader,
                    "android.preference.PreferenceScreen"
            );
            Method populateResultsMethod = findMethod(
                    searchActivityClass,
                    "d",
                    void.class,
                    String.class
            );
            Method removePreferenceMethod = findMethod(
                    preferenceGroupClass,
                    "removePreference",
                    boolean.class,
                    preferenceClass
            );
            Field preferenceScreenField = findField(
                    searchActivityClass,
                    "b",
                    preferenceScreenClass
            );
            Field resultMapField = findField(searchActivityClass, "d", HashMap.class);
            Field searchKeyField = findField(searchItemClass, "f", String.class);
            Field searchPageTypeField = findField(searchItemClass, "q", int.class);
            Field searchParentPathField = findField(searchItemClass, "u", String.class);

            hook(populateResultsMethod)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (!hasSettingsSearchFeatureEnabled()) {
                            return result;
                        }
                        removeBlockedSettingsSearchResults(
                                chain.getThisObject(),
                                searchItemClass,
                                preferenceClass,
                                preferenceScreenField,
                                resultMapField,
                                searchKeyField,
                                searchPageTypeField,
                                searchParentPathField,
                                removePreferenceMethod
                        );
                        return result;
                    });
        });
    }

    private void hookShopStartup(ClassLoader classLoader) {
        safe("shop no-ads startup", () -> {
            Class<?> shopActivityClass = findClass(
                    classLoader,
                    "com.baidu.input.shop.ui.home.ImeShopMainActivity"
            );
            Method noAdsMethod = findMethod(shopActivityClass, "z", boolean.class);

            installConstantHook(noAdsMethod, HookFeature.AD_SDK_BLOCK, true);
        });
    }

    private void removeHostPreference(Object fragment, String key, Method removePreferenceMethod) {
        if (fragment == null) {
            return;
        }
        try {
            removePreferenceMethod.invoke(fragment, key);
        } catch (Throwable t) {
            logMessage("failed to remove preference " + key + ": " + t);
        }
    }

    private List<Object> filterToolboxItems(Object menuItems, Method getMenuFunctionMethod) {
        if (!(menuItems instanceof List<?>)) {
            return null;
        }

        try {
            List<?> source = (List<?>) menuItems;
            ArrayList<Object> filtered = new ArrayList<>(source.size());
            boolean removed = false;
            for (Object item : source) {
                Object menuFunction = item == null ? null : getMenuFunctionMethod.invoke(item);
                if (isBlockedToolboxFunction(menuFunction)) {
                    removed = true;
                    continue;
                }
                filtered.add(item);
            }
            return removed ? filtered : null;
        } catch (Throwable t) {
            logMessage("failed to filter toolbox items: " + t);
            return null;
        }
    }

    private boolean hasToolboxFeatureEnabled() {
        return enabled(HookFeature.HIDE_SEARCH)
                || enabled(HookFeature.HIDE_MECHANICAL_KEYBOARD)
                || enabled(HookFeature.HIDE_FONT_SETTING)
                || enabled(HookFeature.CLOUD_BACKUP_SYNC)
                || enabled(HookFeature.FEEDBACK_BLOCK)
                || enabled(HookFeature.WISDOM_RECOMMENDATION);
    }

    private boolean isBlockedToolboxFunction(Object menuFunction) {
        if (!(menuFunction instanceof Enum<?>)) {
            return false;
        }
        HookFeature feature = toolboxFeature(((Enum<?>) menuFunction).name());
        return feature != null && enabled(feature);
    }

    private static HookFeature toolboxFeature(String name) {
        if (TOOLBOX_SEARCH.equals(name)) {
            return HookFeature.HIDE_SEARCH;
        }
        if (TOOLBOX_MECHANICAL_KEYBOARD.equals(name)) {
            return HookFeature.HIDE_MECHANICAL_KEYBOARD;
        }
        if (TOOLBOX_FONT_SETTING.equals(name)) {
            return HookFeature.HIDE_FONT_SETTING;
        }
        if (TOOLBOX_CLOUD_SYNC.equals(name)) {
            return HookFeature.CLOUD_BACKUP_SYNC;
        }
        if (TOOLBOX_FEEDBACK.equals(name)) {
            return HookFeature.FEEDBACK_BLOCK;
        }
        if (TOOLBOX_INTENT_RECOMMEND.equals(name)) {
            return HookFeature.WISDOM_RECOMMENDATION;
        }
        return null;
    }

    private List<Object> filterPureModeFunctionItems(
            Object functionItems,
            Class<?> functionItemClass,
            Method getTextResource
    ) {
        if (!(functionItems instanceof List<?>)) {
            return null;
        }

        try {
            List<?> source = (List<?>) functionItems;
            ArrayList<Object> filtered = new ArrayList<>(source.size());
            for (Object item : source) {
                if (functionItemClass.isInstance(item)
                        && Integer.valueOf(INTENT_RECOMMEND_TITLE_RES_ID).equals(
                        getTextResource.invoke(item))) {
                    continue;
                }
                filtered.add(item);
            }
            return filtered;
        } catch (Throwable t) {
            logMessage("failed to filter pure-mode function items: " + t);
            return null;
        }
    }

    private static void hideTurboModeWisdomRow(View root) {
        View icon = root.findViewById(VIEW_ID_TURBO_CLOUD_INTENT_ICON);
        View toggle = root.findViewById(VIEW_ID_TURBO_CLOUD_INTENT_SWITCH);
        if (icon == null || toggle == null) {
            return;
        }

        Object row = icon.getParent();
        if (row instanceof View && row == toggle.getParent()) {
            ((View) row).setVisibility(View.GONE);
        }
    }

    private ArrayList<Object> filterTurboModeFunctions(Object functionItems) {
        if (!(functionItems instanceof ArrayList<?>)) {
            return null;
        }

        ArrayList<?> source = (ArrayList<?>) functionItems;
        ArrayList<Object> filtered = new ArrayList<>(source.size());
        int removed = 0;
        for (Object item : source) {
            if (item instanceof Enum<?>
                    && TURBO_CLOUD_INTENT_FUNCTION.equals(((Enum<?>) item).name())) {
                removed++;
                continue;
            }
            filtered.add(item);
        }
        return removed == 1 ? filtered : null;
    }

    private Object filterScenarioRecommendationResults(
            Object results,
            Class<?> resultClass,
            Field resultTypeField
    ) {
        if (results == null) {
            return null;
        }
        if (!results.getClass().isArray()) {
            return Array.newInstance(resultClass, 0);
        }

        try {
            int length = Array.getLength(results);
            ArrayList<Object> filtered = new ArrayList<>(length);
            boolean removed = false;
            for (int i = 0; i < length; i++) {
                Object result = Array.get(results, i);
                if (result != null && !resultClass.isInstance(result)) {
                    return Array.newInstance(resultClass, 0);
                }
                if (result != null
                        && resultTypeField.getInt(result) == UNIQUE_RESULT_SERVICE_TYPE) {
                    removed = true;
                    continue;
                }
                filtered.add(result);
            }
            if (!removed) {
                return results;
            }

            Object filteredArray = Array.newInstance(resultClass, filtered.size());
            for (int i = 0; i < filtered.size(); i++) {
                Array.set(filteredArray, i, filtered.get(i));
            }
            return filteredArray;
        } catch (Throwable t) {
            logMessage("failed to filter scenario recommendation results: " + t);
            return Array.newInstance(resultClass, 0);
        }
    }

    private boolean shouldBlockToolboxClick(Object menuItem, Method getMenuFunctionMethod) {
        if (menuItem == null) {
            return false;
        }
        try {
            return isBlockedToolboxFunction(getMenuFunctionMethod.invoke(menuItem));
        } catch (Throwable t) {
            logMessage("failed to inspect toolbox click: " + t);
            return false;
        }
    }

    private void removeBlockedSettingsSearchResults(
            Object activity,
            Class<?> searchItemClass,
            Class<?> preferenceClass,
            Field preferenceScreenField,
            Field resultMapField,
            Field searchKeyField,
            Field searchPageTypeField,
            Field searchParentPathField,
            Method removePreferenceMethod
    ) {
        if (activity == null) {
            return;
        }

        try {
            Object preferenceScreen = preferenceScreenField.get(activity);
            Object value = resultMapField.get(activity);
            if (preferenceScreen == null || !(value instanceof Map<?, ?>)) {
                return;
            }

            Iterator<? extends Map.Entry<?, ?>> iterator =
                    ((Map<?, ?>) value).entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<?, ?> entry = iterator.next();
                Object searchItem = entry.getValue();
                if (!searchItemClass.isInstance(searchItem)) {
                    continue;
                }
                String key = (String) searchKeyField.get(searchItem);
                int pageType = searchPageTypeField.getInt(searchItem);
                String parentPath = (String) searchParentPathField.get(searchItem);
                if (!isBlockedSettingsSearchTarget(key, pageType, parentPath)) {
                    continue;
                }

                Object preference = entry.getKey();
                if (preferenceClass.isInstance(preference)) {
                    removePreferenceMethod.invoke(preferenceScreen, preference);
                }
                iterator.remove();
            }
        } catch (Throwable t) {
            logMessage("failed to filter settings search results: " + t);
        }
    }

    private boolean hasSettingsSearchFeatureEnabled() {
        return enabled(HookFeature.CLOUD_BACKUP_SYNC)
                || enabled(HookFeature.CLOUD_OPTIMIZATION)
                || enabled(HookFeature.CLOUD_INPUT)
                || enabled(HookFeature.FEEDBACK_BLOCK)
                || enabled(HookFeature.AD_SDK_BLOCK)
                || enabled(HookFeature.SCENARIO_RECOMMENDATION)
                || enabled(HookFeature.ACTIVITY_RECOMMENDATION)
                || enabled(HookFeature.WISDOM_RECOMMENDATION);
    }

    private boolean isBlockedSettingsSearchTarget(
            String key,
            int pageType,
            String parentPath
    ) {
        if ((pageType == CLOUD_BACKUP_PAGE_TYPE
                || isCloudBackupSearchPath(parentPath)
                || CLOUD_BACKUP_KEY.equals(key))
                && enabled(HookFeature.CLOUD_BACKUP_SYNC)) {
            return true;
        }
        if (CLOUD_OPTIMIZATION_KEY.equals(key)) {
            return enabled(HookFeature.CLOUD_OPTIMIZATION);
        }
        if (CLOUD_INPUT_KEY.equals(key)) {
            return enabled(HookFeature.CLOUD_INPUT);
        }
        if (SETTINGS_FEEDBACK_KEY.equals(key)) {
            return enabled(HookFeature.FEEDBACK_BLOCK);
        }
        if (PROGRAMMATIC_AD_KEY.equals(key)) {
            return enabled(HookFeature.AD_SDK_BLOCK);
        }
        if (UNIQUE_RESULT_KEY.equals(key)) {
            return enabled(HookFeature.SCENARIO_RECOMMENDATION);
        }
        if (ACTIVITY_RECOMMENDATION_KEY.equals(key)) {
            return enabled(HookFeature.ACTIVITY_RECOMMENDATION);
        }
        if (SETTINGS_INTENT_RECOMMEND_KEY.equals(key)) {
            return enabled(HookFeature.WISDOM_RECOMMENDATION);
        }
        return false;
    }

    private static boolean isCloudBackupSearchPath(String parentPath) {
        if (parentPath == null) {
            return false;
        }
        int separator = parentPath.indexOf(';');
        String rootKey = separator < 0 ? parentPath : parentPath.substring(0, separator);
        return CLOUD_BACKUP_KEY.equals(rootKey);
    }

    private void hookBlockedSettingsRoutes(ClassLoader classLoader) {
        safe("blocked settings routes", () -> {
            Class<?> menuRouterClass = findClass(classLoader, "com.baidu.h77");
            Class<?> backupPreferenceClass = findClass(
                    classLoader,
                    "com.baidu.input.pref.SettingsBackupPref"
            );
            Class<?> recoveryPreferenceClass = findClass(
                    classLoader,
                    "com.baidu.input.pref.SettingsRecoveryPref"
            );
            Field pendingBackupField = findField(backupPreferenceClass, "g1", boolean.class);
            Field pendingRecoveryField = findField(
                    recoveryPreferenceClass,
                    "f1",
                    boolean.class
            );
            Method openMenuRoute = findMethod(
                    menuRouterClass,
                    "z",
                    boolean.class,
                    Context.class,
                    byte.class,
                    String.class,
                    Bundle.class
            );

            hook(openMenuRoute)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object route = chain.getArg(1);
                        if (route instanceof Byte
                                && (Byte) route == FONT_SETTING_ROUTE
                                && (enabled(HookFeature.HIDE_FONT_SETTING)
                                || enabled(HookFeature.FONT_SHOP_CLEANUP))) {
                            return true;
                        }
                        HookFeature feature = blockedSettingsRouteFeature(route);
                        if (feature == null || !enabled(feature)) {
                            return chain.proceed();
                        }
                        if (route instanceof Byte && (Byte) route == CLOUD_BACKUP_ROUTE) {
                            clearPendingCloudActions(pendingBackupField, pendingRecoveryField);
                        }
                        return true;
                    });
        });

        safe("cloud backup activity restore guard", () -> {
            Class<?> cloudActivityClass = findClass(
                    classLoader,
                    "com.baidu.input.ImeSubConfigActivity"
            );
            Class<?> backupPreferenceClass = findClass(
                    classLoader,
                    "com.baidu.input.pref.SettingsBackupPref"
            );
            Class<?> recoveryPreferenceClass = findClass(
                    classLoader,
                    "com.baidu.input.pref.SettingsRecoveryPref"
            );
            Field pendingBackupField = findField(backupPreferenceClass, "g1", boolean.class);
            Field pendingRecoveryField = findField(
                    recoveryPreferenceClass,
                    "f1",
                    boolean.class
            );
            Field pageTypeField = findField(cloudActivityClass, "s", byte.class);
            Method createActivity = findMethod(
                    cloudActivityClass,
                    "onCreate",
                    void.class,
                    Bundle.class
            );
            Method startActivity = findMethod(cloudActivityClass, "onStart", void.class);

            hook(createActivity)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Activity activity = (Activity) chain.getThisObject();
                        boolean blocked = enabled(HookFeature.CLOUD_BACKUP_SYNC)
                                && isCloudBackupActivity(activity);
                        if (blocked) {
                            clearPendingCloudActions(
                                    pendingBackupField,
                                    pendingRecoveryField
                            );
                        }
                        Object result = chain.proceed();
                        if (blocked) {
                            activity.finish();
                        }
                        return result;
                    });

            hook(startActivity)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        Object activity = chain.getThisObject();
                        if (enabled(HookFeature.CLOUD_BACKUP_SYNC)
                                && activity instanceof Activity
                                && pageTypeField.getByte(activity) == CLOUD_BACKUP_PAGE_TYPE) {
                            clearPendingCloudActions(
                                    pendingBackupField,
                                    pendingRecoveryField
                            );
                            ((Activity) activity).finish();
                        }
                        return result;
                    });
        });

        safe("feedback web entry", () -> {
            Class<?> feedbackEntryClass = findClass(classLoader, "com.baidu.sz3");
            Method openFeedback = findMethod(
                    feedbackEntryClass,
                    "d",
                    void.class,
                    Context.class
            );

            installConstantHook(openFeedback, HookFeature.FEEDBACK_BLOCK, null);
        });

        safe("feedback activity restore guard", () -> {
            Class<?> feedbackActivityClass = findClass(
                    classLoader,
                    "com.baidu.input.feedback.customizer.impl.FeedbackOPPOH5Activity"
            );
            Method createActivity = findMethod(
                    feedbackActivityClass,
                    "onCreate",
                    void.class,
                    Bundle.class
            );

            hook(createActivity)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Activity activity = (Activity) chain.getThisObject();
                        feedbackActivities.add(activity);
                        if (!enabled(HookFeature.FEEDBACK_BLOCK)) {
                            return chain.proceed();
                        }
                        Intent intent = activity.getIntent();
                        if (intent == null) {
                            intent = new Intent();
                            activity.setIntent(intent);
                        }
                        intent.putExtra(FEEDBACK_URL_EXTRA, BLANK_WEB_URL);
                        Object result = chain.proceed();
                        activity.finish();
                        return result;
                    });
        });
    }

    private void finishTrackedFeedbackActivities(long revision) {
        Handler handler = new Handler(Looper.getMainLooper());
        if (!handler.post(() -> {
            FeatureSnapshot current = featureSnapshot.get();
            if (!current.isSchemaValid()
                    || current.revision() < revision
                    || !current.isEnabled(HookFeature.FEEDBACK_BLOCK)) {
                return;
            }
            ArrayList<Activity> tracked;
            synchronized (feedbackActivities) {
                tracked = new ArrayList<>(feedbackActivities);
            }
            for (Activity activity : tracked) {
                if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                    activity.finish();
                }
            }
        })) {
            logMessage("failed to schedule feedback activity cleanup");
        }
    }

    private static HookFeature blockedSettingsRouteFeature(Object route) {
        if (!(route instanceof Byte)) {
            return null;
        }
        byte value = (Byte) route;
        if (value == CLOUD_OPTIMIZATION_ROUTE) {
            return HookFeature.CLOUD_OPTIMIZATION;
        }
        if (value == CLOUD_BACKUP_ROUTE) {
            return HookFeature.CLOUD_BACKUP_SYNC;
        }
        if (value == FEEDBACK_ROUTE) {
            return HookFeature.FEEDBACK_BLOCK;
        }
        if (value == INTENT_RECOMMEND_ROUTE) {
            return HookFeature.WISDOM_RECOMMENDATION;
        }
        return null;
    }

    private static boolean isCloudBackupActivity(Activity activity) {
        if (activity == null) {
            return false;
        }
        Intent intent = activity.getIntent();
        return intent != null
                && intent.getByteExtra(SETTINGS_PAGE_TYPE_EXTRA, (byte) 0)
                == CLOUD_BACKUP_PAGE_TYPE;
    }

    private void clearPendingCloudActions(Field pendingBackupField, Field pendingRecoveryField) {
        try {
            pendingBackupField.setBoolean(null, false);
            pendingRecoveryField.setBoolean(null, false);
        } catch (Throwable t) {
            logMessage("failed to clear pending cloud actions: " + t);
        }
    }

    private void updateSettingsRootLabels(
            Object fragment,
            Method findPreferenceMethod,
            Method setTitleResourceMethod,
            Method getSummaryMethod,
            Method setSummaryMethod
    ) {
        if (fragment == null) {
            return;
        }

        try {
            Object dictionaryPreference = findPreferenceMethod.invoke(fragment, SETTINGS_CIKU_KEY);
            if (dictionaryPreference != null) {
                if (enabled(HookFeature.CLOUD_BACKUP_SYNC)) {
                    setTitleResourceMethod.invoke(dictionaryPreference, CIKU_TITLE_RES_ID);
                }
                CharSequence summary = (CharSequence) getSummaryMethod.invoke(dictionaryPreference);
                CharSequence updated = filterDelimitedSummary(
                        summary,
                        4,
                        enabled(HookFeature.CLOUD_OPTIMIZATION),
                        enabled(HookFeature.CLOUD_BACKUP_SYNC),
                        enabled(HookFeature.CLOUD_BACKUP_SYNC)
                );
                if (updated != summary) {
                    setSummaryMethod.invoke(dictionaryPreference, updated);
                }
            }
        } catch (Throwable t) {
            logMessage("failed to update dictionary settings label: " + t);
        }

        try {
            Object generalPreference = findPreferenceMethod.invoke(fragment, SETTINGS_GENERAL_KEY);
            if (generalPreference != null && enabled(HookFeature.CLOUD_INPUT)) {
                CharSequence summary = (CharSequence) getSummaryMethod.invoke(generalPreference);
                CharSequence updated = filterDelimitedSummary(
                        summary,
                        5,
                        true,
                        false,
                        false,
                        false
                );
                if (updated != summary) {
                    setSummaryMethod.invoke(generalPreference, updated);
                }
            }
        } catch (Throwable t) {
            logMessage("failed to update general settings summary: " + t);
        }
    }

    private static CharSequence filterDelimitedSummary(
            CharSequence summary,
            int expectedParts,
            boolean... removeAfterFirst
    ) {
        if (summary == null) {
            return summary;
        }
        String[] parts = summary.toString().split("\u3001", -1);
        if (parts.length != expectedParts || removeAfterFirst.length != expectedParts - 1) {
            return summary;
        }
        StringBuilder result = new StringBuilder();
        result.append(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (!removeAfterFirst[i - 1]) {
                result.append('\u3001').append(parts[i]);
            }
        }
        String updated = result.toString();
        return updated.contentEquals(summary) ? summary : updated;
    }

    private void removePreferenceOrOnlyCategory(
            Object fragment,
            String key,
            Class<?> preferenceGroupClass,
            Method findPreferenceMethod,
            Method removePreferenceMethod,
            Method getParentMethod,
            Method getChildCountMethod,
            Method getChildMethod,
            Method removeChildMethod
    ) {
        if (fragment == null) {
            return;
        }

        try {
            Object preference = findPreferenceMethod.invoke(fragment, key);
            if (preference == null) {
                return;
            }
            Object parent = getParentMethod.invoke(preference);
            if (preferenceGroupClass.isInstance(parent)
                    && Integer.valueOf(1).equals(getChildCountMethod.invoke(parent))
                    && getChildMethod.invoke(parent, 0) == preference) {
                Object grandParent = getParentMethod.invoke(parent);
                if (preferenceGroupClass.isInstance(grandParent)
                        && Boolean.TRUE.equals(removeChildMethod.invoke(grandParent, parent))) {
                    return;
                }
            }
        } catch (Throwable t) {
            logMessage("failed to remove preference category for " + key + ": " + t);
        }

        removeHostPreference(fragment, key, removePreferenceMethod);
    }

    private void hideCloudInputPreference(
            Object fragment,
            Class<?> cloudInputPreferenceClass,
            Method findPreferenceMethod,
            Field visibleField,
            Method notifyHierarchyChangedMethod
    ) {
        if (fragment == null) {
            return;
        }
        try {
            Object preference = findPreferenceMethod.invoke(fragment, CLOUD_INPUT_KEY);
            if (!cloudInputPreferenceClass.isInstance(preference)) {
                return;
            }
            if (visibleField.getBoolean(preference)) {
                visibleField.setBoolean(preference, false);
                notifyHierarchyChangedMethod.invoke(preference);
            }
        } catch (Throwable t) {
            logMessage("failed to hide cloud input preference: " + t);
        }
    }

    private void hookMyCenterPage(ClassLoader classLoader) {
        safe("shop my-center cleanup", () -> {
            Class<?> myCenterClass = findClass(
                    classLoader,
                    "com.baidu.input.shop.mycenter.MyCenterFragment"
            );
            Class<?> menuDataClass = findClass(classLoader, "com.baidu.zo8");
            Field menuDataField = findField(myCenterClass, "o", ArrayList.class);
            Method menuTitleMethod = findMethod(menuDataClass, "d", int.class);
            Method initializePageMethod = findMethod(myCenterClass, "q0", void.class);
            Method resumePageMethod = findMethod(myCenterClass, "onResume", void.class);
            Method fetchPromotionMethod = findMethod(myCenterClass, "g0", void.class);
            Method createViewMethod = findMethod(
                    myCenterClass,
                    "onCreateView",
                    View.class,
                    LayoutInflater.class,
                    ViewGroup.class,
                    Bundle.class
            );

            installConstantHook(
                    fetchPromotionMethod,
                    HookFeature.SHOP_PROMOTION_CLEANUP,
                    null
            );

            installEmotionMenuFilter(
                    initializePageMethod,
                    menuDataField,
                    menuTitleMethod
            );
            installEmotionMenuFilter(
                    resumePageMethod,
                    menuDataField,
                    menuTitleMethod
            );

            hook(createViewMethod)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (result instanceof View) {
                            cleanMyCenterViews((View) result);
                        }
                        return result;
                    });
        });
    }

    private void hookShopTabs(ClassLoader classLoader) {
        safe("shop tabs cleanup", () -> {
            Class<?> shopHomeClass = findClass(
                    classLoader,
                    "com.baidu.input.shop.ui.home.ImeShopHomeFragment"
            );
            Class<?> tabDataClass = findClass(classLoader, "com.baidu.yg8");
            Class<?> shopViewModelClass = findClass(classLoader, "com.baidu.mq6");
            Class<?> shopBindingClass = findClass(classLoader, "com.baidu.hp");
            Class<?> bottomNavClass = findClass(
                    classLoader,
                    "com.baidu.input.shop.widget.bottomnav.LottieBottomNav"
            );
            Method tabTagMethod = findMethod(tabDataClass, "e", Object.class);
            Field shopBindingField = findField(shopHomeClass, "a", shopBindingClass);
            Field bottomNavField = findField(shopBindingClass, "b", bottomNavClass);
            Field bottomNavSelectedIndexField = findField(bottomNavClass, "e", int.class);
            Method installTabsMethod = findMethod(
                    shopHomeClass,
                    "I",
                    void.class,
                    shopHomeClass,
                    List.class
            );
            Method selectTabMethod = findMethod(
                    shopHomeClass,
                    "J",
                    void.class,
                    String.class,
                    String.class
            );
            Method requestedTabMethod = findMethod(shopHomeClass, "G", String.class);
            Method requestedSubTabMethod = findMethod(shopHomeClass, "F", String.class);
            Method getViewModelMethod = findMethod(shopHomeClass, "H", shopViewModelClass);
            Method refreshTabsMethod = findMethod(shopViewModelClass, "g", void.class);
            Method resumeFragmentMethod = findMethod(shopHomeClass, "onResume", void.class);

            hook(installTabsMethod)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        int mask = shopTabCleanupMask();
                        Object fragment = chain.getArg(0);
                        Object originalMenuItems = chain.getArg(1);
                        FilteredShopTabs filteredTabs = filterShopTabs(
                                fragment,
                                originalMenuItems,
                                tabTagMethod,
                                mask
                        );
                        if (filteredTabs == null) {
                            ShopTabState previous = fragment == null
                                    ? null
                                    : shopTabStates.remove(fragment);
                            Object result;
                            try {
                                result = chain.proceed();
                            } catch (Throwable t) {
                                if (fragment != null) {
                                    restoreShopTabState(fragment, previous);
                                }
                                throw t;
                            }
                            if (shouldRestoreShopBottomNav(previous, originalMenuItems)) {
                                restoreShopBottomNav(
                                        fragment,
                                        shopBindingField,
                                        bottomNavField
                                );
                            }
                            return result;
                        }

                        if (mask != 0 && !prepareShopBottomNavSelection(
                                fragment,
                                shopBindingField,
                                bottomNavField,
                                bottomNavSelectedIndexField,
                                filteredTabs.menuItems.size()
                        )) {
                            ShopTabState previous = shopTabStates.remove(fragment);
                            Object result;
                            try {
                                result = chain.proceed();
                            } catch (Throwable t) {
                                restoreShopTabState(fragment, previous);
                                throw t;
                            }
                            if (shouldRestoreShopBottomNav(previous, originalMenuItems)) {
                                restoreShopBottomNav(
                                        fragment,
                                        shopBindingField,
                                        bottomNavField
                                );
                            }
                            return result;
                        }

                        ShopTabState state = filteredTabs.state;
                        ShopTabState previous = shopTabStates.put(fragment, state);
                        Object result;
                        try {
                            result = mask == 0
                                    ? chain.proceed()
                                    : chain.proceed(
                                            new Object[] { fragment, filteredTabs.menuItems }
                                    );
                        } catch (Throwable t) {
                            restoreShopTabState(fragment, previous);
                            throw t;
                        }
                        if (mask != 0 && filteredTabs.menuItems.size() == 1) {
                            try {
                                selectTabMethod.invoke(
                                        fragment,
                                        requestedTabMethod.invoke(fragment),
                                        requestedSubTabMethod.invoke(fragment)
                                );
                            } catch (Throwable t) {
                                logMessage("failed to select the only visible shop tab: " + t);
                            }
                        }
                        if (previous != null
                                && previous.tags.size() == 1
                                && state.tags.size() > 1) {
                            restoreShopBottomNav(
                                    fragment,
                                    shopBindingField,
                                    bottomNavField
                            );
                        }
                        return result;
                    });

            hook(selectTabMethod)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object fragment = chain.getThisObject();
                        int mask = shopTabCleanupMask();
                        ShopTabState state = shopTabStates.get(fragment);
                        if (state != null && state.appliedMask != mask) {
                            refreshShopTabs(
                                    fragment,
                                    getViewModelMethod,
                                    refreshTabsMethod
                            );
                            state = shopTabStates.get(fragment);
                        }
                        if (mask == 0 || state == null || state.appliedMask != mask) {
                            return chain.proceed();
                        }
                        Object requested = chain.getArg(0);
                        String requestedTag = requested instanceof String ? (String) requested : null;
                        if (requestedTag != null
                                && !requestedTag.isEmpty()
                                && state.tags.contains(requestedTag)) {
                            return chain.proceed();
                        }
                        return chain.proceed(new Object[] { state.firstTag, null });
                    });

            hook(resumeFragmentMethod)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        Object fragment = chain.getThisObject();
                        ShopTabState state = shopTabStates.get(fragment);
                        int mask = shopTabCleanupMask();
                        if (state != null && state.appliedMask != mask) {
                            refreshShopTabs(
                                    fragment,
                                    getViewModelMethod,
                                    refreshTabsMethod
                            );
                        }
                        return result;
                    });
        });
    }

    private void hookLocalSkinDetailCleanup(ClassLoader classLoader) {
        safe("local skin detail cleanup", () -> {
            Class<?> detailFragmentClass = findClass(
                    classLoader,
                    "com.baidu.input.shop.ui.skin.detail.SkinLocalDetailFragment"
            );
            Class<?> skinLocalInfoClass = findClass(
                    classLoader,
                    "com.baidu.input.shop.repository.skin.model.SkinLocalInfo"
            );
            Class<?> fragmentClass = findClass(classLoader, "androidx.fragment.app.Fragment");
            Method bindSkinMethod = findMethod(
                    detailFragmentClass,
                    "W",
                    void.class,
                    skinLocalInfoClass
            );
            Method getFragmentViewMethod = fragmentClass.getMethod("getView");

            hook(bindSkinMethod)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (enabled(HookFeature.ONLINE_SKIN_SHOP_CLEANUP)) {
                            Object root = getFragmentViewMethod.invoke(chain.getThisObject());
                            if (root instanceof View) {
                                hideHostView((View) root, VIEW_ID_SKIN_SHARE_LAYOUT);
                            }
                        }
                        return result;
                    });
        });
    }

    private void hookLocalSkinListCleanup(ClassLoader classLoader) {
        safe("local skin list cleanup", () -> {
            Class<?> adapterClass = findClass(classLoader, "com.baidu.x9c");
            Class<?> itemDataClass = findClass(classLoader, "com.baidu.o9c");
            Class<?> skinLocalInfoClass = findClass(
                    classLoader,
                    "com.baidu.input.shop.repository.skin.model.SkinLocalInfo"
            );
            Method submitItemsMethod = findMethod(adapterClass, "t", void.class, List.class);
            Method getSkinLocalInfoMethod = findMethod(
                    itemDataClass,
                    "a",
                    skinLocalInfoClass
            );
            Method getSkinNameMethod = findMethod(skinLocalInfoClass, "g", String.class);

            hook(submitItemsMethod)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        if (!enabled(HookFeature.ONLINE_SKIN_SHOP_CLEANUP)) {
                            return chain.proceed();
                        }
                        Object items = chain.getArg(0);
                        if (!(items instanceof List<?>)) {
                            return chain.proceed();
                        }
                        try {
                            ArrayList<Object> visibleItems = new ArrayList<>(((List<?>) items).size());
                            for (Object item : (List<?>) items) {
                                Object skin = getSkinLocalInfoMethod.invoke(item);
                                Object name = getSkinNameMethod.invoke(skin);
                                if (!HIDDEN_LOCAL_SKIN_NAME.equals(name)) {
                                    visibleItems.add(item);
                                }
                            }
                            return chain.proceed(new Object[] { visibleItems });
                        } catch (Throwable t) {
                            logMessage("failed to filter local skin list: " + t);
                            return chain.proceed();
                        }
                    });
        });
    }

    private void hookEmotionStoreEntryCleanup(ClassLoader classLoader) {
        installEmotionStoreEntryHook(
                classLoader, "com.baidu.fq8", "g0", "c", null, VIEW_ID_EMOTION_STORE
        );
        installEmotionStoreEntryHook(
                classLoader, "com.baidu.n6d", "f0", "d", null, VIEW_ID_EMOTION_STORE
        );
        installEmotionStoreEntryHook(
                classLoader, "com.baidu.tf9", "i0", "c", null, VIEW_ID_EMOTION_STORE
        );
        installEmotionStoreEntryHook(
                classLoader, "com.baidu.eh9", "d0", "d", "k", VIEW_ID_EMOTION_SHOP
        );
        installEmotionStoreEntryHook(
                classLoader, "com.baidu.xh9", "h0", "d", "l", VIEW_ID_EMOTION_SHOP
        );
    }

    private void hookDoutuPageCleanup(ClassLoader classLoader) {
        installDoutuTabBarHook(classLoader, "com.baidu.ck3", "i");
        installDoutuTabBarHook(classLoader, "com.baidu.sb9", "g");
    }

    private void installDoutuTabBarHook(
            ClassLoader classLoader,
            String className,
            String buildMethodName
    ) {
        safe("doutu page cleanup " + className, () -> {
            Class<?> tabBarClass = findClass(classLoader, className);
            Method buildTabs = findMethod(tabBarClass, buildMethodName, void.class, Context.class);
            Field tabsField = findField(tabBarClass, "f", TextView[].class);
            hook(buildTabs)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        Object value = tabsField.get(chain.getThisObject());
                        if (value instanceof TextView[]) {
                            TextView[] tabs = (TextView[]) value;
                            for (int tabType : EMOTION_TYPE_DOUTU) {
                                if (tabs.length > tabType && tabs[tabType] != null) {
                                    registerDoutuTab(tabs[tabType]);
                                }
                            }
                        }
                        return result;
                    });
        });
    }

    private void registerVisibleDoutuTabs(View root) {
        if (root instanceof TextView && "\u6597\u56fe".contentEquals(((TextView) root).getText())) {
            registerDoutuTab((TextView) root);
            return;
        }
        if (!(root instanceof ViewGroup)) {
            return;
        }
        ViewGroup group = (ViewGroup) root;
        for (int index = 0; index < group.getChildCount(); index++) {
            registerVisibleDoutuTabs(group.getChildAt(index));
        }
    }

    private void registerDoutuTab(TextView doutuTab) {
        if (doutuTab == null) {
            return;
        }
        synchronized (doutuTabs) {
            if (!doutuTabs.containsKey(doutuTab)) {
                doutuTabs.put(doutuTab, DoutuTabState.capture(doutuTab));
            }
        }
        synchronizeDoutuTabs();
    }

    private void synchronizeDoutuTabs() {
        Handler handler = new Handler(Looper.getMainLooper());
        if (!handler.post(() -> {
            boolean hide = enabled(HookFeature.EMOTION_SHOP_CLEANUP);
            synchronized (doutuTabs) {
                Iterator<Map.Entry<TextView, DoutuTabState>> iterator =
                        doutuTabs.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<TextView, DoutuTabState> entry = iterator.next();
                    if (entry.getKey() == null || entry.getValue() == null) {
                        iterator.remove();
                    } else {
                        entry.getValue().apply(hide);
                    }
                }
            }
        })) {
            logMessage("failed to schedule doutu tab sync");
        }
    }

    private void hookAiWriterEntryCleanup(ClassLoader classLoader) {
        safe("ai writer entry cleanup", () -> {
            Class<?> repositoryClass = findClass(
                    classLoader,
                    "com.baidu.input.ime.cand.repository.AiIconRepository"
            );
            Method isAvailable = findMethod(repositoryClass, "a", boolean.class);
            installConstantHook(isAvailable, HookFeature.HIDE_AI_WRITER, false);
        });
    }

    private void installEmotionStoreEntryHook(
            ClassLoader classLoader,
            String className,
            String createMethodName,
            String rootFieldName,
            String entryFieldName,
            int storeViewId
    ) {
        safe("emotion store entry " + className, () -> {
            Class<?> panelClass = findClass(classLoader, className);
            Method createMethod = findMethod(panelClass, createMethodName, void.class);
            Field rootField = findField(panelClass, rootFieldName);
            Field entryField = entryFieldName == null ? null : findField(panelClass, entryFieldName);
            hook(createMethod)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        Object root = rootField.get(chain.getThisObject());
                        if (root instanceof View) {
                            Object entry = entryField == null ? null : entryField.get(chain.getThisObject());
                            registerEmotionStoreEntry(
                                    (View) root,
                                    entry instanceof View ? (View) entry : null,
                                    storeViewId
                            );
                        }
                        return result;
                    });
        });
    }

    private void registerEmotionStoreEntry(View root, View entry, int storeViewId) {
        View candidate = entry != null ? entry : root.findViewById(storeViewId);
        if (candidate == null) {
            return;
        }
        root.post(() -> {
            synchronized (emotionStoreEntries) {
                if (!emotionStoreEntries.containsKey(candidate)) {
                    emotionStoreEntries.put(candidate, EmotionStoreEntryState.capture(root, candidate));
                    logMessage("emotion store entry registered: "
                            + candidate.getClass().getName() + "/0x"
                            + Integer.toHexString(candidate.getId()));
                }
            }
            synchronizeEmotionStoreEntries();
        });
    }

    private void synchronizeEmotionStoreEntries() {
        Handler handler = new Handler(Looper.getMainLooper());
        if (!handler.post(() -> {
            boolean hide = enabled(HookFeature.EMOTION_SHOP_CLEANUP);
            synchronized (emotionStoreEntries) {
                Iterator<Map.Entry<View, EmotionStoreEntryState>> iterator =
                        emotionStoreEntries.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<View, EmotionStoreEntryState> entry = iterator.next();
                    EmotionStoreEntryState state = entry.getValue();
                    if (entry.getKey() == null || state == null) {
                        iterator.remove();
                    } else {
                        state.apply(hide);
                    }
                }
            }
        })) {
            logMessage("failed to schedule emotion store entry sync");
        }
    }

    private void hookKeyboardLogoReplacement(ClassLoader classLoader) {
        safe("custom keyboard logo pure-mode entry", () -> {
            Method decodeResource = findMethod(
                    BitmapFactory.class,
                    "decodeResource",
                    Bitmap.class,
                    android.content.res.Resources.class,
                    int.class
            );
            hook(decodeResource)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Bitmap original = (Bitmap) chain.proceed();
                        Object resourceId = chain.getArg(1);
                        if (!(resourceId instanceof Integer)
                                || (((Integer) resourceId).intValue()
                                != DRAWABLE_PURE_MODE_CAND_ICON_NORMAL
                                && ((Integer) resourceId).intValue()
                                != DRAWABLE_PURE_MODE_CAND_ICON_DARK)) {
                            return original;
                        }
                        return enabled(HookFeature.CUSTOM_KEYBOARD_LOGO)
                                ? replacementOrOriginal(original)
                                : original;
                    });
        });
    }

    private Bitmap replacementOrOriginal(Bitmap original) {
        Bitmap replacement = loadCustomKeyboardLogo(
                original == null ? 72 : original.getWidth(),
                original == null ? 72 : original.getHeight()
        );
        if (replacement == null) {
            logMessage("custom keyboard logo replacement skipped: resource unavailable");
            return original;
        }
        return replacement;
    }

    private Bitmap loadCustomKeyboardLogo(int width, int height) {
        if (width <= 0 || height <= 0) {
            return null;
        }
        Bitmap cached = customKeyboardLogo;
        if (cached != null && !cached.isRecycled()
                && customKeyboardLogoWidth == width && customKeyboardLogoHeight == height) {
            return cached;
        }
        synchronized (this) {
            cached = customKeyboardLogo;
            if (cached != null && !cached.isRecycled()
                    && customKeyboardLogoWidth == width && customKeyboardLogoHeight == height) {
                return cached;
            }
            try (JarFile moduleApk = new JarFile(getModuleApplicationInfo().sourceDir)) {
                JarEntry entry = moduleApk.getJarEntry("assets/miku_keyboard_emoji.png");
                if (entry == null) {
                    logMessage("custom keyboard logo resource missing");
                    return null;
                }
                try (InputStream stream = moduleApk.getInputStream(entry)) {
                    Bitmap source = BitmapFactory.decodeStream(stream);
                    if (source == null) {
                        return null;
                    }
                    int iconSize = Math.min(width, height);
                    int offsetX = (width - iconSize) / 2;
                    int offsetY = (height - iconSize) / 2;
                    Bitmap replacement = Bitmap.createBitmap(
                            width,
                            height,
                            Bitmap.Config.ARGB_8888
                    );
                    Canvas canvas = new Canvas(replacement);
                    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
                    canvas.drawBitmap(
                            source,
                            new Rect(4, 4, source.getWidth() - 5, source.getHeight() - 5),
                            new Rect(offsetX, offsetY, offsetX + iconSize, offsetY + iconSize),
                            paint
                    );
                    customKeyboardLogo = replacement;
                    customKeyboardLogoWidth = width;
                    customKeyboardLogoHeight = height;
                    source.recycle();
                    return customKeyboardLogo;
                }
            } catch (Throwable t) {
                logMessage("failed to load custom keyboard logo: " + t);
                return null;
            }
        }
    }

    private void hookMyCenterDynamicPage(ClassLoader classLoader) {
        safe("my-center dynamic page", () -> {
            Class<?> dynamicViewModelClass = findClass(
                    classLoader,
                    "com.baidu.input.shopbase.dynamic.DynamicViewModel"
            );
            Method fetchDynamicPage = findMethod(
                    dynamicViewModelClass,
                    "F",
                    void.class,
                    String.class
            );

            hook(fetchDynamicPage)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> enabled(HookFeature.SHOP_PROMOTION_CLEANUP)
                            && MY_CENTER_DYNAMIC_PAGE_MARK.equals(chain.getArg(0))
                            ? null
                            : chain.proceed());
        });
    }

    private void installEmotionMenuFilter(
            Method hostMethod,
            Field menuDataField,
            Method menuTitleMethod
    ) {
        hook(hostMethod)
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    if (!enabled(HookFeature.EMOTION_SHOP_CLEANUP)) {
                        return chain.proceed();
                    }
                    RemovedMenuItem removed = temporarilyRemoveEmotionMenuItem(
                            chain.getThisObject(),
                            menuDataField,
                            menuTitleMethod
                    );
                    try {
                        return chain.proceed();
                    } finally {
                        restoreEmotionMenuItem(
                                chain.getThisObject(),
                                menuDataField,
                                removed
                        );
                    }
                });
    }

    private RemovedMenuItem temporarilyRemoveEmotionMenuItem(
            Object fragment,
            Field menuDataField,
            Method menuTitleMethod
    ) {
        if (fragment == null) {
            return null;
        }

        try {
            Object value = menuDataField.get(fragment);
            if (!(value instanceof List<?>)) {
                return null;
            }

            List<?> items = (List<?>) value;
            for (int index = 0; index < items.size(); index++) {
                Object item = items.get(index);
                Object title = menuTitleMethod.invoke(item);
                if (title instanceof Integer && (Integer) title == USER_EMOTION_TITLE_RES_ID) {
                    Object removed = items.remove(index);
                    return new RemovedMenuItem(index, removed);
                }
            }
        } catch (Throwable t) {
            logMessage("failed to remove my-center emotion item: " + t);
        }
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void restoreEmotionMenuItem(
            Object fragment,
            Field menuDataField,
            RemovedMenuItem removed
    ) {
        if (fragment == null || removed == null) {
            return;
        }
        try {
            Object value = menuDataField.get(fragment);
            if (!(value instanceof List<?>)) {
                return;
            }
            List items = (List) value;
            if (!items.contains(removed.item)) {
                items.add(Math.min(removed.index, items.size()), removed.item);
            }
        } catch (Throwable t) {
            logMessage("failed to restore my-center emotion item: " + t);
        }
    }

    private FilteredShopTabs filterShopTabs(
            Object fragment,
            Object menuItems,
            Method tabTagMethod,
            int mask
    ) {
        if (fragment == null || !(menuItems instanceof List<?>)) {
            return null;
        }
        try {
            ArrayList<Object> visibleItems = new ArrayList<>(((List<?>) menuItems).size());
            ArrayList<String> visibleTags = new ArrayList<>(((List<?>) menuItems).size());
            Set<String> seenTags = new HashSet<>();
            for (Object item : (List<?>) menuItems) {
                Object tagValue = tabTagMethod.invoke(item);
                if (!(tagValue instanceof String)) {
                    return null;
                }
                String tag = (String) tagValue;
                if (!seenTags.add(tag)) {
                    return null;
                }
                if (!isHiddenShopTab(tag, mask)) {
                    visibleItems.add(item);
                    visibleTags.add(tag);
                }
            }
            if (visibleItems.isEmpty()) {
                return null;
            }
            return new FilteredShopTabs(
                    visibleItems,
                    new ShopTabState(visibleTags, mask)
            );
        } catch (Throwable t) {
            logMessage("failed to filter shop tabs: " + t);
            return null;
        }
    }

    private int shopTabCleanupMask() {
        FeatureSnapshot snapshot = featureSnapshot.get();
        int mask = 0;
        if (snapshot.isEnabled(HookFeature.ONLINE_SKIN_SHOP_CLEANUP)) {
            mask |= SHOP_MASK_SKIN;
        }
        if (snapshot.isEnabled(HookFeature.EMOTION_SHOP_CLEANUP)) {
            mask |= SHOP_MASK_EMOTION;
        }
        if (snapshot.isEnabled(HookFeature.FONT_SHOP_CLEANUP)) {
            mask |= SHOP_MASK_FONT;
        }
        return mask;
    }

    private static boolean isHiddenShopTab(String tag, int mask) {
        if (SHOP_SKIN_TAB.equals(tag)) {
            return (mask & SHOP_MASK_SKIN) != 0;
        }
        if (SHOP_EMOTION_TAB.equals(tag)) {
            return (mask & SHOP_MASK_EMOTION) != 0;
        }
        return SHOP_FONT_TAB.equals(tag) && (mask & SHOP_MASK_FONT) != 0;
    }

    private static String selectShopFallbackTag(List<String> tags) {
        String[] priority = {
                SHOP_SKIN_TAB,
                SHOP_EMOTION_TAB,
                SHOP_MY_TAB,
                SHOP_FONT_TAB
        };
        for (String candidate : priority) {
            if (tags.contains(candidate)) {
                return candidate;
            }
        }
        return tags.get(0);
    }

    private void restoreShopTabState(Object fragment, ShopTabState previous) {
        if (previous == null) {
            shopTabStates.remove(fragment);
        } else {
            shopTabStates.put(fragment, previous);
        }
    }

    private static boolean shouldRestoreShopBottomNav(
            ShopTabState previous,
            Object menuItems
    ) {
        return previous != null
                && previous.tags.size() == 1
                && menuItems instanceof List<?>
                && ((List<?>) menuItems).size() > 1;
    }

    private boolean prepareShopBottomNavSelection(
            Object fragment,
            Field shopBindingField,
            Field bottomNavField,
            Field selectedIndexField,
            int menuSize
    ) {
        if (fragment == null || menuSize <= 0) {
            return false;
        }
        try {
            Object binding = shopBindingField.get(fragment);
            Object bottomNav = binding == null ? null : bottomNavField.get(binding);
            if (bottomNav == null) {
                return false;
            }
            int selectedIndex = selectedIndexField.getInt(bottomNav);
            if (selectedIndex < 0 || selectedIndex >= menuSize) {
                selectedIndexField.setInt(bottomNav, 0);
            }
            return true;
        } catch (Throwable t) {
            logMessage("failed to prepare shop bottom navigation: " + t);
            return false;
        }
    }

    private void refreshShopTabs(
            Object fragment,
            Method getViewModelMethod,
            Method refreshTabsMethod
    ) {
        if (fragment == null) {
            return;
        }
        try {
            Object viewModel = getViewModelMethod.invoke(fragment);
            refreshTabsMethod.invoke(viewModel);
        } catch (Throwable t) {
            logMessage("failed to refresh shop tabs after settings change: " + t);
        }
    }

    private void restoreShopBottomNav(
            Object fragment,
            Field shopBindingField,
            Field bottomNavField
    ) {
        if (fragment == null) {
            return;
        }
        try {
            Object binding = shopBindingField.get(fragment);
            Object value = binding == null ? null : bottomNavField.get(binding);
            if (value instanceof View && ((View) value).getVisibility() == View.GONE) {
                ((View) value).setVisibility(View.VISIBLE);
            }
        } catch (Throwable t) {
            logMessage("failed to restore shop bottom navigation: " + t);
        }
    }

    private void cleanMyCenterViews(View root) {
        if (enabled(HookFeature.ACCOUNT_ISOLATION)) {
            View loginContainer = root.findViewById(VIEW_ID_CL_LOGIN);
            if (loginContainer != null && loginContainer.getParent() instanceof View) {
                ((View) loginContainer.getParent()).setVisibility(View.GONE);
            }
            hideHostView(root, VIEW_ID_CL_HEADER);
        }

        if (enabled(HookFeature.SHOP_PROMOTION_CLEANUP)) {
            hideHostView(root, VIEW_ID_AD_CONTAINER);
            hideHostView(root, VIEW_ID_MEMBER_BANNER);
        }
        if (enabled(HookFeature.FEEDBACK_BLOCK)) {
            hideHostView(root, VIEW_ID_CL_FEEDBACK);
            hideHostView(root, VIEW_ID_LINE_HELP);
        }
    }

    private static void hideHostView(View root, int id) {
        View view = root.findViewById(id);
        if (view != null) {
            view.setVisibility(View.GONE);
        }
    }

    private void hookAccountIsolation(ClassLoader classLoader) throws Throwable {
        Class<?> passportClass = findClass(
                classLoader,
                "com.baidu.input.account.pub.PassportSdkProxy"
        );
        Method passportInit = findMethod(
                passportClass,
                "A",
                void.class,
                Context.class
        );
        Method directPassportInit = findMethod(
                passportClass,
                "m",
                void.class,
                Context.class
        );

        installConstantHook(passportInit, HookFeature.ACCOUNT_ISOLATION, null);
        installConstantHook(directPassportInit, HookFeature.ACCOUNT_ISOLATION, null);
    }

    private void hookBackgroundUpdateCheck(ClassLoader classLoader) {
        safe("background update check", () -> {
            Class<?> updateAgentClass = findClass(classLoader, "com.baidu.u87");
            Class<?> updateCallbackClass = findClass(classLoader, "com.baidu.i87");
            Class<?> settingsUpdateCallbackClass = findClass(classLoader, "com.baidu.y6b$a");
            Field callbackField = findField(updateAgentClass, "a", updateCallbackClass);
            Field thresholdField = findField(updateAgentClass, "e", int.class);
            Method startCheckMethod = findMethod(updateAgentClass, "S", void.class);
            Method reportResultMethod = findMethod(
                    updateCallbackClass,
                    "a",
                    void.class,
                    int.class,
                    int.class,
                    boolean.class
            );

            hook(startCheckMethod)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        if (!enabled(HookFeature.BACKGROUND_UPDATE_CHECK)) {
                            return chain.proceed();
                        }
                        Object agent = chain.getThisObject();
                        Object callback = callbackField.get(agent);
                        if (thresholdField.getInt(agent) != Integer.MAX_VALUE
                                || !settingsUpdateCallbackClass.isInstance(callback)) {
                            return chain.proceed();
                        }

                        invokeCallback(reportResultMethod, callback, 0, HOST_VERSION_CODE, false);
                        return null;
                    });
        });
    }

    private void hookRemoteSkinUpgradeCheck(ClassLoader classLoader) {
        safe("remote skin upgrade check", () -> {
            Class<?> skinUpgradeClass = findClass(classLoader, "com.baidu.fgd");
            Method checkSkinUpgrade = findMethod(skinUpgradeClass, "a", void.class);

            installConstantHook(
                    checkSkinUpgrade,
                    HookFeature.REMOTE_SKIN_UPGRADE,
                    null
            );
        });
    }

    private void hookPrivacyTelemetry(ClassLoader classLoader) {
        safe("main stats collection", () -> {
            Class<?> statsBootstrapClass = findClass(classLoader, "com.baidu.qrc");
            Class<?> requestStatsClass = findClass(classLoader, "com.baidu.qee");
            Method initializeStats = findMethod(statsBootstrapClass, "f", void.class);
            Method recordRequestStats = findMethod(
                    requestStatsClass,
                    "C",
                    boolean.class,
                    int.class,
                    String.class
            );

            installConstantHook(initializeStats, HookFeature.PRIVACY_TELEMETRY, null);
            installConstantHook(recordRequestStats, HookFeature.PRIVACY_TELEMETRY, false);
        });

        safe("SAPI stats", () -> {
            Class<?> sapiStatsClass = findClass(
                    classLoader,
                    "com.baidu.sapi2.utils.StatService"
            );
            Class<?> sapiParamsClass = findClass(
                    classLoader,
                    "com.baidu.sapi2.httpwrap.HttpHashMapWrap"
            );
            Method recordSapiEvent = findMethod(
                    sapiStatsClass,
                    "onEvent",
                    void.class,
                    String.class,
                    Map.class
            );
            Method sendSapiStats = findMethod(
                    sapiStatsClass,
                    "sendRequest",
                    void.class,
                    sapiParamsClass
            );

            installConstantHook(recordSapiEvent, HookFeature.PRIVACY_TELEMETRY, null);
            installConstantHook(sendSapiStats, HookFeature.PRIVACY_TELEMETRY, null);
        });

        safe("crash upload", () -> {
            Class<?> crashUploadClass = findClass(classLoader, "com.baidu.ar3");
            Class<?> uploadCallbackClass = findClass(classLoader, "com.baidu.trc");
            Method uploadCrash = findMethod(
                    crashUploadClass,
                    "c",
                    void.class,
                    byte[].class,
                    uploadCallbackClass
            );
            Method uploadSucceeded = findMethod(
                    uploadCallbackClass,
                    "onSuccess",
                    void.class
            );

            hook(uploadCrash)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        if (!enabled(HookFeature.PRIVACY_TELEMETRY)) {
                            return chain.proceed();
                        }
                        postCallbackOnMainThread(uploadSucceeded, chain.getArg(1));
                        return null;
                    });
        });
    }

    private void hookAdSdkGates(ClassLoader classLoader) {
        safe("ad SDK gates", () -> {
            Class<?> adConfigClass = findClass(classLoader, "com.baidu.zu");
            Method enableBeiZi = findMethod(adConfigClass, "b", boolean.class);
            Method enablePangolin = findMethod(adConfigClass, "c", boolean.class);
            Method enableGdt = findMethod(adConfigClass, "d", boolean.class);
            Method enableSplash = findMethod(adConfigClass, "e", boolean.class);
            Method enableMobAds = findMethod(adConfigClass, "f", boolean.class);

            installConstantHook(enableBeiZi, HookFeature.AD_SDK_BLOCK, false);
            installConstantHook(enablePangolin, HookFeature.AD_SDK_BLOCK, false);
            installConstantHook(enableGdt, HookFeature.AD_SDK_BLOCK, false);
            installConstantHook(enableSplash, HookFeature.AD_SDK_BLOCK, false);
            installConstantHook(enableMobAds, HookFeature.AD_SDK_BLOCK, false);
        });

        safe("MobAds shop bootstrap", () -> {
            Class<?> mobAdsInitializerClass = findClass(
                    classLoader,
                    "com.baidu.input.mobsdk.impl.ImeMobAdSdkImpl"
            );
            Method initializeMobAds = findMethod(mobAdsInitializerClass, "U2", void.class);

            installConstantHook(initializeMobAds, HookFeature.AD_SDK_BLOCK, null);
        });
    }

    private void hookCandidateAdvertisement(ClassLoader classLoader) {
        safe("candidate advertisement display", () -> {
            Class<?> candidateAdvertisementClass = findClass(classLoader, "com.baidu.ee1");
            Method canDisplayAdvertisement = findMethod(
                    candidateAdvertisementClass,
                    "h",
                    boolean.class
            );

            installConstantHook(
                    canDisplayAdvertisement,
                    HookFeature.ACTIVITY_RECOMMENDATION,
                    false
            );
        });

        safe("candidate advertisement registration", () -> {
            Class<?> notificationCenterClass = findClass(classLoader, "com.baidu.ty8");
            Class<?> notificationHandlerClass = findClass(classLoader, "com.baidu.is5");
            Method registerNotificationHandler = findMethod(
                    notificationCenterClass,
                    "Q2",
                    void.class,
                    String.class,
                    notificationHandlerClass
            );

            hook(registerNotificationHandler)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> enabled(HookFeature.ACTIVITY_RECOMMENDATION)
                            && CANDIDATE_ADVERTISEMENT_KEY.equals(chain.getArg(0))
                            ? null
                            : chain.proceed());
        });

        safe("candidate advertisement image download", () -> {
            Class<?> candidateAdvertisementClass = findClass(classLoader, "com.baidu.ee1");
            Class<?> strategyClass = findClass(
                    classLoader,
                    "com.baidu.input.noti.notiv3.CandAdvStrategyBean"
            );
            Class<?> downloadCallbackClass = findClass(classLoader, "com.baidu.ee1$b");
            Method downloadAdvertisementImage = findMethod(
                    candidateAdvertisementClass,
                    "o",
                    void.class,
                    strategyClass,
                    downloadCallbackClass
            );

            installConstantHook(
                    downloadAdvertisementImage,
                    HookFeature.ACTIVITY_RECOMMENDATION,
                    null
            );
        });
    }

    private void installConstantHook(Method method, HookFeature feature, Object result) {
        hook(method)
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> enabled(feature) ? result : chain.proceed());
    }

    private void invokeCallback(Method callback, Object target, Object... args) {
        if (target == null) {
            return;
        }
        try {
            callback.invoke(target, args);
        } catch (Throwable t) {
            logMessage("disabled capability callback failed: " + t);
        }
    }

    private void postCallbackOnMainThread(Method callback, Object target, Object... args) {
        if (target == null) {
            return;
        }
        Handler handler = new Handler(Looper.getMainLooper());
        if (!handler.post(() -> invokeCallback(callback, target, args))) {
            logMessage("failed to schedule disabled capability callback");
        }
    }

    private void enforceClipCountField(Object config, HookTargets targets) {
        if (config == null) {
            return;
        }

        try {
            int current = targets.clipCountField.getInt(config);
            if (current < MAX_CLIP_COUNT) {
                targets.clipCountField.setInt(config, MAX_CLIP_COUNT);
                logMessage(targets.profile.configClassName + ".a field lifted from " + current + " to " + MAX_CLIP_COUNT);
            }
        } catch (Throwable t) {
            logMessage("failed to update " + targets.profile.configClassName + ".a field: " + t.getMessage());
        }
    }

    private void updateCounterText(Object panel, HookTargets targets) {
        if (panel == null) {
            return;
        }

        try {
            TextView counter = (TextView) targets.counterTextField.get(panel);
            if (counter == null) {
                return;
            }

            CharSequence counterText = counter.getText();
            if (counterText == null) {
                return;
            }

            String text = counterText.toString();
            String updated = liftCounterLimitText(text);

            if (!text.equals(updated)) {
                counter.setText(updated);
                logMessage("counter text updated: " + text + " -> " + updated);
            }
        } catch (Throwable t) {
            logMessage("failed to update counter text: " + t.getMessage());
        }
    }

    private static boolean shouldLiftClipCount(Object count) {
        return count instanceof Integer && (Integer) count < MAX_CLIP_COUNT;
    }

    private static String liftCounterLimitText(String text) {
        return text
                .replace(ORIGINAL_COUNTER_SUFFIX_FULL_WIDTH, MAX_COUNTER_SUFFIX_FULL_WIDTH)
                .replace(ORIGINAL_COUNTER_SUFFIX_ASCII, MAX_COUNTER_SUFFIX_ASCII);
    }

    private static Class<?> findClass(ClassLoader classLoader, String className) throws ClassNotFoundException {
        return Class.forName(className, false, classLoader);
    }

    private static Method findMethod(
            Class<?> targetClass,
            String methodName,
            Class<?> returnType,
            Class<?>... parameterTypes
    ) throws NoSuchMethodException {
        Method method = targetClass.getDeclaredMethod(methodName, parameterTypes);
        if (!returnType.equals(method.getReturnType())) {
            throw new NoSuchMethodException(targetClass.getName() + "." + methodName + " return type");
        }
        method.setAccessible(true);
        return method;
    }

    private static Constructor<?> findConstructor(
            Class<?> targetClass,
            Class<?>... parameterTypes
    ) throws NoSuchMethodException {
        Constructor<?> constructor = targetClass.getDeclaredConstructor(parameterTypes);
        constructor.setAccessible(true);
        return constructor;
    }

    private static Field findField(Class<?> targetClass, String fieldName) throws NoSuchFieldException {
        Field field = targetClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field;
    }

    private static Field findField(
            Class<?> targetClass,
            String fieldName,
            Class<?> fieldType
    ) throws NoSuchFieldException {
        Field field = findField(targetClass, fieldName);
        if (!fieldType.equals(field.getType())) {
            throw new NoSuchFieldException(targetClass.getName() + "." + fieldName + " type");
        }
        return field;
    }

    private void safe(String name, HookInstaller installer) {
        try {
            installer.install();
            logMessage(name + " installed");
        } catch (Throwable t) {
            logMessage(name + " failed: " + t);
        }
    }

    private void logMessage(String message) {
        log(Log.INFO, TAG, message);
    }

    private interface HookInstaller {
        void install() throws Throwable;
    }

    /** Removes the complete Doutu tab cell so the remaining weighted tabs fill the bar. */
    private static final class DoutuTabState {
        private final View tabCell;
        private final LinearLayout parent;
        private final int index;
        private final ViewGroup.LayoutParams layoutParams;
        private final int visibility;

        private DoutuTabState(
                View tabCell,
                LinearLayout parent,
                int index,
                ViewGroup.LayoutParams layoutParams,
                int visibility
        ) {
            this.tabCell = tabCell;
            this.parent = parent;
            this.index = index;
            this.layoutParams = layoutParams;
            this.visibility = visibility;
        }

        static DoutuTabState capture(TextView tab) {
            View tabCell = tab;
            ViewParent parentObject = tabCell.getParent();
            while (parentObject instanceof ViewGroup && !(parentObject instanceof LinearLayout)) {
                tabCell = (View) parentObject;
                parentObject = tabCell.getParent();
            }
            if (!(parentObject instanceof LinearLayout)) {
                return new DoutuTabState(
                        tabCell, null, -1, tabCell.getLayoutParams(), tabCell.getVisibility()
                );
            }
            LinearLayout parent = (LinearLayout) parentObject;
            return new DoutuTabState(
                    tabCell,
                    parent,
                    parent.indexOfChild(tabCell),
                    tabCell.getLayoutParams(),
                    tabCell.getVisibility()
            );
        }

        void apply(boolean hide) {
            if (parent == null) {
                tabCell.setVisibility(hide ? View.GONE : visibility);
                return;
            }
            if (hide) {
                // sb9 validates a page type against getChildCount(). Removing a tab makes
                // type 6 (我的) unreachable; GONE preserves that contract while weighted
                // visible siblings still occupy the complete bar.
                tabCell.setVisibility(View.GONE);
            } else {
                if (tabCell.getParent() == null) {
                    parent.addView(tabCell, Math.min(index, parent.getChildCount()), layoutParams);
                }
                tabCell.setVisibility(visibility);
            }
            parent.requestLayout();
        }
    }

    /** Restores every layout value changed while the emotion-store entry is hidden. */
    private static final class EmotionStoreEntryState {
        private static final int LEFT_OF = 0;
        private static final int RIGHT_OF = 1;
        private static final int ALIGN_PARENT_LEFT = 9;

        private final View root;
        private final View entry;
        private final ViewGroup parent;
        private final int entryVisibility;
        private final int entryIndex;
        private final ViewGroup.LayoutParams entryLayoutParams;
        private final List<RelativeChildState> affectedChildren;

        private EmotionStoreEntryState(
                View root,
                View entry,
                ViewGroup parent,
                int entryVisibility,
                int entryIndex,
                ViewGroup.LayoutParams entryLayoutParams,
                List<RelativeChildState> affectedChildren
        ) {
            this.root = root;
            this.entry = entry;
            this.parent = parent;
            this.entryVisibility = entryVisibility;
            this.entryIndex = entryIndex;
            this.entryLayoutParams = entryLayoutParams;
            this.affectedChildren = affectedChildren;
        }

        static EmotionStoreEntryState capture(View root, View entry) {
            ViewParent parentObject = entry.getParent();
            ViewGroup parent = parentObject instanceof ViewGroup ? (ViewGroup) parentObject : null;
            List<RelativeChildState> children = new ArrayList<>();
            int entryWidth = entry.getLayoutParams() == null ? 0 : entry.getLayoutParams().width;
            boolean entryStartsAtLeft = false;
            if (entry.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
                int[] entryRules = ((RelativeLayout.LayoutParams) entry.getLayoutParams()).getRules();
                entryStartsAtLeft = entryRules[ALIGN_PARENT_LEFT] != 0;
            }
            if (parent instanceof RelativeLayout) {
                for (int index = 0; index < parent.getChildCount(); index++) {
                    View child = parent.getChildAt(index);
                    if (child == entry || !(child.getLayoutParams() instanceof RelativeLayout.LayoutParams)) {
                        continue;
                    }
                    RelativeLayout.LayoutParams params =
                            (RelativeLayout.LayoutParams) child.getLayoutParams();
                    int[] rules = params.getRules();
                    boolean leftOfEntry = rules[LEFT_OF] == entry.getId();
                    boolean rightOfEntry = rules[RIGHT_OF] == entry.getId();
                    boolean contentInset = entryStartsAtLeft
                            && params.width == ViewGroup.LayoutParams.MATCH_PARENT
                            && entryWidth > 0
                            && params.leftMargin >= entryWidth;
                    boolean divider = entryStartsAtLeft
                            && params.width > 0
                            && params.width <= 2
                            && entryWidth > 0
                            && params.leftMargin == entryWidth;
                    if (leftOfEntry || rightOfEntry || contentInset || divider) {
                        children.add(new RelativeChildState(
                                child,
                                leftOfEntry,
                                rightOfEntry,
                                contentInset || rightOfEntry,
                                divider
                        ));
                    }
                }
            }
            return new EmotionStoreEntryState(
                    root,
                    entry,
                    parent,
                    entry.getVisibility(),
                    parent == null ? -1 : parent.indexOfChild(entry),
                    entry.getLayoutParams(),
                    children
            );
        }

        void apply(boolean hide) {
            if (parent instanceof LinearLayout) {
                applyLinearLayout(hide);
                return;
            }
            entry.setVisibility(hide ? View.GONE : entryVisibility);
            for (RelativeChildState child : affectedChildren) {
                child.apply(hide, entryLayoutParams == null ? 0 : entryLayoutParams.width);
            }
            root.requestLayout();
        }

        private void applyLinearLayout(boolean hide) {
            if (hide) {
                if (entry.getParent() == parent) {
                    parent.removeView(entry);
                }
            } else if (entry.getParent() == null) {
                parent.addView(entry, Math.min(entryIndex, parent.getChildCount()), entryLayoutParams);
                entry.setVisibility(entryVisibility);
            }
            root.requestLayout();
        }
    }

    private static final class RelativeChildState {
        private final View child;
        private final boolean leftOfEntry;
        private final boolean rightOfEntry;
        private final boolean reduceLeftMargin;
        private final boolean hideDivider;
        private final int visibility;
        private final int[] rules;
        private final int leftMargin;
        private final int topMargin;
        private final int rightMargin;
        private final int bottomMargin;

        private RelativeChildState(
                View child,
                boolean leftOfEntry,
                boolean rightOfEntry,
                boolean reduceLeftMargin,
                boolean hideDivider
        ) {
            this.child = child;
            this.leftOfEntry = leftOfEntry;
            this.rightOfEntry = rightOfEntry;
            this.reduceLeftMargin = reduceLeftMargin;
            this.hideDivider = hideDivider;
            this.visibility = child.getVisibility();
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) child.getLayoutParams();
            this.rules = params.getRules().clone();
            this.leftMargin = params.leftMargin;
            this.topMargin = params.topMargin;
            this.rightMargin = params.rightMargin;
            this.bottomMargin = params.bottomMargin;
        }

        void apply(boolean hide, int entryWidth) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) child.getLayoutParams();
            if (hide) {
                if (leftOfEntry) {
                    params.removeRule(EmotionStoreEntryState.LEFT_OF);
                }
                if (rightOfEntry) {
                    params.removeRule(EmotionStoreEntryState.RIGHT_OF);
                }
                if (reduceLeftMargin && entryWidth > 0) {
                    params.leftMargin = Math.max(0, leftMargin - entryWidth);
                }
                child.setVisibility(hideDivider ? View.GONE : visibility);
            } else {
                for (int rule = 0; rule < rules.length; rule++) {
                    params.removeRule(rule);
                    if (rules[rule] != 0) {
                        params.addRule(rule, rules[rule]);
                    }
                }
                params.leftMargin = leftMargin;
                params.topMargin = topMargin;
                params.rightMargin = rightMargin;
                params.bottomMargin = bottomMargin;
                child.setVisibility(visibility);
            }
            child.setLayoutParams(params);
        }
    }

    private static final class ShopTabState {
        private final List<String> tags;
        private final String firstTag;
        private final int appliedMask;

        private ShopTabState(List<String> tags, int appliedMask) {
            this.tags = tags;
            this.firstTag = selectShopFallbackTag(tags);
            this.appliedMask = appliedMask;
        }
    }

    private static final class FilteredShopTabs {
        private final List<Object> menuItems;
        private final ShopTabState state;

        private FilteredShopTabs(List<Object> menuItems, ShopTabState state) {
            this.menuItems = menuItems;
            this.state = state;
        }
    }

    private static final class RemovedMenuItem {
        private final int index;
        private final Object item;

        private RemovedMenuItem(int index, Object item) {
            this.index = index;
            this.item = item;
        }
    }

    private static final class HookProfile {
        private final String versionName;
        private final String configClassName;
        private final String panelClassName;
        private final String pasteClassName;
        private final String recordFilterClassName;

        private HookProfile(
                String versionName,
                String configClassName,
                String panelClassName,
                String pasteClassName,
                String recordFilterClassName
        ) {
            this.versionName = versionName;
            this.configClassName = configClassName;
            this.panelClassName = panelClassName;
            this.pasteClassName = pasteClassName;
            this.recordFilterClassName = recordFilterClassName;
        }

        private HookTargets resolve(ClassLoader classLoader)
                throws ClassNotFoundException, NoSuchFieldException, NoSuchMethodException {
            Class<?> configClass = findClass(classLoader, configClassName);
            Class<?> panelClass = findClass(classLoader, panelClassName);
            Class<?> pasteClass = findClass(classLoader, pasteClassName);
            Class<?> recordFilterClass = findClass(classLoader, recordFilterClassName);

            return new HookTargets(
                    this,
                    findField(configClass, "a", int.class),
                    findMethod(configClass, "a", int.class),
                    findMethod(configClass, "d", void.class),
                    findMethod(configClass, "e", void.class, int.class),
                    findField(panelClass, "n", TextView.class),
                    findMethod(panelClass, "O", void.class, List.class),
                    findMethod(pasteClass, "r", String.class, String.class),
                    findMethod(recordFilterClass, "q", void.class, List.class)
            );
        }
    }

    private static final class HookTargets {
        private final HookProfile profile;
        private final Field clipCountField;
        private final Method getClipCountMethod;
        private final Method loadConfigMethod;
        private final Method setClipCountMethod;
        private final Field counterTextField;
        private final Method updatePanelMethod;
        private final Method pasteTruncateMethod;
        private final Method recordLengthFilterMethod;

        private HookTargets(
                HookProfile profile,
                Field clipCountField,
                Method getClipCountMethod,
                Method loadConfigMethod,
                Method setClipCountMethod,
                Field counterTextField,
                Method updatePanelMethod,
                Method pasteTruncateMethod,
                Method recordLengthFilterMethod
        ) {
            this.profile = profile;
            this.clipCountField = clipCountField;
            this.getClipCountMethod = getClipCountMethod;
            this.loadConfigMethod = loadConfigMethod;
            this.setClipCountMethod = setClipCountMethod;
            this.counterTextField = counterTextField;
            this.updatePanelMethod = updatePanelMethod;
            this.pasteTruncateMethod = pasteTruncateMethod;
            this.recordLengthFilterMethod = recordLengthFilterMethod;
        }
    }
}
