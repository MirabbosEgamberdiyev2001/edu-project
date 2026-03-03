package uz.eduplatform.modules.assessment.service.export;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.core.common.utils.MessageService;
import uz.eduplatform.modules.assessment.dto.AssignmentResultDto;
import uz.eduplatform.modules.assessment.dto.StudentResultDto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelResultExportService implements ResultExportService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MessageService messageService;

    @Override
    public byte[] exportResults(AssignmentResultDto results, Locale locale) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // ── Sheet 1: Summary ──
            String summarySheetName = messageService.get("result.export.sheet.summary", locale);
            Sheet summarySheet = workbook.createSheet(summarySheetName);
            CellStyle headerStyle = createHeaderStyle(workbook);

            int rowIdx = 0;
            Row titleRow = summarySheet.createRow(rowIdx++);
            String titlePrefix = messageService.get("result.export.title", locale);
            titleRow.createCell(0).setCellValue(titlePrefix + " " + results.getAssignmentTitle());
            titleRow.getCell(0).setCellStyle(headerStyle);

            rowIdx++;
            addSummaryRow(summarySheet, rowIdx++,
                    messageService.get("result.export.label.total", locale),
                    String.valueOf(results.getTotalStudents()));
            addSummaryRow(summarySheet, rowIdx++,
                    messageService.get("result.export.label.completed", locale),
                    String.valueOf(results.getCompletedStudents()));
            rowIdx++;
            addSummaryRow(summarySheet, rowIdx++,
                    messageService.get("result.export.label.average", locale),
                    results.getAverageScore() != null ? results.getAverageScore().toPlainString() + "%" : "N/A");
            addSummaryRow(summarySheet, rowIdx++,
                    messageService.get("result.export.label.highest", locale),
                    results.getHighestScore() != null ? results.getHighestScore().toPlainString() + "%" : "N/A");
            addSummaryRow(summarySheet, rowIdx,
                    messageService.get("result.export.label.lowest", locale),
                    results.getLowestScore() != null ? results.getLowestScore().toPlainString() + "%" : "N/A");

            summarySheet.autoSizeColumn(0);
            summarySheet.autoSizeColumn(1);

            // ── Sheet 2: Results ──
            String resultsSheetName = messageService.get("result.export.sheet.results", locale);
            Sheet resultsSheet = workbook.createSheet(resultsSheetName);
            String[] headers = {
                    messageService.get("result.export.col.name", locale),
                    messageService.get("result.export.col.score", locale),
                    messageService.get("result.export.col.maxScore", locale),
                    messageService.get("result.export.col.percent", locale),
                    messageService.get("result.export.col.attempts", locale),
                    messageService.get("result.export.col.tabSwitches", locale),
                    messageService.get("result.export.col.status", locale),
                    messageService.get("result.export.col.submittedAt", locale),
            };

            Row headerRow = resultsSheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            if (results.getStudents() != null) {
                int dataRow = 1;
                for (StudentResultDto student : results.getStudents()) {
                    Row row = resultsSheet.createRow(dataRow++);
                    int col = 0;
                    String name = (student.getFirstName() != null ? student.getFirstName() : "")
                            + " " + (student.getLastName() != null ? student.getLastName() : "");
                    row.createCell(col++).setCellValue(name.trim());
                    if (student.getScore() != null) {
                        row.createCell(col++).setCellValue(student.getScore().doubleValue());
                    } else {
                        row.createCell(col++).setCellValue("");
                    }
                    if (student.getMaxScore() != null) {
                        row.createCell(col++).setCellValue(student.getMaxScore().doubleValue());
                    } else {
                        row.createCell(col++).setCellValue("");
                    }
                    if (student.getPercentage() != null) {
                        row.createCell(col++).setCellValue(student.getPercentage().doubleValue());
                    } else {
                        row.createCell(col++).setCellValue("");
                    }
                    row.createCell(col++).setCellValue(student.getAttemptCount() != null ? student.getAttemptCount() : 0);
                    row.createCell(col++).setCellValue(student.getTabSwitches() != null ? student.getTabSwitches() : 0);
                    row.createCell(col++).setCellValue(student.getStatus() != null ? student.getStatus() : "");
                    row.createCell(col).setCellValue(student.getSubmittedAt() != null ? student.getSubmittedAt().format(DATE_FORMAT) : "");
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
