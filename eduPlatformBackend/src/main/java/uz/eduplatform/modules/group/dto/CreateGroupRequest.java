package uz.eduplatform.modules.group.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGroupRequest {

    @NotEmpty(message = "{group.validation.name.required}")
    private Map<String, String> nameTranslations;

    private Map<String, String> descriptionTranslations;

    private UUID subjectId;

    private List<UUID> studentIds;
}
