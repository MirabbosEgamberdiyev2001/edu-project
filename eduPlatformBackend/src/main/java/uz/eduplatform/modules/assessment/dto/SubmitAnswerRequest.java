package uz.eduplatform.modules.assessment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAnswerRequest {

    @NotNull(message = "{assessment.validation.question_id.required}")
    private UUID questionId;

    @NotNull(message = "{assessment.validation.question_index.required}")
    private Integer questionIndex;

    // JSONB-compatible answer: could be a string, list, map depending on question type
    private Object selectedAnswer;

    private Integer timeSpentSeconds;

    private Boolean bookmarked;
}
