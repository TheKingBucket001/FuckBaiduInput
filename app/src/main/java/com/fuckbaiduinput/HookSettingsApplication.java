package com.fuckbaiduinput;

import android.app.Application;

import java.util.concurrent.atomic.AtomicReference;

import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

/** Keeps the framework service handle available to the settings Provider. */
public final class HookSettingsApplication extends Application
        implements XposedServiceHelper.OnServiceListener {
    private static final AtomicReference<XposedService> SERVICE = new AtomicReference<>();

    @Override
    public void onCreate() {
        super.onCreate();
        XposedServiceHelper.registerListener(this);
    }

    static XposedService getService() {
        return SERVICE.get();
    }

    @Override
    public void onServiceBind(XposedService service) {
        if (service != null) {
            SERVICE.set(service);
        }
    }

    @Override
    public void onServiceDied(XposedService service) {
        if (service != null) {
            SERVICE.compareAndSet(service, null);
        } else {
            SERVICE.set(null);
        }
    }
}
