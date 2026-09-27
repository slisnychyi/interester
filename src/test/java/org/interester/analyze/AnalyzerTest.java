package org.interester.analyze;

import org.interester.cli.CliArgs;
import org.interester.cli.Command;
import org.interester.data.FakeFetcher;
import org.interester.data.PageviewsClient;
import org.interester.data.WikiData;
import org.interester.data.WikidataClient;
import org.interester.model.AnalyzeResponse;
import org.interester.model.TopicResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.interester.data.FakeFetcher.fixture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalyzerTest {

    private final FakeFetcher http = new FakeFetcher()
            .route("titles=Stargazing", fixture("wikidata-stargazing.json"))
            .route("titles=Nope", fixture("wikidata-missing.json"))
            .route("wikidata", fixture("wikidata-astronomy.json"))
            .route("/aggregate/", fixture("aggregate-uk.json"))
            .route("/per-article/uk.wikipedia/all-access/user/%D0%90%D1%81", fixture("pageviews-uk-astronomy.json"))
            .route("/per-article/", fixture(404, "pageviews-404.json"));
    private final Analyzer analyzer = new Analyzer(new WikiData(new WikidataClient(http), new PageviewsClient(http)));

    @Test
    void missingSitelinkIsANormalResultAndArticleFixesIt() {
        AnalyzeResponse r = analyzer.analyze(args(List.of("Stargazing"), List.of("uk", "cs"), 24, Map.of())).response();

        TopicResult cs = r.results().get(1);
        assertFalse(cs.found());
        assertEquals("ARTICLE_NOT_FOUND", cs.error().code());
        assertTrue(cs.error().hint().contains("--article cs:"));
        assertEquals(1, r.ranking().size());
        assertTrue(r.notes().getFirst().contains("redirects to 'Amateur astronomy'"));

        TopicResult manual = analyzer.analyze(args(List.of("Stargazing"), List.of("cs"), 24, Map.of("cs", "Astronomie")))
                .response().results().getFirst();
        assertEquals("Astronomie", manual.article());
    }

    @Test
    void unpublishedMonthMovesThePeriodBack() {
        CliArgs args = new CliArgs(Command.ANALYZE, List.of("Astronomy"), List.of("uk"), 24,
                YearMonth.of(2026, 8), Map.of(), Path.of("out"));

        AnalyzeResponse r = analyzer.analyze(args).response();

        assertEquals("Астрономія", r.results().getFirst().article());
        assertEquals("2026-07", r.query().to());
        assertEquals("2024-08", r.query().from());
        assertTrue(r.notes().getFirst().contains("2026-08 is not published yet"));
    }

    @Test
    void shortPeriodRanksByTrend() {
        AnalyzeResponse r = analyzer.analyze(args(List.of("Astronomy"), List.of("uk"), 12, Map.of())).response();

        assertEquals(null, r.results().getFirst().growthPct());
        assertTrue(r.notes().getFirst().contains("Ranking uses the trend slope"));
    }

    private static CliArgs args(List<String> topics, List<String> langs, int months, Map<String, String> articles) {
        return new CliArgs(Command.ANALYZE, topics, langs, months, YearMonth.of(2026, 7), articles, Path.of("out"));
    }
}
