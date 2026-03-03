package uz.eduplatform.modules.assessment.service.export;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.eduplatform.core.common.utils.MessageService;
import uz.eduplatform.modules.assessment.dto.AssignmentResultDto;
import uz.eduplatform.modules.assessment.dto.StudentResultDto;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CsvResultExportService implements ResultExportService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String BOM = "\uFEFF";

    private final MessageService messageService;

    @Override
    public byte[] exportResults(AssignmentResultDto results, Locale locale) {
        StringBuilder sb = new StringBuilder();
        sb.append(BOM);

        // Localized header
        String[] cols = {
                messageService.get("result.export.col.name", locale),
                messageService.get("result.export.col.score", locale),
                messageService.get("result.export.col.maxScore", locale),
                messageService.get("result.export.col.percent", locale),
                messageService.get("result.export.col.attempts", locale),
                messageService.get("result.export.col.tabSwitches", locale),
                messageService.get("result.export.col.status", locale),
                messageService.get("result.export.col.submittedAt", locale),
        };
        sb.append(String.join(",", cols)).append('\n');

        // Data rows
        if (results.getStudents() != null) {
            for (StudentResultDto student : results.getStudents()) {
                String name = (student.getFirstName() != null ? student.getFirstName() : "")
                        + " " + (student.getLastName() != null ? student.getLastName() : "");
                sb.append(escapeCsv(name.trim())).append(',');
                sb.append(student.getScore() != null ? student.getScore().toPlainString() : "").append(',');
                sb.append(student.getMaxScore() != null ? student.getMaxScore().toPlainString() : "").append(',');
                sb.append(student.getPercentage() != null ? student.getPercentage().toPlainString() : "").append(',');
                sb.append(student.getAttemptCount() != null ? student.getAttemptCount() : 0).append(',');
                sb.append(student.getTabSwitches() != null ? student.getTabSwitches() : 0).append(',');
                sb.append(student.getStatus() != null ? student.getStatus() : "").append(',');
                sb.append(student.getSubmittedAt() != null ? student.getSubmittedAt().format(DATE_FORMAT) : "");
                sb.append('\n');
            }
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public ResultExportFormat getFormat() {
        return ResultExportFormat.CSV;
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
