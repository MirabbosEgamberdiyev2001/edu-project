package uz.eduplatform.modules.assessment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
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
public class GradeAnswerRequest {

    @NotNull(message = "{assessment.validation.answer_id.required}")
    private UUID answerId;

    @NotNull(message = "{assessment.validation.score.required}")
    @DecimalMin(value = "0.0", message = "{assessment.validation.score.min}")
    private BigDecimal score;

    private String feedback;
}
