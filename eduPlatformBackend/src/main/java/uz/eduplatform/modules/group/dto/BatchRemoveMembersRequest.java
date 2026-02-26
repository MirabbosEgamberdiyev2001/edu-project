package uz.eduplatform.modules.group.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchRemoveMembersRequest {

    @NotNull(message = "{group.validation.student_ids.required}")
    @Size(min = 1, message = "{group.validation.student_ids.required}")
    private List<UUID> studentIds;
}
