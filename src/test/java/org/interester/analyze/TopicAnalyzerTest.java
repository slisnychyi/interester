package org.interester.analyze;

import org.interester.model.Confidence;
import org.interester.model.Direction;
import org.interester.model.TopicResult;
import org.interester.model.TopicResult.Reason;
import org.interester.model.TopicResult.Spike;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopicAnalyzerTest {

    private final TopicAnalyzer analyzer = new TopicAnalyzer();

    private static final Series UK_ASTRONOMY = new Series(YearMonth.of(2024, 8),
            new long[]{754, 4687, 1776, 1746, 1634, 1558, 1296, 1068, 1019, 789, 361, 305,
                    375, 1642, 635, 557, 622, 449, 425, 410, 405, 600, 281, 322},
            new long[]{66081529L, 74509285L, 83014264L, 83835060L, 84431475L, 94111809L, 81951788L, 79609260L,
                    72146878L, 69040303L, 53652676L, 62209478L, 61801817L, 61256706L, 66044662L, 67137685L,
                    55497356L, 60471976L, 51614348L, 55114806L, 53385280L, 55950057L, 48312008L, 52999047L});

    private static final Series EN_ECLIPSE = new Series(YearMonth.of(2024, 9),
            new long[]{10553, 10487, 8279, 13696, 14377, 12063, 38287, 16294, 16406, 17559, 40495, 50255,
                    42528, 24387, 27397, 38071, 90314, 73367, 60699, 45170, 46678, 57171, 142601, 706753},
            new long[]{7167746176L, 7442946001L, 7358270481L, 7475149173L, 8075023328L, 7086641734L,
                    7740123015L, 6971174625L, 7133413827L, 6637486973L, 6968421594L, 7112406774L,
                    6916125844L, 7153203240L, 6924863090L, 6822630492L, 7230285460L, 6352436517L,
                    6794115655L, 6475256736L, 6702741530L, 6463421139L, 6749320082L, 6497616866L});

    private static final Series DE_ENGLISH = new Series(YearMonth.of(2024, 9),
            new long[]{23967, 25193, 26386, 26007, 30967, 23877, 27856, 26330, 26787, 23082, 22744, 22995,
                    24452, 25580, 23704, 23070, 25610, 21820, 23948, 23635, 23683, 24277, 21586, 20354},
            new long[]{746521495L, 804199250L, 764932003L, 812305022L, 902194788L, 768465579L, 806741813L,
                    736284306L, 712160095L, 643354804L, 695348128L, 736793813L, 696111150L, 748632593L,
                    733914764L, 705194251L, 780199458L, 672194020L, 693410501L, 662879466L, 702429083L,
                    682993791L, 693723660L, 687413224L});

    @Test
    void astronomyUk() {
        TopicResult r = analyzer.analyze("Astronomy", "uk", "Астрономія", UK_ASTRONOMY);

        assertEquals(6_723L, r.last12());
        assertEquals(16_993L, r.previous12());
        assertEquals(-60.4, r.growthPct());
        assertEquals(-23.8, r.wikiGrowthPct());
        assertEquals(-48.0, r.growthVsWikiPct());
        assertEquals(629L, r.monthlyMedian());
        assertEquals(Direction.FALLING, r.trend().direction());
        assertEquals(List.of(new Spike("2024-09", 4_687)), r.spikes());
        assertEquals("09", r.seasonalPeak());
        assertEquals(List.of("DIFFERS_FROM_WIKI", "SEASONAL"), codes(r));
        assertEquals(Confidence.HIGH, r.confidence());
        assertTrue(text(r, "DIFFERS_FROM_WIKI").contains("Ukrainian Wikipedia overall (-23.8%)"));
        assertEquals("Views peak every September.", text(r, "SEASONAL"));
    }

    @Test
    void eclipseGrowthIsSpikeDriven() {
        TopicResult r = analyzer.analyze("Solar eclipse of August 12, 2026", "en", "Solar eclipse of August 12, 2026",
                EN_ECLIPSE);

        assertTrue(r.growthPct() > 400);
        assertTrue(codes(r).contains("SPIKE_DRIVEN"));
        assertTrue(codes(r).contains("NOISY_TREND"));
        assertEquals(Confidence.LOW, r.confidence());
        assertTrue(text(r, "SPIKE_DRIVEN").contains("2026-07, 2026-08"));
    }

    @Test
    void englishDeFollowsWiki() {
        TopicResult r = analyzer.analyze("English language", "de", "Englische Sprache", DE_ENGLISH);

        assertEquals(Direction.FLAT, r.trend().direction());
        assertTrue(Math.abs(r.growthVsWikiPct()) < 10);
        assertEquals(List.of("FOLLOWS_WIKI"), codes(r));
        assertEquals(Confidence.MEDIUM, r.confidence());
    }

    @Test
    void steadyGrowthAgainstFlatWiki() {
        long[] views = new long[24];
        for (int i = 0; i < 24; i++) {
            views[i] = 1_000 + 50L * i;
        }
        TopicResult r = analyzer.analyze("X", "pl", "X", new Series(YearMonth.of(2024, 8), views, flat(24, 1_000_000)));

        assertEquals(Direction.RISING, r.trend().direction());
        assertEquals(1.0, r.trend().r2());
        assertEquals(List.of("STEADY_TREND", "DIFFERS_FROM_WIKI"), codes(r));
        assertEquals(Confidence.HIGH, r.confidence());
        assertTrue(text(r, "DIFFERS_FROM_WIKI").startsWith("Does better than Polish Wikipedia"));
        assertTrue(r.spikes().isEmpty());
        assertNull(r.seasonalPeak());
    }

    @Test
    void articleWithNoViewsDoesNotBreak() {
        TopicResult r = analyzer.analyze("X", "sk", "X", new Series(YearMonth.of(2024, 8), new long[24], flat(24, 1_000)));

        assertTrue(r.found());
        assertEquals(0L, r.monthlyMedian());
        assertEquals(0L, r.previous12());
        assertNull(r.growthPct());
        assertNull(r.growthVsWikiPct());
        assertEquals(Direction.FLAT, r.trend().direction());
        assertTrue(r.spikes().isEmpty());
        assertNull(r.seasonalPeak());
        assertEquals(List.of("LOW_VOLUME", "NOISY_TREND"), codes(r));
        assertEquals(Confidence.LOW, r.confidence());
    }

    @Test
    void shortPeriodHasNoYearOverYearNumbers() {
        TopicResult r = analyzer.analyze("Astronomy", "uk", "Астрономія", new Series(YearMonth.of(2025, 8),
                Arrays.copyOfRange(UK_ASTRONOMY.views(), 12, 24), Arrays.copyOfRange(UK_ASTRONOMY.wikiViews(), 12, 24)));

        assertNull(r.last12());
        assertNull(r.growthPct());
        assertNull(r.growthVsWikiPct());
        assertNull(r.seasonalPeak());
        assertEquals(Direction.FALLING, r.trend().direction());
        assertTrue(codes(r).contains("SHORT_PERIOD"));
        assertTrue(r.confidence() != Confidence.HIGH);
    }

    @Test
    void longerPeriodComparesOnlyTheLastTwoYears() {
        long[] views = new long[36];
        Arrays.fill(views, 0, 12, 99_999);
        Arrays.fill(views, 12, 24, 1_000);
        Arrays.fill(views, 24, 36, 1_500);
        TopicResult r = analyzer.analyze("X", "uk", "X", new Series(YearMonth.of(2023, 8), views, flat(36, 1_000_000)));

        assertEquals(18_000L, r.last12());
        assertEquals(12_000L, r.previous12());
        assertEquals(50.0, r.growthPct());
        assertEquals(0.0, r.wikiGrowthPct());
    }

    @Test
    void seasonalPeakNeedsTheSameMonthInMostYears() {
        long[] views = new long[36];
        Arrays.fill(views, 1_000);
        views[1] = 3_000;
        views[13] = 3_000;
        views[30] = 3_000;
        assertEquals("09", TopicAnalyzer.seasonalPeak(new Series(YearMonth.of(2023, 8), views, new long[36])));

        views[13] = 1_000;
        views[18] = 3_000;
        assertEquals("02", TopicAnalyzer.seasonalPeak(new Series(YearMonth.of(2023, 8), views, new long[36])));

        views[18] = 1_200;
        assertNull(TopicAnalyzer.seasonalPeak(new Series(YearMonth.of(2023, 8), views, new long[36])));
    }

    private static long[] flat(int months, long value) {
        long[] values = new long[months];
        Arrays.fill(values, value);
        return values;
    }

    private static List<String> codes(TopicResult r) {
        return r.reasons().stream().map(Reason::code).toList();
    }

    private static String text(TopicResult r, String code) {
        return r.reasons().stream().filter(x -> x.code().equals(code)).findFirst().orElseThrow().text();
    }
}
