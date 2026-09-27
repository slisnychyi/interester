package org.interester.report;

import lombok.RequiredArgsConstructor;
import org.interester.analyze.Analyzer.Analysis;
import org.interester.cli.CliException;
import org.interester.model.AnalyzeResponse.ReportFiles;
import org.interester.model.AnalyzeResponse.Query;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class ReportWriter {

    private final LocalDate today;

    public ReportFiles write(Analysis analysis, Path outDir) {
        Query query = analysis.response().query();
        ImageIO.setUseCache(false);
        Path base = outDir.toAbsolutePath().normalize().resolve(fileName(query));
        Path png = Path.of(base + ".png");
        Path pdf = Path.of(base + ".pdf");
        try {
            Files.createDirectories(outDir);
            JFreeChart chart = Chart.create(analysis.chartLines(), Labels.of(query));
            ChartUtils.saveChartAsPNG(png.toFile(), chart, Chart.WIDTH, Chart.HEIGHT);
            try (OutputStream out = Files.newOutputStream(pdf)) {
                new PdfReport().write(analysis, chart, today, out);
            }
        } catch (IOException e) {
            throw CliException.invalidArgs("Cannot write the report to " + outDir.toAbsolutePath().normalize() + ": " + e.getMessage(),
                    "Use --out with a folder you can write to, e.g. --out ./out.");
        }
        return new ReportFiles(png.toString(), pdf.toString());
    }

    static String fileName(Query query) {
        return Stream.concat(query.topics().stream(), query.langs().stream())
                .map(s -> s.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", ""))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("-"))
                + "-" + query.from() + "-" + query.to();
    }
}
