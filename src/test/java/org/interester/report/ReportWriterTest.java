package org.interester.report;

import org.interester.analyze.Analyzer;
import org.interester.analyze.Analyzer.Analysis;
import org.interester.analyze.Analyzer.Line;
import org.interester.analyze.Series;
import org.interester.analyze.TopicAnalyzer;
import org.interester.model.AnalyzeResponse;
import org.interester.model.AnalyzeResponse.ReportFiles;
import org.interester.model.AnalyzeResponse.Query;
import org.interester.model.ErrorInfo;
import org.interester.model.TopicResult;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportWriterTest {

    private static final YearMonth FROM = YearMonth.of(2024, 9);

    private final Path dir = Path.of("target", "test-reports");

    @Test
    void writesChartAndOnePagePdf() throws Exception {
        List<String> titles = List.of("Астрономія", "Přerušovaný půst", "a", "b", "c", "d", "e", "f", "g", "h", "i");
        Analysis analysis = analysisWithFallingSeries(List.of("Astronomy", "Intermittent fasting"),
                List.of("uk", "pl", "cs", "sk", "de", "tr"), titles);

        ReportFiles files = new ReportWriter(LocalDate.of(2026, 9, 25)).write(analysis, dir);

        Path png = Path.of(files.chart());
        assertTrue(png.isAbsolute());
        assertTrue(Files.size(png) > 10_000);
        assertEquals("astronomy-intermittent-fasting-uk-pl-cs-sk-de-tr-2024-09-2026-08.pdf",
                Path.of(files.pdf()).getFileName().toString());

        PdfReader reader = new PdfReader(files.pdf());
        assertEquals(1, reader.getNumberOfPages());
        String text = new PdfTextExtractor(reader).getTextFromPage(1);
        reader.close();
        assertTrue(text.contains("What to research next"), text);
        assertTrue(text.contains("Астрономія"), "Cyrillic must be in the PDF: " + text);
        assertTrue(text.contains("No Wikipedia article: Intermittent fasting · Turkish (tr)"), text);
        assertTrue(text.contains("Limits"), text);
    }

    private static Analysis analysisWithFallingSeries(List<String> topics, List<String> langs, List<String> titles) {
        TopicAnalyzer topicAnalyzer = new TopicAnalyzer();
        List<TopicResult> results = new ArrayList<>();
        List<Line> lines = new ArrayList<>();
        int i = 0;
        for (String topic : topics) {
            for (String lang : langs) {
                if (i >= titles.size()) {
                    results.add(TopicResult.notFound(topic, lang,
                            new ErrorInfo("ARTICLE_NOT_FOUND", "No " + lang + " article.", "hint")));
                    continue;
                }
                long[] views = new long[24];
                long[] wiki = new long[24];
                for (int m = 0; m < 24; m++) {
                    views[m] = Math.round(2_000 * Math.pow(1 - 0.01 * (i + 1), m)) + (m % 12 == 0 ? 3_000 : 0);
                    wiki[m] = 1_000_000 - 5_000L * m;
                }
                Series series = new Series(FROM, views, wiki);
                TopicResult result = topicAnalyzer.analyze(topic, lang, titles.get(i++), series);
                results.add(result);
                lines.add(new Line(result, series));
            }
        }
        Query query = new Query(topics, langs, FROM.toString(), FROM.plusMonths(23).toString(), 24);
        return new Analysis(AnalyzeResponse.ok(query, results, Analyzer.ranking(results), List.of()), lines);
    }
}
