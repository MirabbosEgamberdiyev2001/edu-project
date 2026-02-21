package uz.eduplatform.modules.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.eduplatform.modules.assessment.domain.AttemptStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttemptDto {

    private UUID id;
    private UUID assignmentId;
    private String assignmentTitle;
    private UUID studentId;
    private String studentName;
    private Integer attemptNumber;
    private Integer variantIndex;

    // --- Timing ---
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private Long remainingSeconds;

    // --- Scoring ---
    private BigDecimal rawScore;
    private BigDecimal maxScore;
    private BigDecimal percentage;

    // --- Status ---
    private AttemptStatus status;

    // --- Anti-cheat ---
    private Integer tabSwitchCount;
    private String ipAddress;
    private Boolean flagged;
    private String flagReason;

    // --- Answers (included when viewing attempt details) ---
    private List<AnswerDto> answers;

    // --- Progress ---
    private Integer totalQuestions;
    private Integer answeredQuestions;

    private LocalDateTime createdAt;
}
