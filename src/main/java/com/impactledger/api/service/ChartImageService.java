package com.impactledger.api.service;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.RingPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Renders small, quiet chart PNGs for embedding into the PDF report.
 * Deliberately avoids JFreeChart's default styling (drop shadows, baked-in
 * labels/legends, primary-color palettes) in favor of a flat, muted look —
 * the legend is drawn separately in the PDF as text next to the chart.
 */
@Service
public class ChartImageService {

    private static final Color HAIRLINE = new Color(0xE3, 0xE3, 0xE3);

    /**
     * A clean donut (ring) chart, no title, no legend, no data labels, no shadow.
     * Colors are assigned in the exact order of {@code orderedKeys} from {@code palette}.
     */
    public byte[] donutChart(List<String> orderedKeys, Map<String, Long> data, Color[] palette, int width, int height) throws IOException {
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        for (String key : orderedKeys) {
            Long value = data.get(key);
            if (value != null && value > 0) {
                dataset.setValue(key, value);
            }
        }

        JFreeChart chart = ChartFactory.createRingChart(null, dataset, false, false, false);
        chart.setBackgroundPaint(Color.WHITE);
        chart.setBorderVisible(false);
        if (chart.getTitle() != null) {
            chart.getTitle().setVisible(false);
        }

        RingPlot plot = (RingPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setShadowPaint(null);
        plot.setLabelGenerator(null);
        plot.setCircular(true);
        plot.setSectionDepth(0.38);
        plot.setSectionOutlinesVisible(false);
        plot.setSeparatorsVisible(true);
        plot.setSeparatorPaint(Color.WHITE);
        plot.setSeparatorStroke(new BasicStroke(2f));
        plot.setInteriorGap(0.02);

        int colorIndex = 0;
        for (String key : orderedKeys) {
            Long value = data.get(key);
            if (value != null && value > 0) {
                plot.setSectionPaint(key, palette[colorIndex % palette.length]);
                colorIndex++;
            }
        }

        return toPng(chart, width, height);
    }

    /**
     * A flat, single-series bar chart: no gradient "glossy" bars, no shadow,
     * light gridlines only, thin bars.
     */
    public byte[] barChart(String categoryLabel, String valueLabel, Map<String, Long> data, Color barColor, int width, int height) throws IOException {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        data.forEach((k, v) -> dataset.addValue(v, valueLabel, k));

        JFreeChart chart = ChartFactory.createBarChart(
                null, categoryLabel, valueLabel, dataset,
                PlotOrientation.VERTICAL, false, false, false
        );
        chart.setBackgroundPaint(Color.WHITE);
        chart.setBorderVisible(false);
        if (chart.getTitle() != null) {
            chart.getTitle().setVisible(false);
        }

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinePaint(HAIRLINE);

        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setAxisLineVisible(false);
        domainAxis.setTickMarksVisible(false);
        domainAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 11));
        domainAxis.setLabelFont(new Font("SansSerif", Font.PLAIN, 11));

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setAxisLineVisible(false);
        rangeAxis.setTickMarksVisible(false);
        rangeAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        rangeAxis.setLabelFont(new Font("SansSerif", Font.PLAIN, 11));
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setShadowVisible(false);
        renderer.setDrawBarOutline(false);
        renderer.setSeriesPaint(0, barColor);
        renderer.setMaximumBarWidth(0.10);

        return toPng(chart, width, height);
    }

    private byte[] toPng(JFreeChart chart, int width, int height) throws IOException {
        BufferedImage image = chart.createBufferedImage(width, height);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }
}