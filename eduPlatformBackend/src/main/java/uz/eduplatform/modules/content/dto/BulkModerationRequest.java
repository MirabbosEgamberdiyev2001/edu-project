package uz.eduplatform.modules.content.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkModerationRequest {

    @NotEmpty(message = "Question IDs list cannot be empty")
    private List<UUID> questionIds;

    private String reason;
}
