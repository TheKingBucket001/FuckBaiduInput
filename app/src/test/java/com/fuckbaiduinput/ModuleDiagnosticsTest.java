package com.fuckbaiduinput;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ModuleDiagnosticsTest {
    @Test
    public void configurationIssuesDuringInitializationCannotProduceHealthySummary() {
        List<Event> events = new ArrayList<>();
        ModuleDiagnostics diagnostics = diagnostics(events, new AtomicLong());
        diagnostics.beginInitialization();
        diagnostics.warning("configuration", "invalid_schema");
        diagnostics.error("configuration", "read_snapshot", new IllegalStateException("private"));
        diagnostics.installGroup("available", () -> {});
        diagnostics.finishInitialization();

        assertEquals(3, events.size());
        Event summary = events.get(2);
        assertEquals(ModuleDiagnostics.WARN, summary.priority);
        assertTrue(summary.message.contains("completed_groups=1 incomplete_groups=0 issue_events=2"));
        assertFalse(events.get(1).message.contains("private"));
    }

    @Test
    public void successfulGroupsProduceOnlyOneInitializationSummary() {
        List<Event> events = new ArrayList<>();
        ModuleDiagnostics diagnostics = diagnostics(events, new AtomicLong());
        AtomicInteger installed = new AtomicInteger();
        diagnostics.beginInitialization();
        diagnostics.installGroup("first", installed::incrementAndGet);
        diagnostics.installGroup("second", installed::incrementAndGet);
        diagnostics.finishInitialization();
        diagnostics.finishInitialization();

        assertEquals(2, installed.get());
        assertEquals(1, events.size());
        assertEquals(ModuleDiagnostics.INFO, events.get(0).priority);
        assertTrue(events.get(0).message.contains("completed_groups=2 incomplete_groups=0"));
        assertTrue(events.get(0).message.contains("registration_count=untracked"));
    }

    @Test
    public void incompleteGroupKeepsEarlierRegistrationsAndDoesNotStopLaterGroups() {
        List<Event> events = new ArrayList<>();
        ModuleDiagnostics diagnostics = diagnostics(events, new AtomicLong());
        AtomicInteger installed = new AtomicInteger();
        diagnostics.beginInitialization();
        diagnostics.installGroup("multiple targets", () -> {
            installed.incrementAndGet();
            throw new InvocationTargetException(new IllegalStateException("private input text"));
        });
        diagnostics.installGroup("later target", installed::incrementAndGet);
        diagnostics.finishInitialization();

        assertEquals(2, installed.get());
        assertEquals(ModuleDiagnostics.ERROR, events.get(0).priority);
        assertTrue(events.get(0).message.contains("type=java.lang.reflect.InvocationTargetException"));
        assertTrue(events.get(0).message.contains("cause=java.lang.IllegalStateException"));
        assertFalse(events.get(0).message.contains("private input text"));
        assertTrue(events.get(1).message.contains("completed_groups=1 incomplete_groups=1"));
        assertTrue(events.get(1).message.contains("partial_registrations_may_remain=true"));
    }

    @Test
    public void failingLoggerCannotTurnSuccessfulInstallationIntoFailureOrEscape() {
        AtomicInteger logAttempts = new AtomicInteger();
        AtomicInteger installed = new AtomicInteger();
        ModuleDiagnostics diagnostics = new ModuleDiagnostics((priority, message) -> {
            logAttempts.incrementAndGet();
            throw new AssertionError("unavailable logger");
        }, () -> 0L);
        diagnostics.beginInitialization();
        diagnostics.installGroup("failure", () -> { throw new IllegalArgumentException(); });
        diagnostics.installGroup("success", installed::incrementAndGet);
        diagnostics.finishInitialization();
        diagnostics.beginInitialization();
        diagnostics.installGroup("next initialization", installed::incrementAndGet);
        diagnostics.finishInitialization();

        assertEquals(2, installed.get());
        assertEquals(3, logAttempts.get());
    }

    @Test
    public void repeatedFailureHasImmediateFirstReportAndExactBoundedSummaryCount() {
        List<Event> events = new ArrayList<>();
        AtomicLong clock = new AtomicLong();
        ModuleDiagnostics diagnostics = diagnostics(events, clock);
        Throwable failure = new IllegalStateException("must never be printed");
        for (int index = 0; index < 100; index++) {
            diagnostics.error("runtime", "filter", failure);
        }
        assertEquals(1, events.size());
        assertTrue(events.get(0).message.endsWith("count=1"));
        clock.set(ModuleDiagnostics.REPEAT_INTERVAL_MS);
        diagnostics.error("runtime", "filter", failure);

        assertEquals(2, events.size());
        assertTrue(events.get(1).message.endsWith("count=101"));
        assertFalse(events.get(1).message.contains("must never be printed"));
    }

    @Test
    public void distinctFailuresCannotGrowStateWithoutBound() {
        List<Event> events = new ArrayList<>();
        AtomicLong clock = new AtomicLong();
        ModuleDiagnostics diagnostics = diagnostics(events, clock);
        for (int index = 0; index < 100; index++) {
            diagnostics.warning("runtime", "failure_" + index);
        }
        assertEquals(ModuleDiagnostics.MAX_ISSUES, diagnostics.issueCount());
        assertEquals(ModuleDiagnostics.MAX_ISSUES + 1, events.size());
        clock.set(ModuleDiagnostics.REPEAT_INTERVAL_MS);
        diagnostics.warning("runtime", "another_new_failure");

        assertEquals(ModuleDiagnostics.MAX_ISSUES, diagnostics.issueCount());
        assertTrue(events.get(events.size() - 1).message.contains("issue_capacity_exceeded count=37"));
    }

    @Test
    public void concurrentFailureReportsRetainTheCompleteCount() throws InterruptedException {
        List<Event> events = new ArrayList<>();
        AtomicLong clock = new AtomicLong();
        ModuleDiagnostics diagnostics = diagnostics(events, clock);
        Throwable failure = new IllegalStateException();
        CountDownLatch ready = new CountDownLatch(4);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(4);
        for (int worker = 0; worker < 4; worker++) {
            new Thread(() -> {
                ready.countDown();
                try {
                    start.await();
                    for (int index = 0; index < 100; index++) {
                        diagnostics.error("runtime", "concurrent", failure);
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                } finally {
                    finished.countDown();
                }
            }).start();
        }
        ready.await();
        start.countDown();
        finished.await();
        assertEquals(1, events.size());
        clock.set(ModuleDiagnostics.REPEAT_INTERVAL_MS);
        diagnostics.error("runtime", "concurrent", failure);

        assertEquals(2, events.size());
        assertTrue(events.get(1).message.endsWith("count=401"));
    }

    @Test
    public void legacyExceptionMessagesAreRedactedAndLevelsRemainActionable() {
        List<Event> events = new ArrayList<>();
        ModuleDiagnostics diagnostics = diagnostics(events, new AtomicLong());
        diagnostics.legacy("failed to filter: java.lang.IllegalStateException: private clipboard data");
        diagnostics.legacy("custom keyboard logo resource missing");

        assertEquals(2, events.size());
        assertEquals(ModuleDiagnostics.ERROR, events.get(0).priority);
        assertFalse(events.get(0).message.contains("private clipboard data"));
        assertTrue(events.get(0).message.contains("java.lang.IllegalStateException"));
        assertEquals(ModuleDiagnostics.WARN, events.get(1).priority);
    }

    @Test
    public void blockedInitializationIsNeverReportedAsHealthySuccess() {
        List<Event> events = new ArrayList<>();
        ModuleDiagnostics diagnostics = diagnostics(events, new AtomicLong());
        diagnostics.beginInitialization();
        diagnostics.finishInitialization("blocked");

        assertEquals(1, events.size());
        assertEquals(ModuleDiagnostics.WARN, events.get(0).priority);
        assertTrue(events.get(0).message.contains("outcome=blocked"));
    }

    @Test
    public void initializationWarningRemainsVisibleEvenWhenAllInstallersComplete() {
        List<Event> events = new ArrayList<>();
        ModuleDiagnostics diagnostics = diagnostics(events, new AtomicLong());
        diagnostics.beginInitialization();
        diagnostics.warning("initialization", "remote_preferences_unavailable");
        diagnostics.installGroup("target", () -> { });
        diagnostics.finishInitialization();

        assertEquals(2, events.size());
        assertEquals(ModuleDiagnostics.WARN, events.get(1).priority);
        assertTrue(events.get(1).message.contains("completed_groups=1 incomplete_groups=0"));
        assertTrue(events.get(1).message.contains("issue_events=1"));
    }

    @Test
    public void newErrorAtCapacityEscalatesImmediatelyInsteadOfHidingBehindWarnings() {
        List<Event> events = new ArrayList<>();
        ModuleDiagnostics diagnostics = diagnostics(events, new AtomicLong());
        for (int index = 0; index <= ModuleDiagnostics.MAX_ISSUES; index++) {
            diagnostics.warning("runtime", "warning_" + index);
        }
        diagnostics.error("runtime", "new_critical_error", new IllegalStateException());

        assertEquals(ModuleDiagnostics.MAX_ISSUES, diagnostics.issueCount());
        assertEquals(ModuleDiagnostics.MAX_ISSUES + 2, events.size());
        Event last = events.get(events.size() - 1);
        assertEquals(ModuleDiagnostics.ERROR, last.priority);
        assertTrue(last.message.contains("issue_capacity_exceeded count=2"));
    }

    @Test
    public void reflectionFailurePointsToTheCallSiteBeyondTheSharedLookupHelper() {
        List<Event> events = new ArrayList<>();
        ModuleDiagnostics diagnostics = diagnostics(events, new AtomicLong());
        Throwable failure = new NoSuchMethodException("must not expose an arbitrary message");
        failure.setStackTrace(new StackTraceElement[] {
                new StackTraceElement("java.lang.Class", "getDeclaredMethod", "Class.java", 200),
                new StackTraceElement("com.fuckbaiduinput.HookEntry", "findMethod", "HookEntry.java", 3500),
                new StackTraceElement("com.fuckbaiduinput.HookEntry", "lambda$hookShopTabs$7", "HookEntry.java", 2120)
        });
        diagnostics.error("install", "shop tabs", failure);

        String message = events.get(0).message;
        assertTrue(message.contains("module_at=com.fuckbaiduinput.HookEntry.lambda$hookShopTabs$7:2120"));
        assertFalse(message.contains("must not expose"));
    }

    private static ModuleDiagnostics diagnostics(List<Event> events, AtomicLong clock) {
        return new ModuleDiagnostics((priority, message) -> events.add(new Event(priority, message)),
                clock::get);
    }

    private static final class Event {
        final int priority;
        final String message;

        Event(int priority, String message) {
            this.priority = priority;
            this.message = message;
        }
    }
}
