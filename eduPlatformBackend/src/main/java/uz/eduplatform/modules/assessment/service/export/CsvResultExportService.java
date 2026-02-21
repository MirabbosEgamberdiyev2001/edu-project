package uz.eduplatform.modules.assessment.service.export;

import org.springframework.stereotype.Service;
import uz.eduplatform.modules.assessment.dto.AssignmentResultDto;
import uz.eduplatform.modules.assessment.dto.AttemptDto;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class CsvResultExportService implements ResultExportService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String BOM = "\uFEFF";

    @Override
    public byte[] exportResults(AssignmentResultDto results, Locale locale) {
        StringBuilder sb = new StringBuilder();
        sb.append(BOM);

        // Header
        sb.append("Student Name,Attempt #,Status,Score,Max Score,%,Tab Switches,IP Address,Flagged,Started At,Submitted At\n");

        // Data rows
        if (results.getAttempts() != null) {
            for (AttemptDto attempt : results.getAttempts()) {
                sb.append(escapeCsv(attempt.getStudentName())).append(',');
                sb.append(attempt.getAttemptNumber() != null ? attempt.getAttemptNumber() : "").append(',');
                sb.append(attempt.getStatus() != null ? attempt.getStatus().name() : "").append(',');
                sb.append(attempt.getRawScore() != null ? attempt.getRawScore().toPlainString() : "").append(',');
                sb.append(attempt.getMaxScore() != null ? attempt.getMaxScore().toPlainString() : "").append(',');
                sb.append(attempt.getPercentage() != null ? attempt.getPercentage().toPlainString() : "").append(',');
                sb.append(attempt.getTabSwitchCount() != null ? attempt.getTabSwitchCount() : 0).append(',');
                sb.append(attempt.getIpAddress() != null ? attempt.getIpAddress() : "").append(',');
                sb.append(Boolean.TRUE.equals(attempt.getFlagged()) ? "Yes" : "No").append(',');
                sb.append(attempt.getStartedAt() != null ? attempt.getStartedAt().format(DATE_FORMAT) : "").append(',');
                sb.append(attempt.getSubmittedAt() != null ? attempt.getSubmittedAt().format(DATE_FORMAT) : "");
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
