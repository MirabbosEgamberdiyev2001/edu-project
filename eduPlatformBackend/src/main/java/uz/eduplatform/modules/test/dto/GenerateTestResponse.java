package uz.eduplatform.modules.test.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateTestResponse {

    private UUID testId;
    private String title;
    private Map<String, String> titleTranslations;
    private Integer questionCount;
    private Integer variantCount;
    private Map<String, Integer> difficultyDistribution;
    private Long randomSeed;
    private List<VariantDto> variants;
    private LocalDateTime createdAt;
}
