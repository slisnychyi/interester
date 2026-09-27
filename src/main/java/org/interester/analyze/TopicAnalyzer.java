package org.interester.analyze;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.interester.model.Confidence;
import org.interester.model.Direction;
import org.interester.model.TopicResult;
import org.interester.model.TopicResult.Reason;
import org.interester.model.TopicResult.Spike;
import org.interester.model.TopicResult.Trend;

import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TopicAnalyzer {

    static final long LOW_VOLUME_MEDIAN = 100;
    static final double NOISY_R2 = 0.3;
    static final double STEADY_R2 = 0.6;
    static final double FLAT_SLOPE_PCT_PER_MONTH = 1.0;
    static final double SPIKE_FACTOR = 3.0;
    public static final double WIKI_DIFF_PCT = 10.0;
    static final double SEASONAL_PEAK_FACTOR_OF_YEAR_MEDIAN = 1.5;

    private static final Set<String> LOWERS = Set.of("NOISY_TREND", "SPIKE_DRIVEN", "SHORT_PERIOD", "FOLLOWS_WIKI");
    private static final Set<String> RAISES = Set.of("STEADY_TREND", "DIFFERS_FROM_WIKI");

    record YearOverYear(long last12, long previous12, Double growthPct, Double wikiGrowthPct,
                        Double growthVsWikiPct, Double growthWithoutSpikesPct) {
    }

    public TopicResult analyze(String topic, String lang, String article, Series s) {
        double median = median(s.views());
        Trend trend = trend(s.views());
        List<Spike> spikes = spikes(s, median);
        String seasonalPeak = seasonalPeak(s);
        YearOverYear yoy = s.months() >= 24 ? yearOverYear(s, median) : null;
        List<Reason> reasons = reasons(lang, s.months(), median, trend, spikes, seasonalPeak, yoy);

        TopicResult.TopicResultBuilder result = TopicResult.builder()
                .topic(topic).lang(lang).article(article).found(true)
                .monthlyMedian(Math.round(median)).trend(trend).spikes(spikes).seasonalPeak(seasonalPeak)
                .confidence(confidence(reasons)).reasons(reasons);
        if (yoy != null) {
            result.last12(yoy.last12()).previous12(yoy.previous12()).growthPct(yoy.growthPct())
                    .wikiGrowthPct(yoy.wikiGrowthPct()).growthVsWikiPct(yoy.growthVsWikiPct());
        }
        return result.build();
    }

    static YearOverYear yearOverYear(Series s, double median) {
        int n = s.months();
        long[] views = s.views();
        long last12 = sum(views, n - 12, n);
        long previous12 = sum(views, n - 24, n - 12);
        Double growth = growthPct(previous12, last12);
        Double wikiGrowth = growthPct(sum(s.wikiViews(), n - 24, n - 12), sum(s.wikiViews(), n - 12, n));
        Double shareChange = shareChangePct(growth, wikiGrowth);

        long[] withoutSpikes = Arrays.stream(views).map(v -> isSpike(v, median) ? Math.round(median) : v).toArray();
        Double growthWithoutSpikes = growthPct(sum(withoutSpikes, n - 24, n - 12), sum(withoutSpikes, n - 12, n));
        return new YearOverYear(last12, previous12, growth, wikiGrowth, shareChange, growthWithoutSpikes);
    }

    private static Double shareChangePct(Double growth, Double wikiGrowth) {
        return growth == null || wikiGrowth == null ? null : round1(((100 + growth) / (100 + wikiGrowth) - 1) * 100);
    }

    static List<Reason> reasons(String lang, int months, double median, Trend trend, List<Spike> spikes,
                                String seasonalPeak, YearOverYear yoy) {
        List<Reason> reasons = new ArrayList<>();
        if (median < LOW_VOLUME_MEDIAN) {
            reasons.add(new Reason("LOW_VOLUME",
                    "Only about " + Text.number(Math.round(median)) + " views per month. Too few to trust the numbers."));
        }
        if (months < 24) {
            reasons.add(new Reason("SHORT_PERIOD", "Only " + months
                    + " months of data. Growth vs last year and vs whole Wikipedia need 24 months."));
        }
        if (trend.r2() < NOISY_R2) {
            reasons.add(new Reason("NOISY_TREND", "Views jump up and down, the trend is not clear (R² " + trend.r2() + ")."));
        } else if (trend.r2() >= STEADY_R2) {
            reasons.add(new Reason("STEADY_TREND", "Trend is steady (R² " + trend.r2() + ")."));
        }
        if (yoy != null && spikeDriven(yoy.growthPct(), yoy.growthWithoutSpikesPct())) {
            String spikeMonths = spikes.stream().map(Spike::month).collect(Collectors.joining(", "));
            reasons.add(new Reason("SPIKE_DRIVEN", "Growth comes mostly from spike months (" + spikeMonths
                    + "). Without them growth is " + Text.pct(yoy.growthWithoutSpikesPct()) + "."));
        }
        if (yoy != null && yoy.growthVsWikiPct() != null) {
            double vsWiki = yoy.growthVsWikiPct();
            String wiki = Text.languageName(lang) + " Wikipedia overall (" + Text.pct(yoy.wikiGrowthPct()) + ")";
            if (Math.abs(vsWiki) < WIKI_DIFF_PCT) {
                reasons.add(new Reason("FOLLOWS_WIKI", "Moves like " + wiki + ". No extra signal about the topic."));
            } else {
                reasons.add(new Reason("DIFFERS_FROM_WIKI", (vsWiki > 0 ? "Does better than " : "Does worse than ") + wiki
                        + ". Its share of all views changed " + Text.pct(vsWiki) + "."));
            }
        }
        if (seasonalPeak != null) {
            reasons.add(new Reason("SEASONAL", "Views peak every " + Text.monthName(seasonalPeak, TextStyle.FULL) + "."));
        }
        return reasons;
    }

    static Confidence confidence(List<Reason> reasons) {
        List<String> codes = reasons.stream().map(Reason::code).toList();
        long lowers = codes.stream().filter(LOWERS::contains).count();
        if (codes.contains("LOW_VOLUME") || lowers >= 2) {
            return Confidence.LOW;
        }
        if (lowers == 1 || codes.stream().noneMatch(RAISES::contains)) {
            return Confidence.MEDIUM;
        }
        return Confidence.HIGH;
    }

    static Trend trend(long[] views) {
        SimpleRegression regression = new SimpleRegression();
        for (int i = 0; i < views.length; i++) {
            regression.addData(i, views[i]);
        }
        double mean = Arrays.stream(views).average().orElse(0);
        double slopePct = mean > 0 ? regression.getSlope() / mean * 100 : 0;
        double r2 = Double.isNaN(regression.getRSquare()) ? 0 : regression.getRSquare();
        Direction direction = Math.abs(slopePct) < FLAT_SLOPE_PCT_PER_MONTH ? Direction.FLAT
                : slopePct > 0 ? Direction.RISING : Direction.FALLING;
        return new Trend(direction, round1(slopePct), Math.round(r2 * 100) / 100.0);
    }

    static List<Spike> spikes(Series s, double median) {
        List<Spike> spikes = new ArrayList<>();
        for (int i = 0; i < s.months(); i++) {
            if (isSpike(s.views()[i], median)) {
                spikes.add(new Spike(s.month(i).toString(), s.views()[i]));
            }
        }
        return spikes;
    }

    static String seasonalPeak(Series s) {
        int years = s.months() / 12;
        int[] peaksPerMonth = new int[13];
        for (int y = 0; y < years; y++) {
            int start = s.months() - 12 * (y + 1);
            long[] year = Arrays.copyOfRange(s.views(), start, start + 12);
            int top = 0;
            for (int i = 1; i < 12; i++) {
                if (year[i] > year[top]) {
                    top = i;
                }
            }
            if (year[top] > 0 && year[top] >= SEASONAL_PEAK_FACTOR_OF_YEAR_MEDIAN * median(year)) {
                peaksPerMonth[s.month(start + top).getMonthValue()]++;
            }
        }
        for (int month = 1; month <= 12; month++) {
            if (peaksPerMonth[month] >= 2 && peaksPerMonth[month] * 3 >= years * 2) {
                return String.format("%02d", month);
            }
        }
        return null;
    }

    static boolean spikeDriven(Double growth, Double growthWithoutSpikes) {
        if (growth == null || growthWithoutSpikes == null || growth == 0) {
            return false;
        }
        return Math.signum(growth) != Math.signum(growthWithoutSpikes)
                || Math.abs(growthWithoutSpikes) < Math.abs(growth) / 2;
    }

    static double median(long[] values) {
        if (values.length == 0) {
            return 0;
        }
        long[] sorted = values.clone();
        Arrays.sort(sorted);
        int mid = sorted.length / 2;
        return sorted.length % 2 == 1 ? sorted[mid] : (sorted[mid - 1] + sorted[mid]) / 2.0;
    }

    private static boolean isSpike(long views, double median) {
        return median > 0 && views > SPIKE_FACTOR * median;
    }

    private static Double growthPct(long previous, long last) {
        return previous > 0 ? round1(((double) last / previous - 1) * 100) : null;
    }

    private static long sum(long[] values, int from, int to) {
        return Arrays.stream(values, from, to).sum();
    }

    private static double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
