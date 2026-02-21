package uz.eduplatform.modules.group.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGroupRequest {

    @Size(max = 255, message = "{group.validation.name.size}")
    private String name;

    private String description;

    private UUID subjectId;
}
