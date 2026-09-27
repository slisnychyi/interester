package org.interester.analyze;

import java.time.YearMonth;

public record Series(YearMonth from, long[] views, long[] wikiViews) {

    public Series {
        if (views.length != wikiViews.length) {
            throw new IllegalArgumentException("views and wikiViews must cover the same months");
        }
    }

    public int months() {
        return views.length;
    }

    public YearMonth month(int index) {
        return from.plusMonths(index);
    }
}
