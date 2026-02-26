package uz.eduplatform.modules.assessment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAssignmentRequest {

    private String title;
    private Map<String, String> titleTranslations;
    private String description;
    private Map<String, String> descriptionTranslations;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @Min(value = 1, message = "{assessment.validation.duration.min}")
    @Max(value = 480, message = "{assessment.validation.duration.max}")
    private Integer durationMinutes;

    @Min(value = 1, message = "{assessment.validation.max_attempts.min}")
    @Max(value = 10, message = "{assessment.validation.max_attempts.max}")
    private Integer maxAttempts;

    private Boolean showResults;
    private Boolean showCorrectAnswers;
    private Boolean showProofs;
    private Boolean shuffleQuestions;
    private Boolean shuffleOptions;
    private Boolean preventCopyPaste;
    private Boolean preventTabSwitch;
    private Integer tabSwitchThreshold;
    private String tabSwitchAction;

    private String accessCode;
    private List<UUID> assignedStudentIds;
}
