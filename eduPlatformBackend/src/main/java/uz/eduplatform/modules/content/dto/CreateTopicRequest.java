package uz.eduplatform.modules.content.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTopicRequest {

    @NotNull(message = "Topic name is required")
    private Map<String, String> name;

    private Map<String, String> description;

    private UUID parentId;

    @NotNull(message = "Grade level is required")
    @Min(1)
    @Max(11)
    private Integer gradeLevel;
}
