package uz.eduplatform.modules.test.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DifficultyDistribution {

    @Min(0) @Max(100)
    @Builder.Default
    private Integer easy = 30;

    @Min(0) @Max(100)
    @Builder.Default
    private Integer medium = 50;

    @Min(0) @Max(100)
    @Builder.Default
    private Integer hard = 20;
}
