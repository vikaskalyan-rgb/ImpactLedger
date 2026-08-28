package com.impactledger.api.service;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Renders small chart PNGs (in-memory) that get embedded straight into the PDF.
 */
@Service
public class ChartImageService {

    private static final Color BRAND = new Color(0x1F, 0x3A, 0x5F);
    private static final Color ACCENT = new Color(0x3E, 0x92, 0xCC);

    public byte[] pieChart(String title, Map<String, Long> data, int width, int height) throws IOException {
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        data.forEach((k, v) -> {
            if (v > 0) dataset.setValue(k, v);
        });
        JFreeChart chart = ChartFactory.createPieChart(title, dataset, false, false, false);
        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 13));
        return toPng(chart, width, height);
    }

    public byte[] barChart(String title, String categoryLabel, String valueLabel, Map<String, Long> data, int width, int height) throws IOException {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        data.forEach((k, v) -> dataset.addValue(v, valueLabel, k));
        JFreeChart chart = ChartFactory.createBarChart(
                title, categoryLabel, valueLabel, dataset,
                PlotOrientation.VERTICAL, false, false, false
        );
        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 13));
        chart.getCategoryPlot().getRenderer().setSeriesPaint(0, ACCENT);
        chart.getCategoryPlot().setBackgroundPaint(Color.WHITE);
        chart.getCategoryPlot().setRangeGridlinePaint(new Color(0xE0, 0xE0, 0xE0));
        return toPng(chart, width, height);
    }

    private byte[] toPng(JFreeChart chart, int width, int height) throws IOException {
        BufferedImage image = chart.createBufferedImage(width, height);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }
}
