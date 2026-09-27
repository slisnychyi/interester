package org.interester.analyze;

import lombok.RequiredArgsConstructor;
import org.interester.cli.CliArgs;
import org.interester.data.ArticleLinks;
import org.interester.data.WikiData;
import org.interester.model.AnalyzeResponse;
import org.interester.model.AnalyzeResponse.Query;
import org.interester.model.AnalyzeResponse.RankItem;
import org.interester.model.ErrorInfo;
import org.interester.model.TopicResult;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

@RequiredArgsConstructor
public class Analyzer {

    private static final int YEAR_OVER_YEAR_MONTHS = 24;

    private final WikiData data;
    private final TopicAnalyzer topicAnalyzer = new TopicAnalyzer();

    public record Analysis(AnalyzeResponse response, List<Line> chartLines) {
    }

    public record Line(TopicResult result, Series series) {
    }

    public Analysis analyze(CliArgs args) {
        List<String> notes = new ArrayList<>();
        YearMonth end = lastPublishedMonth(args, notes);
        YearMonth from = end.minusMonths(args.months() - 1L);

        List<TopicResult> results = new ArrayList<>();
        List<Line> chartLines = new ArrayList<>();
        for (String topic : args.topics()) {
            ArticleLinks links = data.links(topic);
            if (!links.resolvedEnTitle().equals(topic)) {
                notes.add("'" + topic + "' redirects to '" + links.resolvedEnTitle()
                        + "' on English Wikipedia. Results are for that article.");
            }
            for (String lang : args.langs()) {
                Optional<String> title = Optional.ofNullable(args.manualTitles().get(lang)).or(() -> links.title(lang));
                if (title.isEmpty()) {
                    results.add(TopicResult.notFound(topic, lang, articleNotFound(topic, lang)));
                    continue;
                }
                Series series = new Series(from,
                        data.article(lang, title.get(), from, end).zeroFilledValues(from, end),
                        data.languageTotal(lang, from, end).zeroFilledValues(from, end));
                TopicResult result = topicAnalyzer.analyze(topic, lang, title.get(), series);
                results.add(result);
                chartLines.add(new Line(result, series));
            }
        }
        if (args.months() < YEAR_OVER_YEAR_MONTHS) {
            notes.add("Period is shorter than 24 months, so there is no year-over-year growth. "
                    + "Ranking uses the trend slope.");
        }

        Query query = new Query(args.topics(), args.langs(), from.toString(), end.toString(), args.months());
        return new Analysis(AnalyzeResponse.ok(query, results, ranking(results), notes), chartLines);
    }

    private YearMonth lastPublishedMonth(CliArgs args, List<String> notes) {
        YearMonth end = args.end();
        for (String lang : args.langs()) {
            YearMonth last = data.languageTotal(lang, args.from(), args.end()).lastMonth().orElse(args.from());
            if (last.isBefore(end)) {
                end = last;
            }
        }
        if (end.isBefore(args.end())) {
            notes.add("Data for " + args.end() + " is not published yet. The period ends at " + end + " instead.");
        }
        return end;
    }

    public static List<RankItem> ranking(List<TopicResult> results) {
        List<TopicResult> ranked = results.stream()
                .filter(TopicResult::found)
                .sorted(Comparator.comparing(TopicResult::growthVsWikiPct, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(r -> -r.trend().slopePctPerMonth()))
                .toList();
        return IntStream.range(0, ranked.size())
                .mapToObj(i -> {
                    TopicResult r = ranked.get(i);
                    return new RankItem(i + 1, r.topic(), r.lang(), r.growthVsWikiPct(), r.confidence());
                })
                .toList();
    }

    private static ErrorInfo articleNotFound(String topic, String lang) {
        return new ErrorInfo("ARTICLE_NOT_FOUND",
                "No " + lang + " Wikipedia article linked to '" + topic + "'.",
                "Tell the user. Or retry with --article " + lang + ":<title in that language> if you know one.");
    }
}
