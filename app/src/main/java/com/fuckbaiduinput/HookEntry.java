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

    @Override
    public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        if (!TARGET_PACKAGE.equals(param.getPackageName())) {
            return;
        }

        logMessage("loading in " + param.getPackageName());
        ClassLoader classLoader = param.getClassLoader();
        hookClipboardConfig(classLoader);
        hookClipboardPanel(classLoader);
        hookPasteTruncation(classLoader);
        hookRecordLengthFilter(classLoader);
    }

    private void hookClipboardConfig(final ClassLoader classLoader) {
        safe("vq1.a", () -> hook(findMethod(classLoader, "com.baidu.vq1", "a"))
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object result = chain.proceed();
                    if (result instanceof Integer && (Integer) result < MAX_CLIP_COUNT) {
                        return MAX_CLIP_COUNT;
                    }
                    return result;
                }));

        safe("vq1.d", () -> hook(findMethod(classLoader, "com.baidu.vq1", "d"))
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object result = chain.proceed();
                    enforceClipCountField(chain.getThisObject());
                    return result;
                }));

        safe("vq1.e", () -> hook(findMethod(classLoader, "com.baidu.vq1", "e", int.class))
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object count = chain.getArg(0);
                    if (count instanceof Integer && (Integer) count < MAX_CLIP_COUNT) {
                        return chain.proceed(new Object[] { MAX_CLIP_COUNT });
                    }
                    return chain.proceed();
                }));
    }

    private void hookClipboardPanel(final ClassLoader classLoader) {
        safe("er1.O", () -> hook(findMethod(classLoader, "com.baidu.er1", "O", List.class))
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object result = chain.proceed();
                    updateCounterText(chain.getThisObject());
                    return result;
                }));
    }

    private void hookPasteTruncation(final ClassLoader classLoader) {
        safe("iq1.r", () -> hook(findMethod(classLoader, "com.baidu.iq1", "r", String.class))
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    String input = (String) chain.getArg(0);
                    if (input != null && input.length() > ORIGINAL_PASTE_TRUNCATE_LENGTH) {
                        return input;
                    }
                    return chain.proceed();
                }));
    }

    private void hookRecordLengthFilter(final ClassLoader classLoader) {
        safe("er1$i.q", () -> hook(findMethod(classLoader, "com.baidu.er1$i", "q", List.class))
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> null));
    }

    private void enforceClipCountField(Object config) {
        if (config == null) {
            return;
        }

        try {
            int current = getIntField(config, "a");
            if (current < MAX_CLIP_COUNT) {
                setIntField(config, "a", MAX_CLIP_COUNT);
                logMessage("vq1.a field lifted from " + current + " to " + MAX_CLIP_COUNT);
            }
        } catch (Throwable t) {
            logMessage("failed to update vq1.a field: " + t.getMessage());
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

    private static Method findMethod(
            ClassLoader classLoader,
            String className,
            String methodName,
            Class<?>... parameterTypes
    ) throws ClassNotFoundException, NoSuchMethodException {
        Class<?> targetClass = Class.forName(className, false, classLoader);
        Method method = targetClass.getDeclaredMethod(methodName, parameterTypes);
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
}
