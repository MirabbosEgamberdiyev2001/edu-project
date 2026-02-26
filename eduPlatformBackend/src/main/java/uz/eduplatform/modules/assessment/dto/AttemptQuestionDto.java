package uz.eduplatform.modules.assessment.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * Question representation included in AttemptDto.
 * During the exam (IN_PROGRESS): correctAnswer and proof are null for security.
 * After submission (SUBMITTED/GRADED): correctAnswer and proof are populated
 * only when the assignment's showCorrectAnswers / showProofs flags are true.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttemptQuestionDto {

    private UUID id;

    private String questionText;    // extracted from multilingual map

    private String questionType;    // QuestionType enum name

    private String difficulty;      // Difficulty enum name

    private double points;

    private Integer timeLimitSeconds;

    private Object media;           // JSONB media attachments

    private Object options;         // raw options (MCQ choices, matching pairs, etc.)

    private List<String> optionsOrder; // variant-specific option order (for shuffled MCQ)

    // Populated only for submitted/graded attempts when showCorrectAnswers = true
    private Object correctAnswer;

    // Populated only for submitted/graded attempts when showProofs = true (resolved to current language)
    private String proof;
}
