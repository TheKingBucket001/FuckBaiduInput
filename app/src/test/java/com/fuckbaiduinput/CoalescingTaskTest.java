package com.fuckbaiduinput;

import org.junit.Test;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public final class CoalescingTaskTest {
    @Test
    public void burstQueuesOnceAndReadsLatestValue() {
        ArrayDeque<Runnable> queue = new ArrayDeque<>();
        AtomicInteger current = new AtomicInteger();
        AtomicInteger applied = new AtomicInteger(-1);
        CoalescingTask task = new CoalescingTask(queue::offer, () -> applied.set(current.get()));
        for (int value = 0; value < 100; value++) {
            current.set(value);
            assertTrue(task.request());
        }
        assertEquals(1, queue.size());
        queue.remove().run();
        assertEquals(99, applied.get());
        assertTrue(queue.isEmpty());
    }

    @Test
    public void updateDuringRunQueuesOneFollowup() {
        ArrayDeque<Runnable> queue = new ArrayDeque<>();
        AtomicInteger runs = new AtomicInteger();
        CoalescingTask[] task = new CoalescingTask[1];
        task[0] = new CoalescingTask(queue::offer, () -> {
            if (runs.incrementAndGet() == 1) {
                task[0].request();
                task[0].request();
            }
        });
        task[0].request();
        queue.remove().run();
        assertEquals(1, queue.size());
        queue.remove().run();
        assertEquals(2, runs.get());
        assertTrue(queue.isEmpty());
    }

    @Test
    public void rejectedPostDoesNotLeavePendingSet() {
        AtomicInteger posts = new AtomicInteger();
        CoalescingTask task = new CoalescingTask(runnable -> {
            posts.incrementAndGet();
            return false;
        }, () -> fail("rejected post executed"));
        assertFalse(task.request());
        assertFalse(task.request());
        assertEquals(2, posts.get());
    }

    @Test
    public void failingTaskCanBeRequestedAgain() {
        ArrayDeque<Runnable> queue = new ArrayDeque<>();
        AtomicInteger runs = new AtomicInteger();
        CoalescingTask task = new CoalescingTask(queue::offer, () -> {
            if (runs.incrementAndGet() == 1) {
                throw new IllegalStateException("first update failed");
            }
        });
        task.request();
        try {
            queue.remove().run();
            fail("expected failure");
        } catch (IllegalStateException expected) {
            assertEquals("first update failed", expected.getMessage());
        }
        assertTrue(task.request());
        queue.remove().run();
        assertEquals(2, runs.get());
    }

    @Test
    public void posterErrorClearsPendingAndAllowsAnotherRequest() {
        ArrayDeque<Runnable> queue = new ArrayDeque<>();
        AtomicInteger posts = new AtomicInteger();
        AtomicInteger runs = new AtomicInteger();
        AssertionError failure = new AssertionError("poster unavailable");
        CoalescingTask task = new CoalescingTask(runnable -> {
            if (posts.incrementAndGet() == 1) {
                throw failure;
            }
            return queue.offer(runnable);
        }, runs::incrementAndGet);
        try {
            task.request();
            fail("Expected poster error");
        } catch (AssertionError actual) {
            assertSame(failure, actual);
        }

        assertTrue(task.request());
        assertEquals(2, posts.get());
        assertEquals(1, queue.size());
        queue.remove().run();
        assertEquals(1, runs.get());
    }
}
