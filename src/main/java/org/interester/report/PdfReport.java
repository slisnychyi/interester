package org.interester.report;

import org.interester.analyze.Analyzer.Analysis;
import org.interester.analyze.Text;
import org.interester.model.AnalyzeResponse;
import org.interester.model.AnalyzeResponse.Query;
import org.interester.model.TopicResult;
import org.jfree.chart.JFreeChart;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Image;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.stream.Collectors;

class PdfReport {

    private static final Color GREY = new Color(0x555555);
    private static final String LIMITS = "Limits: views are not willingness to pay. A language is not a country "
            + "(English, Spanish, Portuguese are read worldwide). Only the main article is counted, not related pages. "
            + "Wikipedia traffic falls overall, so compare with the \"Whole wiki\" column.";

    private final Font title = embeddedFont("LiberationSans-Bold", 16, Color.BLACK);
    private final Font heading = embeddedFont("LiberationSans-Bold", 11, Color.BLACK);
    private final Font text = embeddedFont("LiberationSans-Regular", 9, Color.BLACK);
    private final Font cell = embeddedFont("LiberationSans-Regular", 8, Color.BLACK);
    private final Font cellHead = embeddedFont("LiberationSans-Bold", 8, Color.BLACK);
    private final Font small = embeddedFont("LiberationSans-Regular", 7.5f, GREY);

    void write(Analysis analysis, JFreeChart chart, LocalDate today, OutputStream out) throws IOException {
        AnalyzeResponse response = analysis.response();
        Query query = response.query();
        Labels labels = Labels.of(query);

        Document doc = new Document(PageSize.A4, 36, 36, 32, 32);
        PdfWriter.getInstance(doc, out);
        doc.open();

        doc.add(new Paragraph("Wikipedia interest: " + String.join(", ", query.topics()), title));
        doc.add(paragraph("Languages: " + query.langs().stream().map(Text::languageName)
                .collect(Collectors.joining(", ")) + ". Period: " + query.from() + " to " + query.to()
                + " (" + query.months() + " months). Monthly views by people (no bots).", small, 8));

        doc.add(paragraph("What to research next", heading, 4));
        for (String line : Recommendation.lines(response, labels)) {
            doc.add(paragraph("•  " + line, text, 2));
        }

        if (!analysis.chartLines().isEmpty()) {
            Image image = Image.getInstance(chart.createBufferedImage(Chart.WIDTH, Chart.HEIGHT), null);
            image.scaleToFit(doc.getPageSize().getWidth() - 72, 200);
            image.setAlignment(Element.ALIGN_CENTER);
            image.setSpacingBefore(6);
            doc.add(image);
        }

        doc.add(paragraph("Numbers", heading, 6));
        doc.add(table(response.results(), labels));

        for (String note : response.notes()) {
            doc.add(paragraph("Note: " + note, small, 4));
        }
        doc.add(paragraph(LIMITS, small, 6));
        doc.add(paragraph("Source: Wikimedia Pageviews API and Wikidata. Generated " + today + " by interester.",
                small, 2));
        doc.close();
    }

    private PdfPTable table(List<TopicResult> results, Labels labels) {
        String[] head = {"Audience", "Article", "Views / month", "Growth", "Whole wiki", "Share change", "Peak",
                "Confidence"};
        PdfPTable table = new PdfPTable(new float[]{2.6f, 2.2f, 1.1f, 1f, 1f, 1.1f, 0.7f, 1.1f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(3);
        for (String h : head) {
            table.addCell(cell(h, cellHead));
        }
        for (TopicResult r : results) {
            table.addCell(cell(labels.of(r), cell));
            if (!r.found()) {
                table.addCell(cell("no article", cell));
                for (int i = 0; i < head.length - 2; i++) {
                    table.addCell(cell("—", cell));
                }
                continue;
            }
            table.addCell(cell(r.article(), cell));
            table.addCell(cell(Text.number(r.monthlyMedian()), cell));
            table.addCell(cell(pct(r.growthPct()), cell));
            table.addCell(cell(pct(r.wikiGrowthPct()), cell));
            table.addCell(cell(pct(r.growthVsWikiPct()), cell));
            table.addCell(cell(r.seasonalPeak() == null ? "—"
                    : Text.monthName(r.seasonalPeak(), TextStyle.SHORT), cell));
            table.addCell(cell(r.confidence().json(), cell));
        }
        return table;
    }

    private static PdfPCell cell(String value, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(value, font));
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(new Color(0xcccccc));
        c.setPadding(3);
        return c;
    }

    private static Paragraph paragraph(String value, Font font, float spacingBefore) {
        Paragraph p = new Paragraph(value, font);
        p.setSpacingBefore(spacingBefore);
        return p;
    }

    private static String pct(Double value) {
        return value == null ? "—" : Text.pct(value);
    }

    private static Font embeddedFont(String name, float size, Color color) {
        try {
            BaseFont base = BaseFont.createFont("liberation/" + name + ".ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            return new Font(base, size, Font.NORMAL, color);
        } catch (IOException e) {
            throw new IllegalStateException("Font " + name + " is missing from the jar", e);
        }
    }
}
