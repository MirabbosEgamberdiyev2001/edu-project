package uz.eduplatform.modules.assessment.service.export;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.modules.assessment.dto.AssignmentResultDto;
import uz.eduplatform.modules.assessment.dto.AttemptDto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Service
public class ExcelResultExportService implements ResultExportService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public byte[] exportResults(AssignmentResultDto results, Locale locale) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // ── Sheet 1: Summary ──
            Sheet summarySheet = workbook.createSheet("Summary");
            CellStyle headerStyle = createHeaderStyle(workbook);

            int rowIdx = 0;
            Row titleRow = summarySheet.createRow(rowIdx++);
            titleRow.createCell(0).setCellValue("Assignment Results: " + results.getAssignmentTitle());
            titleRow.getCell(0).setCellStyle(headerStyle);

            rowIdx++;
            addSummaryRow(summarySheet, rowIdx++, "Total Assigned", String.valueOf(results.getTotalAssigned()));
            addSummaryRow(summarySheet, rowIdx++, "Total Started", String.valueOf(results.getTotalStarted()));
            addSummaryRow(summarySheet, rowIdx++, "Total Submitted", String.valueOf(results.getTotalSubmitted()));
            addSummaryRow(summarySheet, rowIdx++, "Total Graded", String.valueOf(results.getTotalGraded()));
            rowIdx++;
            addSummaryRow(summarySheet, rowIdx++, "Average Score",
                    results.getAverageScore() != null ? results.getAverageScore().toPlainString() + "%" : "N/A");
            addSummaryRow(summarySheet, rowIdx++, "Highest Score",
                    results.getHighestScore() != null ? results.getHighestScore().toPlainString() + "%" : "N/A");
            addSummaryRow(summarySheet, rowIdx, "Lowest Score",
                    results.getLowestScore() != null ? results.getLowestScore().toPlainString() + "%" : "N/A");

            summarySheet.autoSizeColumn(0);
            summarySheet.autoSizeColumn(1);

            // ── Sheet 2: Results ──
            Sheet resultsSheet = workbook.createSheet("Results");
            String[] headers = {"Student Name", "Attempt #", "Status", "Score", "Max Score", "%",
                    "Tab Switches", "IP Address", "Flagged", "Started At", "Submitted At"};

            Row headerRow = resultsSheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            if (results.getAttempts() != null) {
                int dataRow = 1;
                for (AttemptDto attempt : results.getAttempts()) {
                    Row row = resultsSheet.createRow(dataRow++);
                    int col = 0;
                    row.createCell(col++).setCellValue(attempt.getStudentName() != null ? attempt.getStudentName() : "");
                    row.createCell(col++).setCellValue(attempt.getAttemptNumber() != null ? attempt.getAttemptNumber() : 0);
                    row.createCell(col++).setCellValue(attempt.getStatus() != null ? attempt.getStatus().name() : "");
                    if (attempt.getRawScore() != null) {
                        row.createCell(col++).setCellValue(attempt.getRawScore().doubleValue());
                    } else {
                        row.createCell(col++).setCellValue("");
                    }
                    if (attempt.getMaxScore() != null) {
                        row.createCell(col++).setCellValue(attempt.getMaxScore().doubleValue());
                    } else {
                        row.createCell(col++).setCellValue("");
                    }
                    if (attempt.getPercentage() != null) {
                        row.createCell(col++).setCellValue(attempt.getPercentage().doubleValue());
                    } else {
                        row.createCell(col++).setCellValue("");
                    }
                    row.createCell(col++).setCellValue(attempt.getTabSwitchCount() != null ? attempt.getTabSwitchCount() : 0);
                    row.createCell(col++).setCellValue(attempt.getIpAddress() != null ? attempt.getIpAddress() : "");
                    row.createCell(col++).setCellValue(Boolean.TRUE.equals(attempt.getFlagged()) ? "Yes" : "No");
                    row.createCell(col++).setCellValue(attempt.getStartedAt() != null ? attempt.getStartedAt().format(DATE_FORMAT) : "");
                    row.createCell(col).setCellValue(attempt.getSubmittedAt() != null ? attempt.getSubmittedAt().format(DATE_FORMAT) : "");
                }
            }

            for (int i = 0; i < headers.length; i++) {
                resultsSheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            log.error("Failed to generate Excel export", e);
            throw BusinessException.ofKey("result.export.excel.failed");
        }
    }

    @Override
    public ResultExportFormat getFormat() {
        return ResultExportFormat.EXCEL;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private void addSummaryRow(Sheet sheet, int rowIdx, String label, String value) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
    }
}
