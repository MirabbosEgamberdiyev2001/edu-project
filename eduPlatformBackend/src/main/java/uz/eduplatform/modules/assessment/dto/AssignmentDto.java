package uz.eduplatform.modules.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.eduplatform.modules.assessment.domain.AssignmentStatus;

import java.time.LocalDateTime;
import java.util.List;
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

    private String title;
    private String description;

    // --- Scheduling ---
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMinutes;

    // --- Settings ---
    private Integer maxAttempts;
    private Boolean showResults;
    private Boolean showCorrectAnswers;
    private Boolean showProofs;
    private Boolean shufflePerStudent;
    private Boolean preventCopyPaste;
    private Boolean preventTabSwitch;
    private Integer tabSwitchThreshold;
    private String tabSwitchAction;
    private String accessCode;

    // --- Students ---
    private List<UUID> assignedStudentIds;
    private Integer totalStudents;
    private Integer startedCount;
    private Integer submittedCount;

    // --- Status ---
    private AssignmentStatus status;

    // --- Audit ---
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
