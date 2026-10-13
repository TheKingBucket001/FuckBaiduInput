package com.fuckbaiduinput;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

public final class FilteredListCallTest {
    @Test
    public void failedFilteringCallsOriginalOnceWithUntouchedArgument() throws Throwable {
        List<String> input = Arrays.asList("remove", "bad", "keep");
        IllegalStateException failure = new IllegalStateException("filter");
        AtomicReference<Throwable> reported = new AtomicReference<>();
        AtomicInteger calls = new AtomicInteger();
        FilteredListCall filteredCall = new FilteredListCall(item -> {
            if ("bad".equals(item)) {
                throw failure;
            }
            return "remove".equals(item);
        }, reported::set);

        Object result = filteredCall.invoke(input, argument -> {
            calls.incrementAndGet();
            assertSame(input, argument);
            return "original result";
        });

        assertEquals("original result", result);
        assertEquals(1, calls.get());
        assertSame(failure, reported.get());
        assertEquals(Arrays.asList("remove", "bad", "keep"), input);
    }

    @Test
    public void hostFailurePropagatesUnchangedWithoutRetryOrFilterError() {
        List<String> input = Arrays.asList("remove", "keep");
        IllegalArgumentException failure = new IllegalArgumentException("host");
        AtomicInteger calls = new AtomicInteger();
        AtomicInteger reports = new AtomicInteger();
        FilteredListCall filteredCall = new FilteredListCall("remove"::equals,
                ignored -> reports.incrementAndGet());
        try {
            filteredCall.invoke(input, argument -> {
                calls.incrementAndGet();
                assertEquals(Arrays.asList("keep"), argument);
                throw failure;
            });
            fail("Expected original failure");
        } catch (Throwable actual) {
            assertSame(failure, actual);
        }
        assertEquals(1, calls.get());
        assertEquals(0, reports.get());
        assertEquals(Arrays.asList("remove", "keep"), input);
    }

    @Test
    public void unchangedListIdentityAndOriginalResultArePreserved() throws Throwable {
        List<String> input = Arrays.asList("first", "second");
        Object expectedResult = new Object();
        AtomicInteger calls = new AtomicInteger();
        FilteredListCall filteredCall = new FilteredListCall(item -> false,
                ignored -> fail("Unexpected filter failure"));

        Object result = filteredCall.invoke(input, argument -> {
            calls.incrementAndGet();
            assertSame(input, argument);
            return expectedResult;
        });

        assertSame(expectedResult, result);
        assertEquals(1, calls.get());
    }

    @Test
    public void reporterFailureCannotPreventTheSingleOriginalCall() throws Throwable {
        List<String> input = Arrays.asList("item");
        AtomicInteger calls = new AtomicInteger();
        FilteredListCall filteredCall = new FilteredListCall(item -> {
            throw new IllegalStateException("preparation");
        }, ignored -> { throw new AssertionError("logger"); });

        filteredCall.invoke(input, argument -> {
            calls.incrementAndGet();
            assertSame(input, argument);
            return null;
        });

        assertEquals(1, calls.get());
    }
}
