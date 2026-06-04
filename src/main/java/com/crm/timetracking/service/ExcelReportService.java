package com.crm.timetracking.service;

import com.crm.timetracking.entity.Attendance;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExcelReportService {

    private static final ZoneId IL_ZONE = ZoneId.of("Asia/Jerusalem");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public byte[] generateMonthlyReport(
            List<Attendance> records,
            Map<Long, String> userNames,
            String monthLabel) throws IOException {

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(workbook);

            Map<Long, List<Attendance>> byUser = records.stream()
                    .collect(Collectors.groupingBy(Attendance::getUserId));

            // Summary sheet
            Sheet summary = workbook.createSheet("Summary");
            String[] summaryColumns = {"Employee", "Total Sessions", "Total Hours", "Total Minutes"};
            Row summaryHeader = summary.createRow(0);
            for (int i = 0; i < summaryColumns.length; i++) {
                Cell cell = summaryHeader.createCell(i);
                cell.setCellValue(summaryColumns[i]);
                cell.setCellStyle(headerStyle);
            }

            int summaryRowIdx = 1;

            // Per-employee detail sheets
            for (var entry : byUser.entrySet()) {
                Long userId = entry.getKey();
                List<Attendance> userRecords = entry.getValue();
                String name = userNames.getOrDefault(userId, "User #" + userId);

                Sheet sheet = workbook.createSheet(sanitizeSheetName(name));
                String[] columns = {"Date", "Clock In", "Clock Out", "Duration (h:mm)", "Note"};
                Row header = sheet.createRow(0);
                for (int i = 0; i < columns.length; i++) {
                    Cell cell = header.createCell(i);
                    cell.setCellValue(columns[i]);
                    cell.setCellStyle(headerStyle);
                }

                long totalSeconds = 0;
                int rowIdx = 1;
                for (Attendance a : userRecords) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(
                            a.getStartTime().atZoneSameInstant(IL_ZONE).format(DATE_FMT));
                    row.createCell(1).setCellValue(
                            a.getStartTime().atZoneSameInstant(IL_ZONE).format(TIME_FMT));
                    row.createCell(2).setCellValue(
                            a.getEndTime() != null
                                    ? a.getEndTime().atZoneSameInstant(IL_ZONE).format(TIME_FMT)
                                    : "OPEN");
                    if (a.getDurationSeconds() != null) {
                        long hrs  = a.getDurationSeconds() / 3600;
                        long mins = (a.getDurationSeconds() % 3600) / 60;
                        row.createCell(3).setCellValue(String.format("%d:%02d", hrs, mins));
                        totalSeconds += a.getDurationSeconds();
                    } else {
                        row.createCell(3).setCellValue("—");
                    }
                    row.createCell(4).setCellValue(a.getNote() != null ? a.getNote() : "");
                }

                for (int i = 0; i < columns.length; i++) sheet.autoSizeColumn(i);

                Row sRow = summary.createRow(summaryRowIdx++);
                sRow.createCell(0).setCellValue(name);
                sRow.createCell(1).setCellValue(userRecords.size());
                sRow.createCell(2).setCellValue(totalSeconds / 3600.0);
                sRow.createCell(3).setCellValue(totalSeconds / 60.0);
            }

            for (int i = 0; i < summaryColumns.length; i++) summary.autoSizeColumn(i);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private String sanitizeSheetName(String name) {
        String safe = name.replaceAll("[\\[\\]:*?/\\\\]", "_");
        return safe.substring(0, Math.min(safe.length(), 31));
    }
}
