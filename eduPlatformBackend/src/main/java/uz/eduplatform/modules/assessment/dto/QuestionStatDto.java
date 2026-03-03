package uz.eduplatform.modules.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionStatDto {

    private UUID questionId;
    private Integer questionNumber;
    private String questionText;
    private Integer totalAnswered;
    private Integer correctCount;
    /** Correct answer rate: 0–100 (%) */
    private BigDecimal correctRate;
}
