package uz.eduplatform.modules.test.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.eduplatform.modules.content.dto.QuestionDto;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestPreviewResponse {

    private Integer questionCount;
    private Integer variantCount;
    private Map<String, Integer> actualDistribution;
    private List<QuestionDto> questions;
}
