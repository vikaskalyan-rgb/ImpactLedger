package com.impactledger.api.service;

import com.impactledger.api.dto.PdfGenerationRequest;
import com.impactledger.api.dto.StatsResponse;
import com.impactledger.api.entity.Recognition;
import com.impactledger.api.entity.Task;
import com.impactledger.api.entity.enums.AppraisalType;
import com.impactledger.api.exception.BadRequestException;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PdfService {

    private final TaskService taskService;
    private final StatsService statsService;
    private final RecognitionService recognitionService;
    private final ChartImageService chartImageService;

    // ---- Palette: quiet, deliberate, matches the app's own badge colors ----
    private static final Color INK = new Color(0x2B, 0x2B, 0x2B);
    private static final Color MUTED = new Color(0x8F, 0x8F, 0x8F);
    private static final Color HAIRLINE = new Color(0xE3, 0xE3, 0xE3);
    private static final Color HAIRLINE_STRONG = new Color(0xC9, 0xC9, 0xC9);
    private static final Color BRAND = new Color(0x1F, 0x3A, 0x5F);

    private static final Color P1_COLOR = new Color(0xE5, 0x48, 0x4D);
    private static final Color P2_COLOR = new Color(0xE0, 0xA8, 0x2E);
    private static final Color P3_COLOR = new Color(0x3E, 0x92, 0xCC);
    private static final Color MINOR_COLOR = new Color(0x5F, 0x6B, 0x80);

    private static final Color[] CHART_PALETTE = {
            BRAND,
            new Color(0x3E, 0x92, 0xCC), // teal
            new Color(0xC9, 0xA4, 0x5C), // gold
            new Color(0xB5, 0x65, 0x7A), // rose
            new Color(0x7A, 0x9E, 0x7E), // sage
            new Color(0x8A, 0x93, 0xA6), // slate
            new Color(0xA0, 0x7A, 0xB5), // muted purple
    };

    // ---- Type: serif for the name (mirrors an invoice letterhead), sans everywhere else ----
    private static final Font FONT_NAME = FontFactory.getFont(FontFactory.TIMES_BOLD, 27, INK);
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA, 11, MUTED);
    private static final Font FONT_STAT_NUMBER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, INK);
    private static final Font FONT_STAT_LABEL = FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED);
    private static final Font FONT_SECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, INK);
    private static final Font FONT_TABLE_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, MUTED);
    private static final Font FONT_BODY = FontFactory.getFont(FontFactory.HELVETICA, 9, INK);
    private static final Font FONT_BODY_MUTED = FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED);
    private static final Font FONT_SMALL_ITALIC = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, MUTED);
    private static final Font FONT_LEGEND_LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, INK);
    private static final Font FONT_LEGEND_SUB = FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED);
    private static final Font FONT_LINK = FontFactory.getFont(FontFactory.HELVETICA, 8, BRAND);

    @Transactional(readOnly = true)
    public byte[] generate(PdfGenerationRequest request) {
        if (request.getTaskIds() == null || request.getTaskIds().isEmpty()) {
            throw new BadRequestException("Select at least one task to include in the PDF");
        }
        List<Task> tasks = taskService.getEntitiesByIds(request.getTaskIds());

        String profileName = (request.getProfileName() == null || request.getProfileName().isBlank())
                ? "Your Name" : request.getProfileName();
        String profileTitle = (request.getProfileTitle() == null || request.getProfileTitle().isBlank())
                ? "Software Engineer" : request.getProfileTitle();

        try {
            Document document = new Document(PageSize.A4, 42, 42, 56, 48);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            writer.setPageEvent(new FooterEvent());
            document.open();

            switch (request.getMode()) {
                case APPRAISAL -> buildAppraisalPdf(document, request, tasks, profileName, profileTitle);
                case MONTHLY -> buildMonthlyPdf(document, request, tasks, profileName, profileTitle);
            }

            document.close();
            return baos.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------------------
    // APPRAISAL MODE
    // ---------------------------------------------------------------------

    private void buildAppraisalPdf(Document document, PdfGenerationRequest request, List<Task> tasks,
                                   String profileName, String profileTitle) throws DocumentException, IOException {
        if (request.getAppraisalType() == null) {
            throw new BadRequestException("appraisalType (MIDYEAR or YEAR_END) is required for APPRAISAL mode");
        }
        int maxQuarter = request.getAppraisalType() == AppraisalType.MIDYEAR ? 2 : 4;
        String periodLabel = request.getAppraisalType() == AppraisalType.MIDYEAR
                ? "Mid-Year Review " + request.getYear() + " · Jan\u2013Jun"
                : "Year-End Review " + request.getYear();

        addCoverPage(document, profileName, profileTitle, periodLabel, tasks);

        document.newPage();
        addSectionHeading(document, "Overview");
        addOverviewCharts(document, tasks);

        Map<Integer, List<Task>> byQuarter = tasks.stream()
                .filter(t -> quarterOf(t) != null && quarterOf(t) <= maxQuarter)
                .collect(Collectors.groupingBy(this::quarterOf, TreeMap::new, Collectors.toList()));

        for (Map.Entry<Integer, List<Task>> entry : byQuarter.entrySet()) {
            document.newPage();
            addSectionHeading(document, "Q" + entry.getKey() + " " + request.getYear() + " Highlights");
            addQuarterMonthlyMetrics(document, entry.getValue());
            addSpacer(document, 8f);
            addCrispTaskTable(document, entry.getValue());
        }

        addHighlightsAndRecognition(document, request.getCompanyId(), tasks, periodStart(request), periodEnd(request));
    }

    private void addQuarterMonthlyMetrics(Document document, List<Task> quarterTasks) throws DocumentException {
        Map<String, List<Task>> byMonth = quarterTasks.stream()
                .collect(Collectors.groupingBy(t -> monthLabel(referenceDate(t)), TreeMap::new, Collectors.toList()));

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingAfter(4);
        addTableHeaderCell(table, "MONTH");
        addTableHeaderCell(table, "TASKS");
        addTableHeaderCell(table, "P1 / MAJOR");
        addTableHeaderCell(table, "PRs MERGED");

        for (Map.Entry<String, List<Task>> monthEntry : byMonth.entrySet()) {
            List<Task> monthTasks = monthEntry.getValue();
            long p1Major = monthTasks.stream().filter(t ->
                    (t.getPriority() != null && t.getPriority().name().equals("P1"))
                            || (t.getComplexity() != null && t.getComplexity().name().equals("MAJOR"))
            ).count();
            long prs = monthTasks.stream().mapToLong(t -> t.getPrLinks() != null ? t.getPrLinks().size() : 0).sum();

            addTableBodyCell(table, monthEntry.getKey(), Element.ALIGN_LEFT);
            addTableBodyCell(table, String.valueOf(monthTasks.size()), Element.ALIGN_LEFT);
            addTableBodyCell(table, String.valueOf(p1Major), Element.ALIGN_LEFT);
            addTableBodyCell(table, String.valueOf(prs), Element.ALIGN_LEFT);
        }
        document.add(table);
    }

    // ---------------------------------------------------------------------
    // MONTHLY MODE
    // ---------------------------------------------------------------------

    private void buildMonthlyPdf(Document document, PdfGenerationRequest request, List<Task> tasks,
                                 String profileName, String profileTitle) throws DocumentException, IOException {
        String periodLabel;
        if (request.getCustomStartDate() != null && request.getCustomEndDate() != null) {
            periodLabel = "Progress Update \u00b7 " + request.getCustomStartDate() + " to " + request.getCustomEndDate();
        } else if (request.getMonth() != null) {
            YearMonth ym = YearMonth.of(request.getYear(), request.getMonth());
            periodLabel = "Progress Update \u00b7 " + ym.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + request.getYear();
        } else {
            periodLabel = "Progress Update \u00b7 " + request.getYear();
        }

        addCoverPage(document, profileName, profileTitle, periodLabel, tasks);

        document.newPage();
        addSectionHeading(document, "Overview");
        addOverviewCharts(document, tasks);

        document.newPage();
        addSectionHeading(document, "Tasks This Period");
        addCrispTaskTable(document, tasks);

        addHighlightsAndRecognition(document, request.getCompanyId(), tasks, periodStart(request), periodEnd(request));
    }

    // ---------------------------------------------------------------------
    // Cover page
    // ---------------------------------------------------------------------

    private void addCoverPage(Document document, String name, String title, String periodLabel, List<Task> tasks) throws DocumentException {
        addSpacer(document, 70f);

        Paragraph nameP = new Paragraph(name, FONT_NAME);
        nameP.setAlignment(Element.ALIGN_CENTER);
        document.add(nameP);

        Paragraph titleP = new Paragraph(title.toUpperCase(Locale.ENGLISH), FONT_SUBTITLE);
        titleP.setAlignment(Element.ALIGN_CENTER);
        titleP.setSpacingBefore(4f);
        document.add(titleP);

        Paragraph periodP = new Paragraph(periodLabel, FONT_SUBTITLE);
        periodP.setAlignment(Element.ALIGN_CENTER);
        periodP.setSpacingBefore(2f);
        periodP.setSpacingAfter(46f);
        document.add(periodP);

        long totalPrs = tasks.stream().mapToLong(t -> t.getPrLinks() != null ? t.getPrLinks().size() : 0).sum();
        long totalDocs = tasks.stream().filter(t -> t.getDesignDocLink() != null && !t.getDesignDocLink().isBlank()).count();
        long completed = tasks.stream().filter(t -> t.getStatus() != null && t.getStatus().name().equals("COMPLETED")).count();
        long p1s = tasks.stream().filter(t -> t.getPriority() != null && t.getPriority().name().equals("P1")).count();

        document.add(statStrip(
                new String[]{String.valueOf(tasks.size()), String.valueOf(completed), String.valueOf(p1s), String.valueOf(totalPrs)},
                new String[]{"TASKS DELIVERED", "COMPLETED", "P1 INITIATIVES", "PRs MERGED"}
        ));

        Paragraph docsP = new Paragraph(totalDocs + " design docs authored or updated", FONT_SMALL_ITALIC);
        docsP.setAlignment(Element.ALIGN_CENTER);
        docsP.setSpacingBefore(14f);
        document.add(docsP);
    }

    /** A minimal, borderless stat strip: big number, small caps label, thin vertical dividers. */
    private PdfPTable statStrip(String[] numbers, String[] labels) throws DocumentException {
        PdfPTable table = new PdfPTable(numbers.length);
        table.setWidthPercentage(90);
        table.setHorizontalAlignment(Element.ALIGN_CENTER);
        for (int i = 0; i < numbers.length; i++) {
            PdfPCell cell = new PdfPCell();
            cell.setBorder(i == 0 ? Rectangle.NO_BORDER : Rectangle.LEFT);
            cell.setBorderColor(HAIRLINE);
            cell.setBorderWidthLeft(0.75f);
            cell.setUseVariableBorders(true);
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            Paragraph p = new Paragraph();
            p.setAlignment(Element.ALIGN_CENTER);
            p.add(new Chunk(numbers[i] + "\n", FONT_STAT_NUMBER));
            p.add(new Chunk(labels[i], FONT_STAT_LABEL));
            cell.addElement(p);
            table.addCell(cell);
        }
        return table;
    }

    // ---------------------------------------------------------------------
    // Overview: donut charts with a text legend + a quiet bar chart
    // ---------------------------------------------------------------------

    private void addOverviewCharts(Document document, List<Task> tasks) throws DocumentException, IOException {
        StatsResponse stats = statsService.buildStats(tasks);

        List<String> priorityOrder = List.of("P1", "P2", "P3", "MINOR");
        Color[] priorityColors = {P1_COLOR, P2_COLOR, P3_COLOR, MINOR_COLOR};
        document.add(donutWithLegend("By Priority", stats.getByPriority(), priorityOrder, priorityColors));
        addSpacer(document, 12f);

        List<String> typeOrder = new ArrayList<>(stats.getByTaskType().keySet());
        document.add(donutWithLegend("By Task Type", stats.getByTaskType(), typeOrder, CHART_PALETTE));
        addSpacer(document, 14f);

        if (!stats.getTasksCompletedByMonth().isEmpty()) {
            Paragraph monthHeading = new Paragraph("Completed Tasks by Month", FONT_LEGEND_LABEL);
            monthHeading.setSpacingAfter(6f);
            document.add(monthHeading);
            byte[] monthPng = chartImageService.barChart("Month", "Tasks", stats.getTasksCompletedByMonth(), BRAND, 480, 170);
            Image monthImg = Image.getInstance(monthPng);
            monthImg.scaleToFit(480, 170);
            monthImg.setAlignment(Element.ALIGN_LEFT);
            document.add(monthImg);
            addSpacer(document, 10f);
        }

        if (!stats.getByTechStack().isEmpty()) {
            String techLine = stats.getByTechStack().entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                    .limit(10)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.joining("   \u00b7   "));
            Paragraph techLabel = new Paragraph("TECH TOUCHED", FONT_STAT_LABEL);
            techLabel.setSpacingBefore(4f);
            document.add(techLabel);
            Paragraph techP = new Paragraph(techLine, FONT_BODY_MUTED);
            techP.setSpacingBefore(2f);
            document.add(techP);
        }
    }

    /** Chart on the left, a clean text legend (swatch + label + share) on the right — like a quotation's cost breakdown. */
    private PdfPTable donutWithLegend(String heading, Map<String, Long> data, List<String> orderedKeys, Color[] colors) throws DocumentException, IOException {
        long total = orderedKeys.stream().mapToLong(k -> data.getOrDefault(k, 0L)).sum();

        List<String> presentKeys = orderedKeys.stream().filter(k -> data.getOrDefault(k, 0L) > 0).toList();
        byte[] donutPng = chartImageService.donutChart(presentKeys, data, colors, 200, 200);

        PdfPTable outer = new PdfPTable(new float[]{1f, 1.3f});
        outer.setWidthPercentage(100);

        PdfPCell headingCell = new PdfPCell(new Phrase(heading, FONT_SECTION));
        headingCell.setColspan(2);
        headingCell.setBorder(Rectangle.NO_BORDER);
        headingCell.setPaddingBottom(8);
        outer.addCell(headingCell);

        PdfPCell chartCell = new PdfPCell(Image.getInstance(donutPng), false);
        chartCell.setBorder(Rectangle.NO_BORDER);
        chartCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        chartCell.setPadding(4);
        outer.addCell(chartCell);

        PdfPTable legend = new PdfPTable(new float[]{0.12f, 1f});
        legend.setWidthPercentage(100);
        int colorIndex = 0;
        for (String key : presentKeys) {
            long value = data.getOrDefault(key, 0L);
            double pct = total > 0 ? (value * 100.0 / total) : 0;

            PdfPCell swatch = new PdfPCell();
            swatch.setBorder(Rectangle.NO_BORDER);
            swatch.setBackgroundColor(colors[colorIndex % colors.length]);
            swatch.setFixedHeight(10f);
            swatch.setPaddingTop(4);
            legend.addCell(swatch);

            PdfPCell labelCell = new PdfPCell();
            labelCell.setBorder(Rectangle.NO_BORDER);
            labelCell.setPaddingBottom(6);
            Paragraph p = new Paragraph();
            p.add(new Chunk(key + "  ", FONT_LEGEND_LABEL));
            p.add(new Chunk(String.format("%.0f%%", pct), FONT_LEGEND_SUB));
            p.add(new Chunk("  \u00b7  " + value + " task" + (value == 1 ? "" : "s"), FONT_LEGEND_SUB));
            labelCell.addElement(p);
            legend.addCell(labelCell);

            colorIndex++;
        }
        PdfPCell legendCell = new PdfPCell(legend);
        legendCell.setBorder(Rectangle.NO_BORDER);
        legendCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        legendCell.setPadding(4);
        outer.addCell(legendCell);

        return outer;
    }

    // ---------------------------------------------------------------------
    // Task table — decluttered: no raw URLs, tighter impact text, colored priority
    // ---------------------------------------------------------------------

    private void addCrispTaskTable(Document document, List<Task> tasks) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{2.7f, 1f, 3.4f, 1.6f});
        table.setWidthPercentage(100);
        addTableHeaderCell(table, "TASK");
        addTableHeaderCell(table, "PRIORITY");
        addTableHeaderCell(table, "IMPACT");
        addTableHeaderCell(table, "LINKS");

        List<Task> sorted = tasks.stream()
                .sorted((a, b) -> {
                    LocalDate da = referenceDate(a);
                    LocalDate db = referenceDate(b);
                    if (da == null || db == null) return 0;
                    return db.compareTo(da);
                })
                .toList();

        for (Task t : sorted) {
            PdfPCell taskCell = bodyCell();
            Paragraph taskP = new Paragraph();
            taskP.add(new Chunk(t.getTitle() + "\n", FONT_BODY));
            taskP.add(new Chunk(t.getTicketId(), FONT_BODY_MUTED));
            taskCell.addElement(taskP);
            table.addCell(taskCell);

            PdfPCell priorityCell = bodyCell();
            Font priorityFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, priorityColor(t.getPriority() != null ? t.getPriority().name() : "MINOR"));
            priorityCell.addElement(new Paragraph(t.getPriority() != null ? t.getPriority().name() : "-", priorityFont));
            table.addCell(priorityCell);

            PdfPCell impactCell = bodyCell();
            impactCell.addElement(new Paragraph(crisp(t.getImpact() != null && !t.getImpact().isBlank() ? t.getImpact() : t.getDescription(), 150), FONT_BODY));
            table.addCell(impactCell);

            PdfPCell linksCell = bodyCell();
            addLinksParagraph(linksCell, t);
            table.addCell(linksCell);
        }
        document.add(table);
    }

    private void addLinksParagraph(PdfPCell cell, Task t) {
        Paragraph p = new Paragraph();
        boolean any = false;
        if (t.getPrLinks() != null && !t.getPrLinks().isEmpty()) {
            if (t.getPrLinks().size() == 1) {
                Chunk chunk = new Chunk("View PR \u2197", FONT_LINK);
                chunk.setAnchor(t.getPrLinks().get(0));
                p.add(chunk);
            } else {
                p.add(new Chunk(t.getPrLinks().size() + " PRs merged", FONT_BODY_MUTED));
            }
            any = true;
        }
        if (t.getDesignDocLink() != null && !t.getDesignDocLink().isBlank()) {
            if (any) p.add(Chunk.NEWLINE);
            Chunk chunk = new Chunk("View Doc \u2197", FONT_LINK);
            chunk.setAnchor(t.getDesignDocLink());
            p.add(chunk);
            any = true;
        }
        if (!any) {
            p.add(new Chunk("\u2014", FONT_BODY_MUTED));
        }
        cell.addElement(p);
    }

    private Color priorityColor(String priority) {
        return switch (priority) {
            case "P1" -> P1_COLOR;
            case "P2" -> P2_COLOR;
            case "P3" -> P3_COLOR;
            default -> MINOR_COLOR;
        };
    }

    // ---------------------------------------------------------------------
    // Highlights & Recognition
    // ---------------------------------------------------------------------

    private void addHighlightsAndRecognition(Document document, Long companyId, List<Task> tasks,
                                             LocalDate start, LocalDate end) throws DocumentException {
        List<Task> highlights = tasks.stream().filter(Task::isHighlight).toList();
        List<Recognition> recognitions = (start != null && end != null)
                ? recognitionService.findForPeriod(companyId, start, end)
                : List.of();

        if (highlights.isEmpty() && recognitions.isEmpty()) {
            return;
        }

        document.newPage();
        if (!highlights.isEmpty()) {
            addSectionHeading(document, "Highlights");
            for (Task t : highlights) {
                Paragraph p = new Paragraph();
                p.add(new Chunk(t.getTitle() + "  ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, INK)));
                p.add(new Chunk("(" + t.getTicketId() + ")", FONT_SMALL_ITALIC));
                document.add(p);
                Paragraph impactP = new Paragraph(crisp(t.getImpact() != null ? t.getImpact() : t.getDescription(), 220), FONT_BODY);
                impactP.setSpacingAfter(10f);
                document.add(impactP);
            }
        }

        if (!recognitions.isEmpty()) {
            addSectionHeading(document, "Recognition");
            for (Recognition r : recognitions) {
                Paragraph p = new Paragraph();
                p.add(new Chunk(r.getDate() + " \u2014 " + r.getSource() + ": ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, INK)));
                p.add(new Chunk(r.getMessage(), FONT_BODY));
                p.setSpacingAfter(6f);
                document.add(p);
            }
        }
    }

    // ---------------------------------------------------------------------
    // Small helpers
    // ---------------------------------------------------------------------

    private void addSectionHeading(Document document, String text) throws DocumentException {
        Paragraph p = new Paragraph(text, FONT_SECTION);
        p.setSpacingAfter(2f);
        document.add(p);

        PdfPTable rule = new PdfPTable(1);
        rule.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(HAIRLINE_STRONG);
        cell.setBorderWidthBottom(1f);
        cell.setUseVariableBorders(true);
        cell.setFixedHeight(6f);
        rule.addCell(cell);
        rule.setSpacingAfter(10f);
        document.add(rule);
    }

    private void addSpacer(Document document, float height) throws DocumentException {
        Paragraph p = new Paragraph(" ");
        p.setSpacingAfter(height);
        document.add(p);
    }

    private void addTableHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_TABLE_HEADER));
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(HAIRLINE_STRONG);
        cell.setBorderWidthBottom(1f);
        cell.setUseVariableBorders(true);
        cell.setPadding(6);
        cell.setBackgroundColor(Color.WHITE);
        table.addCell(cell);
    }

    private void addTableBodyCell(PdfPTable table, String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, FONT_BODY));
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(HAIRLINE);
        cell.setBorderWidthBottom(0.5f);
        cell.setUseVariableBorders(true);
        cell.setPadding(6);
        cell.setHorizontalAlignment(alignment);
        cell.setBackgroundColor(Color.WHITE);
        table.addCell(cell);
    }

    private PdfPCell bodyCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(HAIRLINE);
        cell.setBorderWidthBottom(0.5f);
        cell.setUseVariableBorders(true);
        cell.setPadding(7);
        cell.setBackgroundColor(Color.WHITE);
        return cell;
    }

    /** Trims verbose text down for the table so the PDF stays crisp and readable. */
    private String crisp(String text, int limit) {
        if (text == null || text.isBlank()) return "\u2014";
        String cleaned = text.strip().replaceAll("\\s+", " ");
        return cleaned.length() > limit ? cleaned.substring(0, limit).trim() + "\u2026" : cleaned;
    }

    private LocalDate referenceDate(Task t) {
        return t.getEndDate() != null ? t.getEndDate() : t.getStartDate();
    }

    private Integer quarterOf(Task t) {
        LocalDate d = referenceDate(t);
        if (d == null) return null;
        return ((d.getMonthValue() - 1) / 3) + 1;
    }

    private String monthLabel(LocalDate date) {
        if (date == null) return "Unknown";
        return date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + date.getYear();
    }

    private LocalDate periodStart(PdfGenerationRequest request) {
        if (request.getCustomStartDate() != null) return request.getCustomStartDate();
        if (request.getMode().name().equals("MONTHLY") && request.getMonth() != null) {
            return YearMonth.of(request.getYear(), request.getMonth()).atDay(1);
        }
        return LocalDate.of(request.getYear(), 1, 1);
    }

    private LocalDate periodEnd(PdfGenerationRequest request) {
        if (request.getCustomEndDate() != null) return request.getCustomEndDate();
        if (request.getMode().name().equals("MONTHLY") && request.getMonth() != null) {
            YearMonth ym = YearMonth.of(request.getYear(), request.getMonth());
            return ym.atEndOfMonth();
        }
        int endMonth = (request.getAppraisalType() == AppraisalType.MIDYEAR) ? 6 : 12;
        return YearMonth.of(request.getYear(), endMonth).atEndOfMonth();
    }

    private static class FooterEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            Phrase footer = new Phrase("Page " + writer.getPageNumber(), FONT_SMALL_ITALIC);
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, footer,
                    (document.right() + document.left()) / 2, document.bottom() - 20, 0);
        }
    }
}