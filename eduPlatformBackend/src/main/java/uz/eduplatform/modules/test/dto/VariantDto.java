package uz.eduplatform.modules.test.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantDto {

    private String code;
    private List<UUID> questionIds;
    private List<Map<String, Object>> answerKey;
    private List<List<String>> optionsOrder;
}
