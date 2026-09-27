package org.interester.report;

import org.interester.analyze.Series;
import org.interester.analyze.TopicAnalyzer;
import org.interester.model.TopicResult;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationTest {

    private final Labels labels = new Labels(false, true);

    @Test
    void verdictFollowsTheShareOfViews() {
        assertTrue(verdict(1_000, 1_500).startsWith("Research first: Polish (pl). Its share of Wikipedia views grows"));
        assertTrue(verdict(1_000, 950).contains("keeps its share"));
        assertTrue(verdict(1_000, 500).startsWith("No growing interest found. Best result: Polish (pl)"));
    }

    private String verdict(long before, long after) {
        long[] views = new long[24];
        Arrays.fill(views, 0, 12, before);
        Arrays.fill(views, 12, 24, after);
        long[] wiki = new long[24];
        Arrays.fill(wiki, 1_000_000);
        TopicResult r = new TopicAnalyzer().analyze("X", "pl", "X", new Series(YearMonth.of(2024, 9), views, wiki));
        return Recommendation.verdict(r, labels);
    }
}
