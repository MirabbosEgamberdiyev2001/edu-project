package uz.eduplatform.modules.group.dto;

import lombok.*;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGroupRequest {

    private Map<String, String> nameTranslations;

    private Map<String, String> descriptionTranslations;

    private UUID subjectId;
}
