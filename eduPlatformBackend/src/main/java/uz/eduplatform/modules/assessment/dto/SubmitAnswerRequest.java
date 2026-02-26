package uz.eduplatform.modules.assessment.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
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

    // Optional — used for ordering; frontend may omit it
    private Integer questionIndex;

    // JSONB-compatible answer: could be a string, list, map depending on question type.
    // Accepts both "selectedAnswer" (canonical) and "response" (frontend alias).
    @JsonAlias("response")
    private Object selectedAnswer;

    private Integer timeSpentSeconds;

    private Boolean bookmarked;
}
