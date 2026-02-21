package uz.eduplatform.modules.group.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGroupRequest {

    @NotBlank(message = "{group.validation.name.required}")
    @Size(max = 255, message = "{group.validation.name.size}")
    private String name;

    private String description;

    private UUID subjectId;

    private List<UUID> studentIds;
}
