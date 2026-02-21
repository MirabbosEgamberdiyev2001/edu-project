package uz.eduplatform.modules.assessment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchSaveAnswerRequest {

    @NotEmpty(message = "At least one answer is required")
    @Valid
    private List<SubmitAnswerRequest> answers;
}
