package com.fuckbaiduinput;

import org.junit.Test;

import static org.junit.Assert.*;

public final class BoundedRetryCacheTest {
    @Test
    public void distinctSizesStayBoundedAndRecentlyUsedEntrySurvivesEviction() {
        BoundedRetryCache<String, Object> cache = new BoundedRetryCache<>(2, 3, 30L);
        Object small = new Object();
        Object large = new Object();
        cache.put("normal-72", small);
        cache.put("dark-96", large);
        assertSame(small, cache.get("normal-72"));
        cache.put("normal-120", new Object());
        assertEquals(2, cache.size());
        assertNull(cache.get("dark-96"));
        assertSame(small, cache.get("normal-72"));
    }

    @Test
    public void failuresBackOffAndStopAfterConfiguredAttempts() {
        BoundedRetryCache<String, Object> cache = new BoundedRetryCache<>(4, 3, 30L);
        assertTrue(cache.canLoad("key", 0L));
        cache.recordFailure("key", 0L);
        assertFalse(cache.canLoad("key", 29L));
        assertTrue(cache.canLoad("key", 30L));
        cache.recordFailure("key", 30L);
        assertTrue(cache.canLoad("key", 60L));
        cache.recordFailure("key", 60L);
        assertFalse(cache.canLoad("key", Long.MAX_VALUE));
        assertTrue(cache.canLoad("changed-configuration", 60L));
    }

    @Test
    public void successReplacesFailureAndExplicitInvalidationAllowsReload() {
        BoundedRetryCache<String, Object> cache = new BoundedRetryCache<>(4, 3, 30L);
        cache.recordFailure("key", 0L);
        Object value = new Object();
        cache.put("key", value);
        assertSame(value, cache.get("key"));
        assertFalse(cache.canLoad("key", 100L));
        cache.invalidate("key");
        assertNull(cache.get("key"));
        assertTrue(cache.canLoad("key", 100L));
    }

    @Test
    public void failureMetadataAlsoRespectsCapacity() {
        BoundedRetryCache<String, Object> cache = new BoundedRetryCache<>(2, 3, 30L);
        for (int index = 0; index < 100; index++) {
            cache.recordFailure("key-" + index, index);
            assertTrue(cache.size() <= 2);
        }
    }
}
