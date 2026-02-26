package uz.eduplatform.modules.assessment.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * Lightweight question representation included in AttemptDto for the exam UI.
 * Correct answers are intentionally excluded from this DTO.
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
}
