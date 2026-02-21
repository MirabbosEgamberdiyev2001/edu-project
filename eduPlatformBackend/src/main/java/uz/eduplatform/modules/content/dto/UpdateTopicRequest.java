package uz.eduplatform.modules.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTopicRequest {

    private Map<String, String> name;

    private Map<String, String> description;

    private Boolean isActive;
}
