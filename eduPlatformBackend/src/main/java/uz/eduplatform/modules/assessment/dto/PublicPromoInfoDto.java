package uz.eduplatform.modules.assessment.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PublicPromoInfoDto {

    private UUID assignmentId;
    private String assignmentTitle;
    private String testTitle;
    private Integer questionCount;
    private Integer durationMinutes;
    private String teacherFullName;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private Integer maxAttempts;
    private Boolean showResults;
}
