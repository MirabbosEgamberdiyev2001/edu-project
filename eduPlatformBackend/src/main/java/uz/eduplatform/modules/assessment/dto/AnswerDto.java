package uz.eduplatform.modules.assessment.dto;

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
public class AnswerDto {

    private UUID id;
    private UUID questionId;
    private Integer questionIndex;

    // --- Question info (resolved for display) ---
    private String questionText;
    private String questionType;
    private Object options;

    // --- Student's answer ---
    private Object selectedAnswer;

    // --- Grading ---
    private Boolean isCorrect;
    private Boolean isPartial;
    private BigDecimal earnedPoints;
    private BigDecimal maxPoints;

    // --- Manual grading ---
    private Boolean needsManualGrading;
    private BigDecimal manualScore;
    private String manualFeedback;
    private String gradedByName;
    private LocalDateTime gradedAt;

    // --- Correct answer & proof (only shown if assignment settings allow) ---
    private Object correctAnswer;
    private String proof;

    // --- Metadata ---
    private Integer timeSpentSeconds;
    private Boolean bookmarked;
}
