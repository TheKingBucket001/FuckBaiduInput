package com.fuckbaiduinput;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import org.junit.Test;

public class ListFiltersTest {
    @Test public void unchangedAndEmptyInputsDoNotCreateCopies() throws Throwable {
        assertNull(ListFilters.removeMatching(Arrays.asList("a", "b"), item -> false));
        assertNull(ListFilters.removeMatching(Collections.emptyList(), item -> true));
    }

    @Test public void removalPreservesOrderNullsAndOriginalInput() throws Throwable {
        List<Object> original = Arrays.asList("a", null, "remove", "b", "remove", "c");
        assertEquals(Arrays.asList("a", null, "b", "c"),
                ListFilters.removeMatching(original, item -> "remove".equals(item)));
        assertEquals(Arrays.asList("a", null, "remove", "b", "remove", "c"), original);
    }

    @Test public void failedInspectionLeavesOriginalUntouched() {
        List<String> original = Arrays.asList("a", "b", "c");
        try {
            ListFilters.removeMatching(original, item -> {
                if ("b".equals(item)) throw new IllegalStateException("test");
                return true;
            });
            fail("Expected matcher failure");
        } catch (Throwable expected) {
            assertTrue(expected instanceof IllegalStateException);
            assertEquals(Arrays.asList("a", "b", "c"), original);
        }
    }

    @Test public void sequentialListsNeverUseIndexedReads() throws Throwable {
        LinkedList<Object> sequential = new LinkedList<Object>(Arrays.asList("a", "b", "c")) {
            @Override public Object get(int index) {
                throw new AssertionError("Indexed access on a sequential list");
            }
        };
        assertEquals(Arrays.asList("a", "c"),
                ListFilters.removeMatching(sequential, item -> "b".equals(item)));
    }
}
