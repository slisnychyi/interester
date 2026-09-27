package org.interester.report;

import org.interester.analyze.Analyzer.Line;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Month;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import lombok.experimental.UtilityClass;

import java.awt.Color;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

@UtilityClass
class Chart {

    static final int WIDTH = 800;
    static final int HEIGHT = 300;

    static JFreeChart create(List<Line> lines, Labels labels) {
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        for (Line line : lines) {
            TimeSeries series = new TimeSeries(labels.of(line.result()));
            double[] index = indexToFirstYearAverage(line.series().views());
            for (int i = 0; i < index.length; i++) {
                YearMonth month = line.series().month(i);
                series.add(new Month(month.getMonthValue(), month.getYear()), index[i]);
            }
            dataset.addSeries(series);
        }
        JFreeChart chart = ChartFactory.createTimeSeriesChart(null, null,
                "Index (first 12 months = 100)", dataset, true, false, false);
        chart.setBackgroundPaint(Color.WHITE);
        chart.getXYPlot().setBackgroundPaint(Color.WHITE);
        chart.getXYPlot().setRangeGridlinePaint(Color.LIGHT_GRAY);
        return chart;
    }

    static double[] indexToFirstYearAverage(long[] views) {
        double base = Arrays.stream(views, 0, Math.min(12, views.length)).average().orElse(0);
        return Arrays.stream(views).mapToDouble(v -> base > 0 ? v / base * 100 : 0).toArray();
    }
}
