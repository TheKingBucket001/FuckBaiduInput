package com.fuckbaiduinput;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/** Bounded process-local diagnostics; logging cannot change a hook's outcome. */
final class ModuleDiagnostics {
    static final int INFO = 4;
    static final int WARN = 5;
    static final int ERROR = 6;
    static final int MAX_ISSUES = 64;
    static final long REPEAT_INTERVAL_MS = 30_000L;

    interface Sink {
        void write(int priority, String message) throws Throwable;
    }

    interface Installer {
        void install() throws Throwable;
    }

    private final Sink sink;
    private final LongSupplier clock;
    private final Map<String, Issue> issues = new LinkedHashMap<>();
    private final Issue overflow = new Issue();
    private boolean initializing;
    private int completedGroups;
    private int incompleteGroups;
    private long initializationIssues;
    private long sinkFailures;
    private long startedAt;

    ModuleDiagnostics(Sink sink) {
        this(sink, () -> System.nanoTime() / 1_000_000L);
    }

    ModuleDiagnostics(Sink sink, LongSupplier clock) {
        this.sink = sink;
        this.clock = clock;
    }

    synchronized void beginInitialization() {
        initializing = true;
        completedGroups = 0;
        incompleteGroups = 0;
        initializationIssues = 0L;
        startedAt = now();
    }

    void installGroup(String name, Installer installer) {
        try {
            installer.install();
        } catch (Throwable failure) {
            synchronized (this) {
                incompleteGroups++;
            }
            error("install", name, failure);
            return;
        }
        synchronized (this) {
            completedGroups++;
        }
    }

    synchronized void finishInitialization() {
        finishInitialization("completed");
    }

    synchronized void finishInitialization(String outcome) {
        if (!initializing) {
            return;
        }
        initializing = false;
        write(incompleteGroups == 0 && initializationIssues == 0L
                        && "completed".equals(outcome) ? INFO : WARN,
                "phase=initialization outcome=" + safeToken(outcome)
                        + " completed_groups=" + completedGroups
                        + " incomplete_groups=" + incompleteGroups
                        + " issue_events=" + initializationIssues
                        + " registration_count=untracked"
                        + " partial_registrations_may_remain=" + (incompleteGroups != 0)
                        + " elapsed_ms=" + Math.max(0L, now() - startedAt)
                        + " logger_failures=" + sinkFailures);
    }

    void warning(String phase, String operation) {
        report(WARN, phase, operation, null);
    }

    void error(String phase, String operation, Throwable failure) {
        report(ERROR, phase, operation, failure);
    }

    /** Legacy callers pass exception text; retain its class, never its message or payload. */
    void legacy(String message) {
        try {
            String phase;
            synchronized (this) {
                phase = initializing ? "initialization" : "runtime";
            }
            if (message == null) {
                return;
            }
            int colon = message.indexOf(':');
            String operation = colon < 0 ? message : message.substring(0, colon);
            String type = "";
            if (colon >= 0) {
                String suffix = message.substring(colon + 1).trim();
                int end = suffix.indexOf(':');
                String candidate = end < 0 ? suffix : suffix.substring(0, end);
                if (candidate.matches("[A-Za-z_$][A-Za-z0-9_.$]*"
                        + "(?:Exception|Error)")) {
                    type = " type=" + candidate;
                }
            }
            boolean error = operation.contains("failed") || operation.contains("rejected");
            report(error ? ERROR : WARN, phase, operation + type, null);
        } catch (Throwable ignored) {
            // Diagnostics, including malformed legacy input, are never part of host behavior.
        }
    }

    private void report(int priority, String phase, String operation, Throwable failure) {
        try {
            String detail = failureDetail(failure);
            String key = safeToken(phase) + " event=" + safeToken(operation) + detail;
            synchronized (this) {
                if (initializing) {
                    initializationIssues = increment(initializationIssues);
                }
                Issue issue = issues.get(key);
                if (issue == null) {
                    if (issues.size() >= MAX_ISSUES) {
                        emitIssue(priority, "diagnostics event=issue_capacity_exceeded", overflow);
                        return;
                    }
                    issue = new Issue();
                    issues.put(key, issue);
                }
                emitIssue(priority, key, issue);
            }
        } catch (Throwable ignored) {
            // A failed logger or exceptional Throwable implementation must not escape.
        }
    }

    private void emitIssue(int priority, String key, Issue issue) {
        issue.count = increment(issue.count);
        long timestamp = now();
        boolean escalated = priority > issue.priority;
        issue.priority = Math.max(issue.priority, priority);
        if (issue.count != 1L && !escalated
                && timestamp - issue.lastEmission < REPEAT_INTERVAL_MS) {
            return;
        }
        issue.lastEmission = timestamp;
        write(issue.priority, "phase=" + key + " count=" + issue.count);
    }

    private void write(int priority, String message) {
        try {
            sink.write(priority, message);
        } catch (Throwable ignored) {
            sinkFailures = increment(sinkFailures);
        }
    }

    private long now() {
        try {
            return clock.getAsLong();
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    private static long increment(long value) {
        return value == Long.MAX_VALUE ? value : value + 1L;
    }

    private static String safeToken(String value) {
        if (value == null) {
            return "unknown";
        }
        StringBuilder token = new StringBuilder(Math.min(value.length(), 160));
        for (int index = 0; index < value.length() && index < 160; index++) {
            char item = value.charAt(index);
            token.append(Character.isLetterOrDigit(item)
                    || item == '.' || item == '_' || item == '$' || item == '='
                    ? item : '_');
        }
        return token.toString();
    }

    private static String failureDetail(Throwable failure) {
        if (failure == null) {
            return "";
        }
        StringBuilder detail = new StringBuilder();
        Throwable current = failure;
        for (int depth = 0; current != null && depth < 6; depth++) {
            detail.append(depth == 0 ? " type=" : " cause=")
                    .append(current.getClass().getName());
            StackTraceElement[] trace = current.getStackTrace();
            if (trace.length > 0) {
                StackTraceElement location = trace[0];
                detail.append(" at=").append(safeToken(location.getClassName()))
                        .append('.').append(safeToken(location.getMethodName()))
                        .append(':').append(location.getLineNumber());
            }
            for (StackTraceElement location : trace) {
                String method = location.getMethodName();
                if (location.getClassName().startsWith("com.fuckbaiduinput.")
                        && !"findClass".equals(method) && !"findMethod".equals(method)
                        && !"findField".equals(method) && !"findConstructor".equals(method)) {
                    detail.append(" module_at=").append(safeToken(location.getClassName()))
                            .append('.').append(safeToken(method))
                            .append(':').append(location.getLineNumber());
                    break;
                }
            }
            Throwable next = current.getCause();
            if (next == current) {
                break;
            }
            current = next;
        }
        return detail.toString();
    }

    synchronized int issueCount() {
        return issues.size();
    }

    private static final class Issue {
        int priority;
        long count;
        long lastEmission;
    }
}
