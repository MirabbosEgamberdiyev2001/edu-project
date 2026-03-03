package uz.eduplatform.modules.homework.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeworkSubmissionDto {
    private UUID id;
    private UUID homeworkId;
    private UUID studentId;
    private String studentName;
    private String textAnswer;
    private String fileUrl;
    private String fileName;
    private LocalDateTime submittedAt;
    private BigDecimal grade;
    private String teacherComment;
    private LocalDateTime gradedAt;
    private Integer attemptNumber;
    private String status;
}
