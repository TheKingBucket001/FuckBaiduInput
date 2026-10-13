package com.fuckbaiduinput;

import android.content.res.Configuration;
import android.content.res.Resources;

import java.lang.ref.WeakReference;

/** A resource identity and immutable configuration snapshot, without retaining a Context. */
final class KeyboardLogoKey {
    private final WeakReference<Resources> resources;
    private final int resourceIdentity;
    private final int resourceId;
    private final int densityDpi;
    private final Configuration configuration;

    private KeyboardLogoKey(Resources resources, int resourceId) {
        this.resources = new WeakReference<>(resources);
        this.resourceIdentity = System.identityHashCode(resources);
        this.resourceId = resourceId;
        this.densityDpi = resources.getDisplayMetrics().densityDpi;
        this.configuration = new Configuration(resources.getConfiguration());
    }

    static KeyboardLogoKey create(Resources resources, int resourceId, String expectedName) {
        try {
            if (!"com.baidu.input_oppo".equals(resources.getResourcePackageName(resourceId))
                    || !"drawable".equals(resources.getResourceTypeName(resourceId))
                    || !expectedName.equals(resources.getResourceEntryName(resourceId))) {
                return null;
            }
            return new KeyboardLogoKey(resources, resourceId);
        } catch (RuntimeException error) {
            return null;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof KeyboardLogoKey)) {
            return false;
        }
        KeyboardLogoKey that = (KeyboardLogoKey) other;
        Resources current = resources.get();
        return current != null && current == that.resources.get()
                && resourceId == that.resourceId && densityDpi == that.densityDpi
                && configuration.equals(that.configuration);
    }

    @Override
    public int hashCode() {
        int hash = 31 * resourceIdentity + resourceId;
        hash = 31 * hash + densityDpi;
        return 31 * hash + configuration.hashCode();
    }
}
