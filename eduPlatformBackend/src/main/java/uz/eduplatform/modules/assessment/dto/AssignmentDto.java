package uz.eduplatform.modules.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.eduplatform.modules.assessment.domain.AssignmentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentDto {

    private UUID id;
    private UUID testHistoryId;
    private UUID teacherId;
    private String teacherName;

    // --- Group & Test info ---
    private UUID groupId;
    private String groupName;
    private String testTitle;

    private String title;
    private Map<String, String> titleTranslations;
    private String description;
    private Map<String, String> descriptionTranslations;

    // --- Scheduling ---
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer durationMinutes;

    // --- Settings ---
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

    // --- Students ---
    private List<UUID> assignedStudentIds;
    private Integer totalStudents;
    private Integer activeStudents;
    private Integer completedStudents;
    private BigDecimal averageScore;

    // --- Status ---
    private AssignmentStatus status;

    // --- Audit ---
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
