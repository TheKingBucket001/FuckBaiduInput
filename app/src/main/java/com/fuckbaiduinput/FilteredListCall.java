package com.fuckbaiduinput;

import java.util.List;

/** Prepares a list argument defensively, then invokes the original exactly once. */
final class FilteredListCall {
    interface Original {
        Object invoke(List<?> argument) throws Throwable;
    }

    interface FailureReporter {
        void report(Throwable failure) throws Throwable;
    }

    private final ListFilters.Matcher matcher;
    private final FailureReporter reporter;

    FilteredListCall(ListFilters.Matcher matcher, FailureReporter reporter) {
        this.matcher = matcher;
        this.reporter = reporter;
    }

    Object invoke(List<?> original, Original call) throws Throwable {
        List<?> argument = original;
        try {
            List<?> filtered = ListFilters.removeMatching(original, matcher);
            if (filtered != null) {
                argument = filtered;
            }
        } catch (Throwable failure) {
            try {
                reporter.report(failure);
            } catch (Throwable ignored) {
                // Reporting a preparation failure must not prevent the original call.
            }
        }
        // Original failures propagate unchanged; they are never preparation failures.
        return call.invoke(argument);
    }
}
