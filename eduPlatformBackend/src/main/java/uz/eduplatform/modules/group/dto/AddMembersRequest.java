package uz.eduplatform.modules.group.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddMembersRequest {

    @NotEmpty(message = "{group.validation.student_ids.required}")
    private List<UUID> studentIds;
}
