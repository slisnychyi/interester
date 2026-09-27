package org.interester.report;

import lombok.experimental.UtilityClass;
import org.interester.analyze.Text;
import org.interester.analyze.TopicAnalyzer;
import org.interester.model.AnalyzeResponse;
import org.interester.model.Confidence;
import org.interester.model.TopicResult;

import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@UtilityClass
public class Recommendation {

    public static List<String> summary(AnalyzeResponse response) {
        return lines(response, Labels.of(response.query()));
    }

    static List<String> lines(AnalyzeResponse response, Labels labels) {
        List<TopicResult> ranked = response.ranking().stream()
                .map(item -> response.results().stream()
                        .filter(r -> r.found() && r.topic().equals(item.topic()) && r.lang().equals(item.lang()))
                        .findFirst().orElseThrow())
                .toList();
        List<TopicResult> usable = ranked.stream().filter(r -> r.confidence() != Confidence.LOW).toList();

        List<String> lines = new ArrayList<>();
        if (ranked.isEmpty()) {
            lines.add("No Wikipedia article was found. Try a related, more general topic.");
        } else if (usable.isEmpty()) {
            lines.add("All signals are weak (low confidence). Do not decide on this data alone.");
        } else {
            lines.add(verdict(usable.getFirst(), labels));
        }
        addNamesLine(lines, "Weak data, do not rely on: ", ranked, r -> r.confidence() == Confidence.LOW, labels);
        addNamesLine(lines, "No Wikipedia article: ", response.results(), r -> !r.found(), labels);
        String peaks = ranked.stream().map(TopicResult::seasonalPeak).filter(Objects::nonNull).distinct().sorted()
                .map(peak -> Text.monthName(peak, TextStyle.FULL)).collect(Collectors.joining(", "));
        if (!peaks.isEmpty()) {
            lines.add("Views peak every year in " + peaks + ". Plan launches and campaigns for it.");
        }
        return lines;
    }

    static String verdict(TopicResult best, Labels labels) {
        String who = labels.of(best);
        Double share = best.growthVsWikiPct();
        String slope = Text.pct(best.trend().slopePctPerMonth());
        if (share == null) {
            return switch (best.trend().direction()) {
                case RISING -> "Research first: " + who + ". Views rise " + slope + " per month.";
                case FLAT -> "Research first: " + who + ". Views are stable.";
                case FALLING -> "No growing interest found. Best result: " + who + ", views fall " + slope + " per month.";
            };
        }
        if (share >= TopicAnalyzer.WIKI_DIFF_PCT) {
            return "Research first: " + who + ". Its share of Wikipedia views grows (" + Text.pct(share) + ").";
        }
        if (share > -TopicAnalyzer.WIKI_DIFF_PCT) {
            return "Research first: " + who + ". Views changed " + Text.pct(best.growthPct())
                    + ", but it keeps its share of Wikipedia views (" + Text.pct(share) + ").";
        }
        return "No growing interest found. Best result: " + who + ", share of Wikipedia views " + Text.pct(share)
                + ". Test demand another way before investing.";
    }

    private static void addNamesLine(List<String> lines, String prefix, List<TopicResult> results,
                            Predicate<TopicResult> filter, Labels labels) {
        String matching = results.stream().filter(filter).map(labels::of).collect(Collectors.joining(", "));
        if (!matching.isEmpty()) {
            lines.add(prefix + matching + ".");
        }
    }
}
