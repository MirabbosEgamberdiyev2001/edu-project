package uz.eduplatform.modules.content.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.eduplatform.modules.content.domain.Difficulty;
import uz.eduplatform.modules.content.domain.QuestionType;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateQuestionRequest {

    @NotNull(message = "{content.validation.topic.id.required}")
    private UUID topicId;

    @NotNull(message = "{content.validation.question.text.required}")
    private Map<String, String> questionText;

    @NotNull(message = "{content.validation.question.type.required}")
    private QuestionType questionType;

    private Difficulty difficulty;

    private BigDecimal points;

    private Integer timeLimitSeconds;

    private Map<String, Object> media;

    private Object options;

    private Object correctAnswer;

    private Map<String, String> proof;
}
