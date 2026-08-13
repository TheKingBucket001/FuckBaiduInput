package com.fuckbaiduinput;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds module controls with the host's own COUI preference classes. */
final class HostSettingsUi {
    static final String ACTION = "com.fuckbaiduinput.action.HOOK_SETTINGS";
    static final String EXTRA_PAGE = "com.fuckbaiduinput.extra.PAGE";
    static final String EXTRA_SCHEMA = "com.fuckbaiduinput.extra.SCHEMA";

    static final String PAGE_ROOT = "root";
    static final String PAGE_CLIPBOARD = "clipboard";
    static final String PAGE_ACCOUNT_CLOUD = "account_cloud";
    static final String PAGE_RECOMMENDATION = "recommendation";
    static final String PAGE_ADS = "ads";
    static final String PAGE_PRIVACY = "privacy";
    static final String PAGE_SKIN_STORE = "skin_store";
    static final String PAGE_UI_TOOLS = "ui_tools";
    static final String PAGE_HIDDEN = "hidden";

    private static final String TARGET_PACKAGE = "com.baidu.input_oppo";
    private static final String HOST_SUB_CONFIG_ACTIVITY =
            "com.baidu.input.ImeSubConfigActivity";
    private static final String HOST_TURBO_PREFERENCE_KEY = "pref_key_turbo_mode";
    private static final String ENTRY_KEY = "com.fuckbaiduinput.preference.HOOK_SETTINGS";
    private static final String PAGE_KEY_PREFIX = "com.fuckbaiduinput.preference.PAGE.";
    private static final String BATCH_CATEGORY_KEY =
            "com.fuckbaiduinput.preference.BATCH_CATEGORY";
    private static final String SET_TYPE_EXTRA = "settype";
    private static final String TITLE_EXTRA = "title";
    private static final byte MODULE_PAGE_TYPE = 18;
    private static final int MODULE_SCHEMA = 1;
    private static final String MODULE_TITLE = "\ud83d\udeab fuckinginput";
    private static final int BRIDGE_REQUEST_CODE = 0x4642;
    private static final int DESTINATION_REQUEST_CODE = 0x4643;

    private static final HookFeature[] CLIPBOARD_FEATURES = {
            HookFeature.CLIPBOARD_CAPACITY,
            HookFeature.CLIPBOARD_LONG_TEXT,
            HookFeature.CLIPBOARD_NO_RECOGNITION
    };
    private static final HookFeature[] ACCOUNT_CLOUD_FEATURES = {
            HookFeature.ACCOUNT_ISOLATION,
            HookFeature.CLOUD_BACKUP_SYNC,
            HookFeature.CLOUD_OPTIMIZATION,
            HookFeature.CLOUD_INPUT
    };
    private static final HookFeature[] RECOMMENDATION_FEATURES = {
            HookFeature.WISDOM_RECOMMENDATION,
            HookFeature.SCENARIO_RECOMMENDATION
    };
    private static final HookFeature[] ADS_FEATURES = {
            HookFeature.AD_SDK_BLOCK,
            HookFeature.SHOP_PROMOTION_CLEANUP,
            HookFeature.ACTIVITY_RECOMMENDATION
    };
    private static final HookFeature[] PRIVACY_FEATURES = {
            HookFeature.PRIVACY_TELEMETRY,
            HookFeature.FEEDBACK_BLOCK,
            HookFeature.BACKGROUND_UPDATE_CHECK
    };
    private static final HookFeature[] SKIN_STORE_FEATURES = {
            HookFeature.ONLINE_SKIN_SHOP_CLEANUP,
            HookFeature.EMOTION_SHOP_CLEANUP,
            HookFeature.FONT_SHOP_CLEANUP,
            HookFeature.REMOTE_SKIN_UPGRADE,
            HookFeature.HIDE_SETTINGS_SKIN_ENTRY
    };
    private static final HookFeature[] UI_TOOLS_FEATURES = {
            HookFeature.HIDE_SEARCH,
            HookFeature.HIDE_MECHANICAL_KEYBOARD,
            HookFeature.HIDE_FONT_SETTING,
            HookFeature.HIDE_AI_WRITER,
            HookFeature.CUSTOM_KEYBOARD_LOGO
    };

    interface FeatureAccess {
        boolean isEnabled(HookFeature feature);

        boolean writeFlags(Context context, Map<HookFeature, Boolean> flags);
    }

    interface Logger {
        void log(String message);
    }

    private interface ClickAction {
        boolean click();
    }

    private interface ChangeAction {
        boolean change(Object value);
    }

    private final FeatureAccess featureAccess;
    private final Logger logger;
    private final ClassLoader hostClassLoader;
    private final Class<?> preferenceClass;
    private final Class<?> preferenceScreenClass;
    private final Class<?> clickListenerClass;
    private final Class<?> changeListenerClass;
    private final Constructor<?> jumpPreferenceConstructor;
    private final Constructor<?> switchPreferenceConstructor;
    private final Constructor<?> categoryConstructor;
    private final Field categoryTopMarginTypeField;
    private final Method categoryBindMethod;
    private final Method getPreferenceKeyMethod;
    private final Field holderItemViewField;
    private final Method getActivityMethod;
    private final Method getPreferenceScreenMethod;
    private final Method addPreferenceMethod;
    private final Method clearPreferencesMethod;
    private final Method findPreferenceMethod;
    private final Method setKeyMethod;
    private final Method setTitleMethod;
    private final Method setSummaryMethod;
    private final Method setOrderMethod;
    private final Method setClickListenerMethod;
    private final Method setChangeListenerMethod;
    private final Method setCheckedMethod;
    private String lastStartedPage;
    private long lastStartedAt;
    private boolean compactStyleFailureLogged;

    private HostSettingsUi(
            FeatureAccess featureAccess,
            Logger logger,
            ClassLoader hostClassLoader,
            Class<?> preferenceClass,
            Class<?> preferenceScreenClass,
            Class<?> clickListenerClass,
            Class<?> changeListenerClass,
            Constructor<?> jumpPreferenceConstructor,
            Constructor<?> switchPreferenceConstructor,
            Constructor<?> categoryConstructor,
            Field categoryTopMarginTypeField,
            Method categoryBindMethod,
            Method getPreferenceKeyMethod,
            Field holderItemViewField,
            Method getActivityMethod,
            Method getPreferenceScreenMethod,
            Method addPreferenceMethod,
            Method clearPreferencesMethod,
            Method findPreferenceMethod,
            Method setKeyMethod,
            Method setTitleMethod,
            Method setSummaryMethod,
            Method setOrderMethod,
            Method setClickListenerMethod,
            Method setChangeListenerMethod,
            Method setCheckedMethod
    ) {
        this.featureAccess = featureAccess;
        this.logger = logger;
        this.hostClassLoader = hostClassLoader;
        this.preferenceClass = preferenceClass;
        this.preferenceScreenClass = preferenceScreenClass;
        this.clickListenerClass = clickListenerClass;
        this.changeListenerClass = changeListenerClass;
        this.jumpPreferenceConstructor = jumpPreferenceConstructor;
        this.switchPreferenceConstructor = switchPreferenceConstructor;
        this.categoryConstructor = categoryConstructor;
        this.categoryTopMarginTypeField = categoryTopMarginTypeField;
        this.categoryBindMethod = categoryBindMethod;
        this.getPreferenceKeyMethod = getPreferenceKeyMethod;
        this.holderItemViewField = holderItemViewField;
        this.getActivityMethod = getActivityMethod;
        this.getPreferenceScreenMethod = getPreferenceScreenMethod;
        this.addPreferenceMethod = addPreferenceMethod;
        this.clearPreferencesMethod = clearPreferencesMethod;
        this.findPreferenceMethod = findPreferenceMethod;
        this.setKeyMethod = setKeyMethod;
        this.setTitleMethod = setTitleMethod;
        this.setSummaryMethod = setSummaryMethod;
        this.setOrderMethod = setOrderMethod;
        this.setClickListenerMethod = setClickListenerMethod;
        this.setChangeListenerMethod = setChangeListenerMethod;
        this.setCheckedMethod = setCheckedMethod;
    }

    static HostSettingsUi resolve(
            ClassLoader classLoader,
            FeatureAccess featureAccess,
            Logger logger
    ) throws ReflectiveOperationException {
        Class<?> fragmentClass = findClass(classLoader, "androidx.fragment.app.Fragment");
        Class<?> fragmentActivityClass = findClass(
                classLoader,
                "androidx.fragment.app.FragmentActivity"
        );
        Class<?> preferenceFragmentClass = findClass(classLoader, "androidx.preference.d");
        Class<?> preferenceClass = findClass(classLoader, "androidx.preference.Preference");
        Class<?> preferenceGroupClass = findClass(
                classLoader,
                "androidx.preference.PreferenceGroup"
        );
        Class<?> preferenceScreenClass = findClass(
                classLoader,
                "androidx.preference.PreferenceScreen"
        );
        Class<?> twoStatePreferenceClass = findClass(
                classLoader,
                "androidx.preference.TwoStatePreference"
        );
        Class<?> clickListenerClass = findClass(
                classLoader,
                "androidx.preference.Preference$d"
        );
        Class<?> changeListenerClass = findClass(
                classLoader,
                "androidx.preference.Preference$c"
        );
        Class<?> jumpPreferenceClass = findClass(
                classLoader,
                "com.coui.appcompat.preference.COUIJumpPreference"
        );
        Class<?> switchPreferenceClass = findClass(
                classLoader,
                "com.coui.appcompat.preference.COUISwitchPreference"
        );
        Class<?> categoryClass = findClass(
                classLoader,
                "com.coui.appcompat.preference.COUIPreferenceCategory"
        );
        Class<?> preferenceViewHolderClass = findClass(classLoader, "com.baidu.r2a");

        return new HostSettingsUi(
                featureAccess,
                logger,
                classLoader,
                preferenceClass,
                preferenceScreenClass,
                clickListenerClass,
                changeListenerClass,
                findConstructor(jumpPreferenceClass, Context.class),
                findConstructor(switchPreferenceClass, Context.class),
                findConstructor(categoryClass, Context.class, AttributeSet.class),
                findOptionalIntField(categoryClass, "w0", logger),
                findOptionalMethod(
                        categoryClass,
                        "z0",
                        void.class,
                        logger,
                        preferenceViewHolderClass
                ),
                findOptionalMethod(preferenceClass, "P", String.class, logger),
                findOptionalPublicField(preferenceViewHolderClass, "itemView", View.class, logger),
                findMethod(fragmentClass, "getActivity", fragmentActivityClass),
                findMethod(preferenceFragmentClass, "D", preferenceScreenClass),
                findMethod(
                        preferenceGroupClass,
                        "u1",
                        boolean.class,
                        preferenceClass
                ),
                findMethod(preferenceGroupClass, "C1", void.class),
                findMethod(
                        preferenceGroupClass,
                        "v1",
                        preferenceClass,
                        CharSequence.class
                ),
                findMethod(preferenceClass, "e1", void.class, String.class),
                findMethod(preferenceClass, "n1", void.class, CharSequence.class),
                findMethod(preferenceClass, "k1", void.class, CharSequence.class),
                findMethod(preferenceClass, "j1", void.class, int.class),
                findMethod(
                        preferenceClass,
                        "i1",
                        void.class,
                        clickListenerClass
                ),
                findMethod(
                        preferenceClass,
                        "h1",
                        void.class,
                        changeListenerClass
                ),
                findMethod(twoStatePreferenceClass, "u1", void.class, boolean.class)
        );
    }

    void injectRootEntry(Object fragment) {
        try {
            Activity activity = getActivity(fragment);
            Object screen = getScreen(fragment);
            if (activity == null || screen == null) {
                return;
            }
            Object existing = findPreferenceMethod.invoke(screen, ENTRY_KEY);
            if (existing != null) {
                return;
            }

            Object entry = newJumpPreference(activity);
            setKeyMethod.invoke(entry, ENTRY_KEY);
            setTitleMethod.invoke(entry, MODULE_TITLE);
            setOrderMethod.invoke(entry, -1);
            setClickListenerMethod.invoke(entry, clickProxy(() -> {
                startModulePage(activity, PAGE_ROOT, MODULE_TITLE);
                return true;
            }));
            add(screen, entry);
        } catch (Throwable t) {
            logger.log("host settings entry injection failed: " + t);
        }
    }

    /** Returns true only for an authenticated module-owned settype 18 page. */
    boolean populateMarkedPage(Object fragment) {
        Activity activity = null;
        try {
            activity = getActivity(fragment);
            String page = markedPage(activity);
            if (page == null) {
                return false;
            }
            Object screen = getScreen(fragment);
            if (screen == null) {
                throw new IllegalStateException("missing preference screen");
            }
            clearPreferencesMethod.invoke(screen);
            if (PAGE_ROOT.equals(page)) {
                buildRootPage(activity, screen);
            } else if (PAGE_HIDDEN.equals(page)) {
                buildHiddenPage(activity, screen);
            } else {
                HookFeature[] features = featuresForPage(page);
                if (features == null) {
                    throw new IllegalArgumentException("unknown marked page");
                }
                buildFeaturePage(activity, screen, features);
            }
            return true;
        } catch (Throwable t) {
            logger.log("host settings page population failed: " + t);
            if (activity != null && markedPage(activity) != null) {
                activity.finish();
                return true;
            }
            return false;
        }
    }

    Method categoryBindMethod() {
        return categoryBindMethod;
    }

    void compactBatchCategory(Object category, Object holder) {
        if (category == null || holder == null
                || getPreferenceKeyMethod == null || holderItemViewField == null) {
            return;
        }
        try {
            Object key = getPreferenceKeyMethod.invoke(category);
            if (!BATCH_CATEGORY_KEY.equals(key)) {
                return;
            }
            Object value = holderItemViewField.get(holder);
            if (!(value instanceof View)) {
                return;
            }
            View itemView = (View) value;
            ViewGroup.LayoutParams params = itemView.getLayoutParams();
            if (!(params instanceof ViewGroup.MarginLayoutParams)) {
                return;
            }
            ViewGroup.MarginLayoutParams margins = (ViewGroup.MarginLayoutParams) params;
            // Keep the host category's normal first-row extension. The module page shares
            // the same RecyclerView as native settings, where removing it lifts the header.
            if (margins.topMargin != 0) {
                margins.topMargin = 0;
                itemView.setLayoutParams(margins);
            }
        } catch (Throwable t) {
            logCompactStyleFailure(t);
        }
    }

    private void buildRootPage(Activity activity, Object screen) throws ReflectiveOperationException {
        addPageJump(activity, screen, PAGE_CLIPBOARD, "\u526a\u8d34\u677f");
        addPageJump(activity, screen, PAGE_ACCOUNT_CLOUD, "\u8d26\u53f7\u4e0e\u4e91");
        addPageJump(activity, screen, PAGE_RECOMMENDATION, "\u63a8\u8350");
        addPageJump(activity, screen, PAGE_ADS, "\u5e7f\u544a\u4e0e\u63a8\u5e7f");
        addPageJump(activity, screen, PAGE_PRIVACY, "\u9690\u79c1\u4e0e\u540e\u53f0");
        addPageJump(activity, screen, PAGE_SKIN_STORE, "\u76ae\u80a4\u4e0e\u5546\u5e97");
        addPageJump(activity, screen, PAGE_UI_TOOLS, "\u754c\u9762\u4e0e\u5de5\u5177\u7bb1");
        addPageJump(activity, screen, PAGE_HIDDEN, "\u9690\u85cf\u9875\u9762");
    }

    private void addPageJump(
            Activity activity,
            Object screen,
            String page,
            String title
    ) throws ReflectiveOperationException {
        Object preference = newJumpPreference(activity);
        setKeyMethod.invoke(preference, PAGE_KEY_PREFIX + page);
        setTitleMethod.invoke(preference, title);
        setClickListenerMethod.invoke(preference, clickProxy(() -> {
            startModulePage(activity, page, title);
            return true;
        }));
        add(screen, preference);
    }

    private void buildFeaturePage(
            Activity activity,
            Object screen,
            HookFeature[] features
    ) throws ReflectiveOperationException {
        Object batchCategory = newCategory(activity);
        applyFirstCategoryStyle(batchCategory);
        setKeyMethod.invoke(batchCategory, BATCH_CATEGORY_KEY);
        setTitleMethod.invoke(batchCategory, "\u6279\u91cf\u8bbe\u7f6e");
        add(screen, batchCategory);

        Object batchSwitch = newSwitchPreference(activity);
        setTitleMethod.invoke(batchSwitch, "\u672c\u7ec4\u5168\u90e8\u542f\u7528");
        setCheckedMethod.invoke(batchSwitch, allEnabled(features));
        List<Object> childSwitches = new ArrayList<>(features.length);
        setChangeListenerMethod.invoke(batchSwitch, changeProxy(value -> {
            if (!(value instanceof Boolean)) {
                return false;
            }
            boolean enabled = (Boolean) value;
            Map<HookFeature, Boolean> changes = new LinkedHashMap<>();
            for (HookFeature feature : features) {
                changes.put(feature, enabled);
            }
            if (!featureAccess.writeFlags(activity, changes)) {
                return false;
            }
            try {
                for (Object child : childSwitches) {
                    setCheckedMethod.invoke(child, enabled);
                }
            } catch (Throwable t) {
                logger.log("batch switch refresh failed: " + t);
            }
            return true;
        }));
        setClickListenerMethod.invoke(batchSwitch, clickProxy(() -> true));
        add(batchCategory, batchSwitch);

        Object featureCategory = newCategory(activity);
        setTitleMethod.invoke(featureCategory, "\u529f\u80fd");
        add(screen, featureCategory);

        for (HookFeature feature : features) {
            Object child = newSwitchPreference(activity);
            childSwitches.add(child);
            setTitleMethod.invoke(child, featureTitle(feature));
            String summary = featureSummary(feature);
            if (summary != null) {
                setSummaryMethod.invoke(child, summary);
            }
            setCheckedMethod.invoke(child, featureAccess.isEnabled(feature));
            setChangeListenerMethod.invoke(child, changeProxy(value -> {
                if (!(value instanceof Boolean)) {
                    return false;
                }
                Map<HookFeature, Boolean> changes = new LinkedHashMap<>();
                changes.put(feature, (Boolean) value);
                if (!featureAccess.writeFlags(activity, changes)) {
                    return false;
                }
                try {
                    setCheckedMethod.invoke(batchSwitch, allEnabled(features));
                } catch (Throwable t) {
                    logger.log("category switch refresh failed: " + t);
                }
                return true;
            }));
            setClickListenerMethod.invoke(child, clickProxy(() -> true));
            add(featureCategory, child);
        }
    }

    private void buildHiddenPage(Activity activity, Object screen)
            throws ReflectiveOperationException {
        Object category = newCategory(activity);
        applyFirstCategoryStyle(category);
        setTitleMethod.invoke(category, "\u5df2\u9a8c\u8bc1\u9875\u9762");
        add(screen, category);

        Object turbo = newJumpPreference(activity);
        setKeyMethod.invoke(turbo, HOST_TURBO_PREFERENCE_KEY);
        setTitleMethod.invoke(turbo, "\u6781\u901f\u6a21\u5f0f\u8be6\u7ec6\u8bbe\u7f6e");
        // No click listener: the host's l2a dispatcher opens the route-113 destination.
        add(category, turbo);
    }

    private Object newJumpPreference(Context context) throws ReflectiveOperationException {
        return jumpPreferenceConstructor.newInstance(context);
    }

    private Object newSwitchPreference(Context context) throws ReflectiveOperationException {
        return switchPreferenceConstructor.newInstance(context);
    }

    private Object newCategory(Context context) throws ReflectiveOperationException {
        return categoryConstructor.newInstance(context, null);
    }

    private Activity getActivity(Object fragment) throws ReflectiveOperationException {
        Object value = fragment == null ? null : getActivityMethod.invoke(fragment);
        return value instanceof Activity ? (Activity) value : null;
    }

    private Object getScreen(Object fragment) throws ReflectiveOperationException {
        Object value = fragment == null ? null : getPreferenceScreenMethod.invoke(fragment);
        return preferenceScreenClass.isInstance(value) ? value : null;
    }

    private void add(Object group, Object preference) throws ReflectiveOperationException {
        if (group == null || !preferenceClass.isInstance(preference)) {
            throw new IllegalArgumentException("invalid preference node");
        }
        Object added = addPreferenceMethod.invoke(group, preference);
        if (!(added instanceof Boolean) || !((Boolean) added)) {
            throw new IllegalStateException("host rejected preference node");
        }
    }

    private Object clickProxy(ClickAction action) {
        return Proxy.newProxyInstance(
                hostClassLoader,
                new Class<?>[] { clickListenerClass },
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return handleObjectMethod(proxy, method, args);
                    }
                    if ("a".equals(method.getName()) && args != null && args.length == 1) {
                        return action.click();
                    }
                    return false;
                }
        );
    }

    private Object changeProxy(ChangeAction action) {
        return Proxy.newProxyInstance(
                hostClassLoader,
                new Class<?>[] { changeListenerClass },
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return handleObjectMethod(proxy, method, args);
                    }
                    if ("t".equals(method.getName()) && args != null && args.length == 2) {
                        return action.change(args[1]);
                    }
                    return false;
                }
        );
    }

    private static Object handleObjectMethod(Object proxy, Method method, Object[] args) {
        if ("toString".equals(method.getName())) {
            return "HookSettingsPreferenceListener";
        }
        if ("hashCode".equals(method.getName())) {
            return System.identityHashCode(proxy);
        }
        if ("equals".equals(method.getName())) {
            return args != null && args.length == 1 && proxy == args[0];
        }
        return null;
    }

    private void startModulePage(Activity activity, String page, String title) {
        try {
            if (activity == null || !isAllowedPage(page)) {
                return;
            }
            long now = SystemClock.elapsedRealtime();
            if (page.equals(lastStartedPage) && now - lastStartedAt < 500L) {
                return;
            }
            lastStartedPage = page;
            lastStartedAt = now;
            Intent destination = new Intent();
            destination.setComponent(new ComponentName(TARGET_PACKAGE, HOST_SUB_CONFIG_ACTIVITY));
            destination.setAction(ACTION);
            destination.putExtra(SET_TYPE_EXTRA, MODULE_PAGE_TYPE);
            destination.putExtra(EXTRA_SCHEMA, MODULE_SCHEMA);
            destination.putExtra(EXTRA_PAGE, page);
            destination.putExtra(TITLE_EXTRA, title);
            if (!PAGE_ROOT.equals(page)) {
                activity.startActivity(destination);
                return;
            }

            PendingIntent pendingDestination = PendingIntent.getActivity(
                    activity,
                    DESTINATION_REQUEST_CODE,
                    destination,
                    PendingIntent.FLAG_CANCEL_CURRENT
                            | PendingIntent.FLAG_ONE_SHOT
                            | PendingIntent.FLAG_IMMUTABLE
            );
            Intent bridge = new Intent(HookSettingsContract.ACTION_PREPARE_SETTINGS);
            bridge.setComponent(new ComponentName(
                    HookSettingsContract.MODULE_PACKAGE,
                    HookSettingsContract.BRIDGE_ACTIVITY
            ));
            bridge.putExtra(HookSettingsContract.EXTRA_DESTINATION, pendingDestination);
            activity.startActivityForResult(bridge, BRIDGE_REQUEST_CODE);
        } catch (Throwable t) {
            logger.log("host settings navigation failed: " + t);
        }
    }

    private String markedPage(Activity activity) {
        try {
            if (activity == null || !TARGET_PACKAGE.equals(activity.getPackageName())) {
                return null;
            }
            Intent intent = activity.getIntent();
            if (intent == null || !ACTION.equals(intent.getAction())) {
                return null;
            }
            ComponentName component = intent.getComponent();
            if (component == null
                    || !TARGET_PACKAGE.equals(component.getPackageName())
                    || !HOST_SUB_CONFIG_ACTIVITY.equals(component.getClassName())) {
                return null;
            }
            Bundle extras = intent.getExtras();
            if (extras == null) {
                return null;
            }
            Object setType = extras.get(SET_TYPE_EXTRA);
            Object schema = extras.get(EXTRA_SCHEMA);
            Object page = extras.get(EXTRA_PAGE);
            if (!(setType instanceof Byte) || ((Byte) setType) != MODULE_PAGE_TYPE
                    || !(schema instanceof Integer) || ((Integer) schema) != MODULE_SCHEMA
                    || !(page instanceof String) || !isAllowedPage((String) page)) {
                return null;
            }
            return (String) page;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private boolean allEnabled(HookFeature[] features) {
        for (HookFeature feature : features) {
            if (!featureAccess.isEnabled(feature)) {
                return false;
            }
        }
        return true;
    }

    private static HookFeature[] featuresForPage(String page) {
        if (PAGE_CLIPBOARD.equals(page)) {
            return CLIPBOARD_FEATURES;
        }
        if (PAGE_ACCOUNT_CLOUD.equals(page)) {
            return ACCOUNT_CLOUD_FEATURES;
        }
        if (PAGE_RECOMMENDATION.equals(page)) {
            return RECOMMENDATION_FEATURES;
        }
        if (PAGE_ADS.equals(page)) {
            return ADS_FEATURES;
        }
        if (PAGE_PRIVACY.equals(page)) {
            return PRIVACY_FEATURES;
        }
        if (PAGE_SKIN_STORE.equals(page)) {
            return SKIN_STORE_FEATURES;
        }
        if (PAGE_UI_TOOLS.equals(page)) {
            return UI_TOOLS_FEATURES;
        }
        return null;
    }

    private static boolean isAllowedPage(String page) {
        return PAGE_ROOT.equals(page)
                || PAGE_CLIPBOARD.equals(page)
                || PAGE_ACCOUNT_CLOUD.equals(page)
                || PAGE_RECOMMENDATION.equals(page)
                || PAGE_ADS.equals(page)
                || PAGE_PRIVACY.equals(page)
                || PAGE_SKIN_STORE.equals(page)
                || PAGE_UI_TOOLS.equals(page)
                || PAGE_HIDDEN.equals(page);
    }

    private static String featureTitle(HookFeature feature) {
        switch (feature) {
            case CLIPBOARD_CAPACITY:
                return "\u6269\u5c55\u526a\u8d34\u677f\u5bb9\u91cf";
            case CLIPBOARD_LONG_TEXT:
                return "\u4fdd\u7559\u957f\u6587\u672c";
            case CLIPBOARD_NO_RECOGNITION:
                return "\u5173\u95ed\u526a\u8d34\u677f\u5185\u5bb9\u8bc6\u522b";
            case ACCOUNT_ISOLATION:
                return "\u9694\u79bb\u767e\u5ea6\u8d26\u53f7";
            case CLOUD_BACKUP_SYNC:
                return "\u5173\u95ed\u4e91\u5907\u4efd\u4e0e\u540c\u6b65";
            case CLOUD_OPTIMIZATION:
                return "\u5173\u95ed\u4e91\u4f18\u5316";
            case CLOUD_INPUT:
                return "\u5173\u95ed\u4e91\u8f93\u5165";
            case WISDOM_RECOMMENDATION:
                return "\u5173\u95ed\u667a\u6167\u63a8\u8350";
            case SCENARIO_RECOMMENDATION:
                return "\u5173\u95ed\u573a\u666f\u5316\u667a\u80fd\u63a8\u8350";
            case AD_SDK_BLOCK:
                return "\u5173\u95ed\u5e7f\u544a SDK";
            case SHOP_PROMOTION_CLEANUP:
                return "\u6e05\u7406\u5546\u5e97\u63a8\u5e7f";
            case ACTIVITY_RECOMMENDATION:
                return "\u5173\u95ed\u6d3b\u52a8\u63a8\u8350";
            case PRIVACY_TELEMETRY:
                return "\u5173\u95ed\u7edf\u8ba1\u4e0e\u5d29\u6e83\u4e0a\u4f20";
            case FEEDBACK_BLOCK:
                return "\u5173\u95ed\u95ee\u9898\u53cd\u9988";
            case BACKGROUND_UPDATE_CHECK:
                return "\u5173\u95ed\u540e\u53f0\u66f4\u65b0\u68c0\u67e5";
            case ONLINE_SKIN_SHOP_CLEANUP:
                return "\u7cbe\u7b80\u5728\u7ebf\u76ae\u80a4\u5546\u5e97";
            case EMOTION_SHOP_CLEANUP:
                return "\u7cbe\u7b80\u8868\u60c5\u5546\u5e97";
            case FONT_SHOP_CLEANUP:
                return "\u7cbe\u7b80\u5b57\u4f53\u754c\u9762";
            case REMOTE_SKIN_UPGRADE:
                return "\u5173\u95ed\u8fdc\u7a0b\u76ae\u80a4\u5347\u7ea7";
            case HIDE_SETTINGS_SKIN_ENTRY:
                return "\u9690\u85cf\u8bbe\u7f6e\u9875\u76ae\u80a4\u5165\u53e3";
            case HIDE_SEARCH:
                return "\u9690\u85cf\u641c\u7d22";
            case HIDE_MECHANICAL_KEYBOARD:
                return "\u9690\u85cf\u673a\u68b0\u952e\u76d8";
            case HIDE_FONT_SETTING:
                return "\u9690\u85cf\u5b57\u4f53\u8bbe\u7f6e";
            case HIDE_AI_WRITER:
                return "\u9690\u85cf AI \u5199\u4f5c\u5165\u53e3";
            case CUSTOM_KEYBOARD_LOGO:
                return "\u66ff\u6362\u7eaf\u51c0\u6a21\u5f0f\u5de6\u4e0a\u89d2\u56fe\u6807";
            default:
                throw new IllegalArgumentException("unknown feature");
        }
    }

    private static String featureSummary(HookFeature feature) {
        switch (feature) {
            case CLIPBOARD_CAPACITY:
                return "\u5bb9\u91cf\u4e0a\u9650 99999 \u6761\uff0c\u5173\u95ed\u540e\u9700\u91cd\u542f\u8f93\u5165\u6cd5\u6062\u590d";
            case ACCOUNT_ISOLATION:
                return "\u9700\u91cd\u542f\u8f93\u5165\u6cd5\u540e\u5b8c\u6574\u751f\u6548\uff1b"
                        + "\u767b\u5f55\u3001\u8d2d\u4e70\u53ca\u8d26\u53f7\u5728\u7ebf\u8d44\u4ea7"
                        + "\u4e0d\u53ef\u7528\uff0c\u79bb\u7ebf\u76ae\u80a4\u4e0d\u53d7\u5f71\u54cd";
            case AD_SDK_BLOCK:
            case PRIVACY_TELEMETRY:
            case BACKGROUND_UPDATE_CHECK:
            case REMOTE_SKIN_UPGRADE:
            case ACTIVITY_RECOMMENDATION:
                return "\u9700\u91cd\u542f\u8f93\u5165\u6cd5\u540e\u5b8c\u6574\u751f\u6548";
            case CUSTOM_KEYBOARD_LOGO:
                return "\u4ee5\u521d\u97f3\u672a\u6765 Emoji \u56fe\u6807\u66ff\u6362\u7eaf\u51c0\u6a21\u5f0f\u5165\u53e3\uff0c\u5173\u95ed\u540e\u6062\u590d\u5bbf\u4e3b\u56fe\u6807";
            default:
                return null;
        }
    }

    private void applyFirstCategoryStyle(Object category) {
        if (categoryTopMarginTypeField == null) {
            return;
        }
        try {
            categoryTopMarginTypeField.setInt(category, 2);
        } catch (Throwable t) {
            logOptionalStyleFailure(logger, "host first category style failed: ", t);
        }
    }

    private void logCompactStyleFailure(Throwable error) {
        if (compactStyleFailureLogged) {
            return;
        }
        compactStyleFailureLogged = true;
        logOptionalStyleFailure(logger, "host compact category style failed: ", error);
    }

    private static Class<?> findClass(ClassLoader classLoader, String name)
            throws ClassNotFoundException {
        return Class.forName(name, false, classLoader);
    }

    private static Constructor<?> findConstructor(Class<?> type, Class<?>... parameters)
            throws NoSuchMethodException {
        Constructor<?> constructor = type.getDeclaredConstructor(parameters);
        constructor.setAccessible(true);
        return constructor;
    }

    private static Field findOptionalIntField(Class<?> type, String name, Logger logger) {
        try {
            Field field = type.getDeclaredField(name);
            if (!int.class.equals(field.getType())) {
                throw new NoSuchFieldException(type.getName() + "." + name + " type");
            }
            field.setAccessible(true);
            return field;
        } catch (Throwable t) {
            logOptionalStyleFailure(logger, "host first category style unavailable: ", t);
            return null;
        }
    }

    private static Method findOptionalMethod(
            Class<?> type,
            String name,
            Class<?> returnType,
            Logger logger,
            Class<?>... parameters
    ) {
        try {
            return findMethod(type, name, returnType, parameters);
        } catch (Throwable t) {
            logOptionalStyleFailure(logger, "host compact category method unavailable: ", t);
            return null;
        }
    }

    private static Field findOptionalPublicField(
            Class<?> type,
            String name,
            Class<?> fieldType,
            Logger logger
    ) {
        try {
            Field field = type.getField(name);
            if (!fieldType.equals(field.getType())) {
                throw new NoSuchFieldException(type.getName() + "." + name + " type");
            }
            field.setAccessible(true);
            return field;
        } catch (Throwable t) {
            logOptionalStyleFailure(logger, "host compact category field unavailable: ", t);
            return null;
        }
    }

    private static void logOptionalStyleFailure(Logger logger, String prefix, Throwable error) {
        try {
            logger.log(prefix + error);
        } catch (Throwable ignored) {
            // Optional styling must never affect the settings page.
        }
    }

    private static Method findMethod(
            Class<?> type,
            String name,
            Class<?> returnType,
            Class<?>... parameters
    ) throws NoSuchMethodException {
        Method method = type.getDeclaredMethod(name, parameters);
        if (!returnType.equals(method.getReturnType())) {
            throw new NoSuchMethodException(type.getName() + "." + name + " return type");
        }
        method.setAccessible(true);
        return method;
    }
}
