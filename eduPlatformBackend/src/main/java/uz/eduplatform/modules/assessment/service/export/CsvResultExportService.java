package uz.eduplatform.modules.assessment.service.export;

import org.springframework.stereotype.Service;
import uz.eduplatform.modules.assessment.dto.AssignmentResultDto;
import uz.eduplatform.modules.assessment.dto.StudentResultDto;

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
        sb.append("Student Name,Score,Max Score,%,Attempts,Tab Switches,Status,Submitted At\n");

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
