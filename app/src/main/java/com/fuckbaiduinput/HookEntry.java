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
        HookProfile profile = findMatchingProfile(classLoader);
        if (profile == null) {
            logMessage("no matching hook profile in " + param.getPackageName());
            return;
        }

        logMessage("loading in " + param.getPackageName() + " with profile " + profile.versionName);
        hookClipboardConfig(classLoader, profile);
        hookClipboardPanel(classLoader, profile);
        hookPasteTruncation(classLoader, profile);
        hookRecordLengthFilter(classLoader, profile);
    }

    private HookProfile findMatchingProfile(ClassLoader classLoader) {
        for (HookProfile profile : HOOK_PROFILES) {
            if (profile.matches(classLoader)) {
                return profile;
            }
        }
        return null;
    }

    private void hookClipboardConfig(final ClassLoader classLoader, final HookProfile profile) {
        safe(profile.configClassName + ".a", () -> hook(findMethod(
                classLoader,
                profile.configClassName,
                "a",
                int.class
        ))
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object result = chain.proceed();
                    if (result instanceof Integer && (Integer) result < MAX_CLIP_COUNT) {
                        return MAX_CLIP_COUNT;
                    }
                    return result;
                }));

        safe(profile.configClassName + ".d", () -> hook(findMethod(
                classLoader,
                profile.configClassName,
                "d",
                void.class
        ))
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object result = chain.proceed();
                    enforceClipCountField(chain.getThisObject(), profile);
                    return result;
                }));

        safe(profile.configClassName + ".e", () -> hook(findMethod(
                classLoader,
                profile.configClassName,
                "e",
                void.class,
                int.class
        ))
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object count = chain.getArg(0);
                    if (count instanceof Integer && (Integer) count < MAX_CLIP_COUNT) {
                        return chain.proceed(new Object[] { MAX_CLIP_COUNT });
                    }
                    return chain.proceed();
                }));
    }

    private void hookClipboardPanel(final ClassLoader classLoader, final HookProfile profile) {
        safe(profile.panelClassName + ".O", () -> hook(findMethod(
                classLoader,
                profile.panelClassName,
                "O",
                void.class,
                List.class
        ))
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object result = chain.proceed();
                    updateCounterText(chain.getThisObject());
                    return result;
                }));
    }

    private void hookPasteTruncation(final ClassLoader classLoader, final HookProfile profile) {
        safe(profile.pasteClassName + ".r", () -> hook(findMethod(
                classLoader,
                profile.pasteClassName,
                "r",
                String.class,
                String.class
        ))
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    String input = (String) chain.getArg(0);
                    if (input != null && input.length() > ORIGINAL_PASTE_TRUNCATE_LENGTH) {
                        return input;
                    }
                    return chain.proceed();
                }));
    }

    private void hookRecordLengthFilter(final ClassLoader classLoader, final HookProfile profile) {
        safe(profile.recordFilterClassName + ".q", () -> hook(findMethod(
                classLoader,
                profile.recordFilterClassName,
                "q",
                void.class,
                List.class
        ))
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> null));
    }

    private void enforceClipCountField(Object config, HookProfile profile) {
        if (config == null) {
            return;
        }

        try {
            int current = getIntField(config, "a");
            if (current < MAX_CLIP_COUNT) {
                setIntField(config, "a", MAX_CLIP_COUNT);
                logMessage(profile.configClassName + ".a field lifted from " + current + " to " + MAX_CLIP_COUNT);
            }
        } catch (Throwable t) {
            logMessage("failed to update " + profile.configClassName + ".a field: " + t.getMessage());
        }
    }

    private void updateCounterText(Object panel) {
        try {
            TextView counter = (TextView) getObjectField(panel, "n");
            if (counter == null || counter.getText() == null) {
                return;
            }

            String text = counter.getText().toString();
            String updated = text
                    .replace("/" + ORIGINAL_CLIP_COUNT + "\uff09", "/" + MAX_CLIP_COUNT + "\uff09")
                    .replace("\uff080/" + ORIGINAL_CLIP_COUNT + "\uff09", "\uff080/" + MAX_CLIP_COUNT + "\uff09");

            if (!text.equals(updated)) {
                counter.setText(updated);
                logMessage("counter text updated: " + text + " -> " + updated);
            }
        } catch (Throwable t) {
            logMessage("failed to update counter text: " + t.getMessage());
        }
    }

    private static Class<?> findClass(ClassLoader classLoader, String className) throws ClassNotFoundException {
        return Class.forName(className, false, classLoader);
    }

    private static Method findMethod(
            ClassLoader classLoader,
            String className,
            String methodName,
            Class<?> returnType,
            Class<?>... parameterTypes
    ) throws ClassNotFoundException, NoSuchMethodException {
        Class<?> targetClass = findClass(classLoader, className);
        return findMethod(targetClass, methodName, returnType, parameterTypes);
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

    private static Object getObjectField(Object instance, String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        return findField(instance.getClass(), fieldName).get(instance);
    }

    private static int getIntField(Object instance, String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        return findField(instance.getClass(), fieldName).getInt(instance);
    }

    private static void setIntField(Object instance, String fieldName, int value)
            throws NoSuchFieldException, IllegalAccessException {
        findField(instance.getClass(), fieldName).setInt(instance, value);
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

        private boolean matches(ClassLoader classLoader) {
            try {
                Class<?> configClass = findClass(classLoader, configClassName);
                Class<?> panelClass = findClass(classLoader, panelClassName);
                Class<?> pasteClass = findClass(classLoader, pasteClassName);
                Class<?> recordFilterClass = findClass(classLoader, recordFilterClassName);

                findField(configClass, "a", int.class);
                findMethod(configClass, "a", int.class);
                findMethod(configClass, "d", void.class);
                findMethod(configClass, "e", void.class, int.class);

                findField(panelClass, "n", TextView.class);
                findMethod(panelClass, "O", void.class, List.class);

                findMethod(pasteClass, "r", String.class, String.class);
                findMethod(recordFilterClass, "q", void.class, List.class);
                return true;
            } catch (Throwable ignored) {
                return false;
            }
        }
    }
}
