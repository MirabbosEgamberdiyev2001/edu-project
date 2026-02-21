package uz.eduplatform.modules.content.dto;

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
public class BulkModerationResponse {

    private int totalRequested;
    private int successCount;
    private int failedCount;
    private List<UUID> failedIds;
    private List<String> errors;
}
