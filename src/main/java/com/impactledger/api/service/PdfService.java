package com.impactledger.api.service;

import com.impactledger.api.dto.PdfGenerationRequest;
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
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
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

    private static final Color BRAND = new Color(0x1F, 0x3A, 0x5F);
    private static final Color ACCENT = new Color(0x3E, 0x92, 0xCC);
    private static final Color LIGHT_GREY = new Color(0xF2, 0xF4, 0xF7);
    private static final Color TEXT_GREY = new Color(0x55, 0x5B, 0x66);

    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 26, BRAND);
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA, 13, TEXT_GREY);
    private static final Font FONT_SECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, BRAND);
    private static final Font FONT_SUBSECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);
    private static final Font FONT_TABLE_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
    private static final Font FONT_TABLE_CELL = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
    private static final Font FONT_STAT_NUMBER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BRAND);
    private static final Font FONT_STAT_LABEL = FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_GREY);
    private static final Font FONT_SMALL_ITALIC = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, TEXT_GREY);

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
            Document document = new Document(PageSize.A4, 36, 36, 54, 46);
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
                ? "Mid-Year Review " + request.getYear() + " (Jan - Jun)"
                : "Year-End Review " + request.getYear();

        addCoverPage(document, profileName, profileTitle, periodLabel, tasks);

        // Charts page
        document.newPage();
        addSectionHeading(document, "Overview");
        addChartsRow(document, tasks);

        // Group tasks by quarter (1-4), keeping only quarters within scope
        Map<Integer, List<Task>> byQuarter = tasks.stream()
                .filter(t -> quarterOf(t) != null && quarterOf(t) <= maxQuarter)
                .collect(Collectors.groupingBy(this::quarterOf, TreeMap::new, Collectors.toList()));

        for (Map.Entry<Integer, List<Task>> entry : byQuarter.entrySet()) {
            document.newPage();
            addSectionHeading(document, "Q" + entry.getKey() + " " + request.getYear() + " Highlights");
            addQuarterMonthlyMetrics(document, entry.getValue());
            addCrispTaskTable(document, entry.getValue());
        }

        addHighlightsAndRecognition(document, request.getCompanyId(), tasks, periodStart(request), periodEnd(request));
    }

    private void addQuarterMonthlyMetrics(Document document, List<Task> quarterTasks) throws DocumentException {
        Map<String, List<Task>> byMonth = quarterTasks.stream()
                .collect(Collectors.groupingBy(t -> monthLabel(referenceDate(t)), TreeMap::new, Collectors.toList()));

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(6);
        table.setSpacingAfter(10);
        addHeaderCell(table, "Month");
        addHeaderCell(table, "Tasks");
        addHeaderCell(table, "P1 / Major");
        addHeaderCell(table, "PRs Merged");

        for (Map.Entry<String, List<Task>> monthEntry : byMonth.entrySet()) {
            List<Task> monthTasks = monthEntry.getValue();
            long p1Major = monthTasks.stream().filter(t ->
                    t.getPriority() != null && t.getPriority().name().equals("P1")
                            || (t.getComplexity() != null && t.getComplexity().name().equals("MAJOR"))
            ).count();
            long prs = monthTasks.stream().mapToLong(t -> t.getPrLinks() != null ? t.getPrLinks().size() : 0).sum();

            addBodyCell(table, monthEntry.getKey());
            addBodyCell(table, String.valueOf(monthTasks.size()));
            addBodyCell(table, String.valueOf(p1Major));
            addBodyCell(table, String.valueOf(prs));
        }
        document.add(table);
    }

    // ---------------------------------------------------------------------
    // MONTHLY MODE (for the end-of-month progress email to leaders)
    // ---------------------------------------------------------------------

    private void buildMonthlyPdf(Document document, PdfGenerationRequest request, List<Task> tasks,
                                 String profileName, String profileTitle) throws DocumentException, IOException {
        String periodLabel;
        if (request.getCustomStartDate() != null && request.getCustomEndDate() != null) {
            periodLabel = "Progress Update: " + request.getCustomStartDate() + " to " + request.getCustomEndDate();
        } else if (request.getMonth() != null) {
            YearMonth ym = YearMonth.of(request.getYear(), request.getMonth());
            periodLabel = "Progress Update: " + ym.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + request.getYear();
        } else {
            periodLabel = "Progress Update: " + request.getYear();
        }

        addCoverPage(document, profileName, profileTitle, periodLabel, tasks);

        document.newPage();
        addSectionHeading(document, "Overview");
        addChartsRow(document, tasks);

        document.newPage();
        addSectionHeading(document, "Tasks This Period");
        addCrispTaskTable(document, tasks);

        addHighlightsAndRecognition(document, request.getCompanyId(), tasks, periodStart(request), periodEnd(request));
    }

    // ---------------------------------------------------------------------
    // Shared building blocks
    // ---------------------------------------------------------------------

    private void addCoverPage(Document document, String name, String title, String periodLabel, List<Task> tasks) throws DocumentException {
        Paragraph spacer = new Paragraph(" ");
        spacer.setSpacingAfter(60f);
        document.add(spacer);

        Paragraph nameP = new Paragraph(name, FONT_TITLE);
        nameP.setAlignment(Element.ALIGN_CENTER);
        document.add(nameP);

        Paragraph titleP = new Paragraph(title, FONT_SUBTITLE);
        titleP.setAlignment(Element.ALIGN_CENTER);
        titleP.setSpacingAfter(4f);
        document.add(titleP);

        Paragraph periodP = new Paragraph(periodLabel, FONT_SUBTITLE);
        periodP.setAlignment(Element.ALIGN_CENTER);
        periodP.setSpacingAfter(40f);
        document.add(periodP);

        // Headline stat cards
        long totalPrs = tasks.stream().mapToLong(t -> t.getPrLinks() != null ? t.getPrLinks().size() : 0).sum();
        long totalDocs = tasks.stream().filter(t -> t.getDesignDocLink() != null && !t.getDesignDocLink().isBlank()).count();
        long completed = tasks.stream().filter(t -> t.getStatus() != null && t.getStatus().name().equals("COMPLETED")).count();
        long p1s = tasks.stream().filter(t -> t.getPriority() != null && t.getPriority().name().equals("P1")).count();

        PdfPTable statTable = new PdfPTable(4);
        statTable.setWidthPercentage(100);
        addStatCell(statTable, String.valueOf(tasks.size()), "Tasks Delivered");
        addStatCell(statTable, String.valueOf(completed), "Completed");
        addStatCell(statTable, String.valueOf(p1s), "P1 Initiatives");
        addStatCell(statTable, String.valueOf(totalPrs), "PRs Merged");
        document.add(statTable);

        Paragraph docsP = new Paragraph(totalDocs + " design docs authored/updated", FONT_SMALL_ITALIC);
        docsP.setAlignment(Element.ALIGN_CENTER);
        docsP.setSpacingBefore(10f);
        document.add(docsP);
    }

    private void addChartsRow(Document document, List<Task> tasks) throws DocumentException, IOException {
        var stats = statsService.buildStats(tasks);

        byte[] priorityPng = chartImageService.pieChart("By Priority", stats.getByPriority(), 260, 220);
        byte[] typePng = chartImageService.pieChart("By Task Type", stats.getByTaskType(), 260, 220);
        byte[] monthPng = chartImageService.barChart("Completed Tasks by Month", "Month", "Tasks", stats.getTasksCompletedByMonth(), 520, 220);

        PdfPTable row = new PdfPTable(2);
        row.setWidthPercentage(100);
        row.setSpacingBefore(8);
        row.addCell(imageCell(priorityPng));
        row.addCell(imageCell(typePng));
        document.add(row);

        Image monthImg = Image.getInstance(monthPng);
        monthImg.scaleToFit(520, 220);
        monthImg.setAlignment(Element.ALIGN_CENTER);
        monthImg.setSpacingBefore(6f);
        document.add(monthImg);

        // Tech stack summary as a compact tag line rather than another chart (keeps page count down)
        if (!stats.getByTechStack().isEmpty()) {
            String techLine = stats.getByTechStack().entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                    .limit(10)
                    .map(e -> e.getKey() + " (" + e.getValue() + ")")
                    .collect(Collectors.joining("   •   "));
            Paragraph techP = new Paragraph("Tech touched: " + techLine, FONT_SMALL_ITALIC);
            techP.setSpacingBefore(10f);
            document.add(techP);
        }
    }

    private void addCrispTaskTable(Document document, List<Task> tasks) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{2.6f, 1.4f, 3f, 2f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);
        addHeaderCell(table, "Task");
        addHeaderCell(table, "Priority / Type");
        addHeaderCell(table, "Impact");
        addHeaderCell(table, "Links");

        List<Task> sorted = tasks.stream()
                .sorted((a, b) -> {
                    LocalDate da = referenceDate(a);
                    LocalDate db = referenceDate(b);
                    if (da == null || db == null) return 0;
                    return db.compareTo(da);
                })
                .toList();

        for (Task t : sorted) {
            addBodyCell(table, t.getTitle() + "\n" + t.getTicketId());
            String typeLine = t.getPriority() + " / " + t.getComplexity()
                    + (t.getTaskTypes() != null && !t.getTaskTypes().isEmpty() ? "\n" + String.join(", ", t.getTaskTypes()) : "");
            addBodyCell(table, typeLine);
            addBodyCell(table, crisp(t.getImpact() != null && !t.getImpact().isBlank() ? t.getImpact() : t.getDescription()));

            StringBuilder links = new StringBuilder();
            if (t.getPrLinks() != null && !t.getPrLinks().isEmpty()) {
                links.append(t.getPrLinks().size()).append(" PR(s)");
            }
            if (t.getDesignDocLink() != null && !t.getDesignDocLink().isBlank()) {
                if (links.length() > 0) links.append("\n");
                links.append("Doc: ").append(t.getDesignDocLink());
            }
            addBodyCell(table, links.toString());
        }
        document.add(table);
    }

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
                p.add(new Chunk(t.getTitle() + "  ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BRAND)));
                p.add(new Chunk("(" + t.getTicketId() + ")", FONT_SMALL_ITALIC));
                document.add(p);
                Paragraph impactP = new Paragraph(crisp(t.getImpact() != null ? t.getImpact() : t.getDescription()), FONT_TABLE_CELL);
                impactP.setSpacingAfter(8f);
                document.add(impactP);
            }
        }

        if (!recognitions.isEmpty()) {
            addSectionHeading(document, "Recognition");
            for (Recognition r : recognitions) {
                Paragraph p = new Paragraph();
                p.add(new Chunk(r.getDate() + " — " + r.getSource() + ": ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BRAND)));
                p.add(new Chunk(r.getMessage(), FONT_TABLE_CELL));
                p.setSpacingAfter(4f);
                document.add(p);
            }
        }
    }

    // ---------------------------------------------------------------------
    // Small helpers
    // ---------------------------------------------------------------------

    private void addSectionHeading(Document document, String text) throws DocumentException {
        Paragraph p = new Paragraph(text, FONT_SECTION);
        p.setSpacingBefore(4f);
        p.setSpacingAfter(8f);
        document.add(p);
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_TABLE_HEADER));
        cell.setBackgroundColor(BRAND);
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, FONT_TABLE_CELL));
        cell.setPadding(5);
        cell.setBackgroundColor(Color.WHITE);
        table.addCell(cell);
    }

    private void addStatCell(PdfPTable table, String number, String label) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(LIGHT_GREY);
        cell.setPadding(12);
        cell.setBorderColor(new Color(0xE0, 0xE0, 0xE0));
        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_CENTER);
        p.add(new Chunk(number + "\n", FONT_STAT_NUMBER));
        p.add(new Chunk(label, FONT_STAT_LABEL));
        cell.addElement(p);
        table.addCell(cell);
    }

    private PdfPCell imageCell(byte[] png) throws BadElementException, IOException {
        Image img = Image.getInstance(png);
        img.scaleToFit(260, 220);
        PdfPCell cell = new PdfPCell(img, false);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    /** Trims verbose AI-generated text down to ~220 chars so the appraisal PDF stays crisp. */
    private String crisp(String text) {
        if (text == null || text.isBlank()) return "-";
        String cleaned = text.strip().replaceAll("\\s+", " ");
        int limit = 220;
        return cleaned.length() > limit ? cleaned.substring(0, limit).trim() + "…" : cleaned;
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

    /** Simple "Page X of Y" footer across the whole document. */
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