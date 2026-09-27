package org.interester.data;

import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

public record MonthlyViews(NavigableMap<YearMonth, Long> views) {

    public MonthlyViews {
        views = Collections.unmodifiableNavigableMap(new TreeMap<>(views));
    }

    public static MonthlyViews empty() {
        return new MonthlyViews(new TreeMap<>());
    }

    public Optional<YearMonth> lastMonth() {
        return views.isEmpty() ? Optional.empty() : Optional.of(views.lastKey());
    }

    public long[] zeroFilledValues(YearMonth from, YearMonth toInclusive) {
        int size = (int) (from.until(toInclusive, ChronoUnit.MONTHS) + 1);
        long[] result = new long[Math.max(size, 0)];
        for (int i = 0; i < result.length; i++) {
            result[i] = views.getOrDefault(from.plusMonths(i), 0L);
        }
        return result;
    }
}
