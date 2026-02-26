package uz.eduplatform.modules.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.eduplatform.modules.assessment.domain.AttemptStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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

    // --- Questions (populated for IN_PROGRESS attempts, for exam display) ---
    private List<AttemptQuestionDto> questions;

    // --- Answers (Map<questionId, AnswerDto> for O(1) lookup by frontend) ---
    private Map<UUID, AnswerDto> answers;

    // --- Progress ---
    private Integer totalQuestions;
    private Integer answeredQuestions;

    private LocalDateTime createdAt;

    // ── Frontend-expected alias fields ────────────────────────────────────────

    /** Assignment title — matches frontend AttemptDto.testTitle */
    private String testTitle;

    /** Tab switch count — matches frontend AttemptDto.tabSwitches */
    private Integer tabSwitches;

    /** Raw score as double — matches frontend AttemptDto.score */
    private Double score;

    /** Remaining seconds alias — matches frontend AttemptDto.timeRemaining */
    private Long timeRemaining;

    /** Assignment duration — matches frontend AttemptDto.durationMinutes */
    private Integer durationMinutes;
}
