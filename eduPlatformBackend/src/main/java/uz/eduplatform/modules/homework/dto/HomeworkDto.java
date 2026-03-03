package uz.eduplatform.modules.homework.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeworkDto {
    private UUID id;
    private UUID teacherId;
    private String teacherName;
    private UUID groupId;
    private String groupName;
    private String title;
    private String description;
    private LocalDateTime deadline;
    private Boolean allowResubmission;
    private Boolean fileRequired;
    private Integer maxFileSizeMb;
    private String status;
    private LocalDateTime createdAt;
    // Submission stats (for teacher view)
    private Long totalSubmissions;
    private Long gradedSubmissions;
    // Student's own submission (for student view)
    private HomeworkSubmissionDto mySubmission;
}
