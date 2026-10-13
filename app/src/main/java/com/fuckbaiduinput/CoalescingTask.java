package com.fuckbaiduinput;

import java.util.concurrent.atomic.AtomicBoolean;

/** Keeps at most one queued update; the task reads the latest state when it runs. */
final class CoalescingTask {
    interface Poster {
        boolean post(Runnable task);
    }

    private final Poster poster;
    private final Runnable task;
    private final AtomicBoolean pending = new AtomicBoolean();

    CoalescingTask(Poster poster, Runnable task) {
        this.poster = poster;
        this.task = task;
    }

    boolean request() {
        if (!pending.compareAndSet(false, true)) {
            return true;
        }
        try {
            if (poster.post(() -> {
                // A change arriving during this run may queue one follow-up update.
                pending.set(false);
                task.run();
            })) {
                return true;
            }
        } catch (RuntimeException | Error error) {
            pending.set(false);
            throw error;
        }
        pending.set(false);
        return false;
    }
}
