package com.fuckbaiduinput;

import android.util.Log;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import io.github.libxposed.api.XposedInterface.ExceptionMode;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public final class HookEntry extends XposedModule {
    private static final String TAG = "FuckBaiduInput";
    private static final String TARGET_PACKAGE = "com.baidu.input_oppo";

    private static final int ORIGINAL_CLIP_COUNT = 0x12c;
    private static final int MAX_CLIP_COUNT = 0x1869f;
    private static final int ORIGINAL_PASTE_TRUNCATE_LENGTH = 0x1b58;
    private static final String ORIGINAL_COUNTER_SUFFIX_ASCII = "/" + ORIGINAL_CLIP_COUNT + ")";
    private static final String MAX_COUNTER_SUFFIX_ASCII = "/" + MAX_CLIP_COUNT + ")";
    private static final String ORIGINAL_COUNTER_SUFFIX_FULL_WIDTH = "/" + ORIGINAL_CLIP_COUNT + "\uff09";
    private static final String MAX_COUNTER_SUFFIX_FULL_WIDTH = "/" + MAX_CLIP_COUNT + "\uff09";
    private static final HookProfile[] HOOK_PROFILES = {
            new HookProfile(
                    "8.5.302.328",
                    "com.baidu.vq1",
                    "com.baidu.er1",
                    "com.baidu.iq1",
                    "com.baidu.er1$i"
            ),
            new HookProfile(
                    "8.5.302.367",
                    "com.baidu.uq1",
                    "com.baidu.dr1",
                    "com.baidu.hq1",
                    "com.baidu.dr1$i"
            )
    };

    @Override
    public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        if (!TARGET_PACKAGE.equals(param.getPackageName())) {
            return;
        }

        ClassLoader classLoader = param.getClassLoader();
        HookTargets targets = findMatchingTargets(classLoader);
        if (targets == null) {
            logMessage("no matching hook profile in " + param.getPackageName());
            return;
        }

        logMessage("loading in " + param.getPackageName() + " with profile " + targets.profile.versionName);
        hookClipboardConfig(targets);
        hookClipboardPanel(targets);
        hookPasteTruncation(targets);
        hookRecordLengthFilter(targets);
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
                    if (shouldLiftClipCount(result)) {
                        return MAX_CLIP_COUNT;
                    }
                    return result;
                }));

        safe(targets.profile.configClassName + ".d", () -> hook(targets.loadConfigMethod)
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object result = chain.proceed();
                    enforceClipCountField(chain.getThisObject(), targets);
                    return result;
                }));

        safe(targets.profile.configClassName + ".e", () -> hook(targets.setClipCountMethod)
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object count = chain.getArg(0);
                    if (shouldLiftClipCount(count)) {
                        return chain.proceed(new Object[] { MAX_CLIP_COUNT });
                    }
                    return chain.proceed();
                }));
    }

    private void hookClipboardPanel(final HookTargets targets) {
        safe(targets.profile.panelClassName + ".O", () -> hook(targets.updatePanelMethod)
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object result = chain.proceed();
                    updateCounterText(chain.getThisObject(), targets);
                    return result;
                }));
    }

    private void hookPasteTruncation(final HookTargets targets) {
        safe(targets.profile.pasteClassName + ".r", () -> hook(targets.pasteTruncateMethod)
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    String input = (String) chain.getArg(0);
                    if (input != null && input.length() > ORIGINAL_PASTE_TRUNCATE_LENGTH) {
                        return input;
                    }
                    return chain.proceed();
                }));
    }

    private void hookRecordLengthFilter(final HookTargets targets) {
        safe(targets.profile.recordFilterClassName + ".q", () -> hook(targets.recordLengthFilterMethod)
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> null));
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
