package com.fuckbaiduinput;

import java.util.ArrayList;
import java.util.List;

/** Creates a filtered copy only after the first matching item; never mutates the input. */
final class ListFilters {
    interface Matcher {
        boolean matches(Object item) throws Throwable;
    }

    private ListFilters() {
    }

    static ArrayList<Object> removeMatching(List<?> source, Matcher matcher) throws Throwable {
        ArrayList<Object> filtered = null;
        int index = 0;
        for (Object item : source) {
            if (matcher.matches(item)) {
                if (filtered == null) {
                    filtered = new ArrayList<>(source.size() - 1);
                    filtered.addAll(source.subList(0, index));
                }
            } else if (filtered != null) {
                filtered.add(item);
            }
            index++;
        }
        return filtered;
    }
}
