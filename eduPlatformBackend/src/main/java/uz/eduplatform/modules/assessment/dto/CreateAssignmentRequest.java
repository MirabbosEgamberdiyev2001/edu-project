package uz.eduplatform.modules.assessment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateAssignmentRequest {

    @NotNull(message = "{assessment.validation.test_history_id.required}")
    private UUID testHistoryId;

    private String title;

    private Map<String, String> titleTranslations;

    private String description;

    private Map<String, String> descriptionTranslations;

    // --- Scheduling ---
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @Builder.Default
    @Min(value = 1, message = "{assessment.validation.duration.min}")
    @Max(value = 480, message = "{assessment.validation.duration.max}")
    private Integer durationMinutes = 45;

    // --- Attempt settings ---
    @Builder.Default
    @Min(value = 1, message = "{assessment.validation.max_attempts.min}")
    @Max(value = 10, message = "{assessment.validation.max_attempts.max}")
    private Integer maxAttempts = 1;

    // --- Result visibility ---
    @Builder.Default
    private Boolean showResults = true;

    @Builder.Default
    private Boolean showCorrectAnswers = false;

    @Builder.Default
    private Boolean showProofs = false;

    // --- Shuffling ---
    @Builder.Default
    private Boolean shuffleQuestions = true;

    @Builder.Default
    private Boolean shuffleOptions = false;

    // --- Anti-cheat ---
    @Builder.Default
    private Boolean preventCopyPaste = true;

    @Builder.Default
    private Boolean preventTabSwitch = false;

    @Builder.Default
    private Integer tabSwitchThreshold = 0;

    @Builder.Default
    private String tabSwitchAction = "WARN";

    // --- Access control ---
    private String accessCode;

    // --- Assigned students ---
    private List<UUID> assignedStudentIds;

    // --- Group assignment (resolves to student IDs) ---
    private UUID groupId;
}
