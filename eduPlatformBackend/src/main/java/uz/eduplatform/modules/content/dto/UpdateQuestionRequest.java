package uz.eduplatform.modules.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.eduplatform.modules.content.domain.Difficulty;
import uz.eduplatform.modules.content.domain.QuestionType;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateQuestionRequest {

    private Map<String, String> questionText;

    private QuestionType questionType;
    private Difficulty difficulty;
    private BigDecimal points;
    private Integer timeLimitSeconds;

    private Map<String, Object> media;
    private Object options;
    private Object correctAnswer;

    private Map<String, String> proof;
    private String changeReason;
}
