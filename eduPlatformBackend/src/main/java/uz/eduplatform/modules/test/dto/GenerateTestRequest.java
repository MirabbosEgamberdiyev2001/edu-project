package uz.eduplatform.modules.test.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateTestRequest {

    @NotBlank(message = "{test.validation.title.required}")
    @Size(max = 255, message = "{test.validation.title.size}")
    private String title;

    @NotNull(message = "{test.validation.subject.id.required}")
    private UUID subjectId;

    @NotEmpty(message = "{test.validation.topic.ids.required}")
    private List<UUID> topicIds;

    @Min(value = 1, message = "{test.validation.question.count.min}")
    @Max(value = 100, message = "{test.validation.question.count.max}")
    private Integer questionCount;

    private List<UUID> questionIds;

    @Builder.Default
    @Min(value = 1, message = "{test.validation.variant.count.min}")
    @Max(value = 10, message = "{test.validation.variant.count.max}")
    private Integer variantCount = 1;

    private DifficultyDistribution difficultyDistribution;

    @Builder.Default
    private Boolean shuffleQuestions = true;

    @Builder.Default
    private Boolean shuffleOptions = true;

    private Long randomSeed;

    private HeaderConfig headerConfig;
}
