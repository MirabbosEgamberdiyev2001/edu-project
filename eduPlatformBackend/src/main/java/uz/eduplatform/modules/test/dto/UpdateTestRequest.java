package uz.eduplatform.modules.test.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.eduplatform.modules.test.domain.TestCategory;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTestRequest {

    @Size(max = 255)
    private String title;

    private Map<String, String> titleTranslations;

    private TestCategory category;

    private HeaderConfig headerConfig;
}
