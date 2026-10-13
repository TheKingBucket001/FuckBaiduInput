package com.fuckbaiduinput;

import java.util.LinkedHashMap;

/** Bounded successful values and bounded failure retries, with no disposal of borrowed values. */
final class BoundedRetryCache<K, V> {
    private static final class Entry<V> {
        final V value;
        final int failures;
        final long retryAt;

        Entry(V value, int failures, long retryAt) {
            this.value = value;
            this.failures = failures;
            this.retryAt = retryAt;
        }
    }

    private final int capacity;
    private final int maxFailures;
    private final long retryDelayMillis;
    private final LinkedHashMap<K, Entry<V>> entries = new LinkedHashMap<>(4, 0.75f, true);

    BoundedRetryCache(int capacity, int maxFailures, long retryDelayMillis) {
        if (capacity < 1 || maxFailures < 1 || retryDelayMillis < 1) {
            throw new IllegalArgumentException("invalid cache limits");
        }
        this.capacity = capacity;
        this.maxFailures = maxFailures;
        this.retryDelayMillis = retryDelayMillis;
    }

    synchronized V get(K key) {
        Entry<V> entry = entries.get(key);
        return entry == null ? null : entry.value;
    }

    synchronized boolean canLoad(K key, long now) {
        Entry<V> entry = entries.get(key);
        return entry == null || (entry.value == null
                && entry.failures < maxFailures && now >= entry.retryAt);
    }

    synchronized void put(K key, V value) {
        if (value == null) {
            throw new IllegalArgumentException("null cache value");
        }
        entries.put(key, new Entry<>(value, 0, 0L));
        trim();
    }

    synchronized void recordFailure(K key, long now) {
        Entry<V> previous = entries.get(key);
        int failures = previous == null ? 1 : Math.min(maxFailures, previous.failures + 1);
        entries.put(key, new Entry<>(null, failures, now + retryDelayMillis));
        trim();
    }

    synchronized void invalidate(K key) {
        entries.remove(key);
    }

    synchronized int size() {
        return entries.size();
    }

    private void trim() {
        while (entries.size() > capacity) {
            K oldest = entries.entrySet().iterator().next().getKey();
            entries.remove(oldest);
        }
    }
}
