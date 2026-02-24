package uz.eduplatform.modules.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.eduplatform.modules.assessment.domain.ExportJobStatus;
import uz.eduplatform.modules.assessment.service.export.ResultExportFormat;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportJobDto {

    private UUID id;
    private UUID assignmentId;
    private ExportJobStatus status;
    private ResultExportFormat format;
    private String fileName;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
