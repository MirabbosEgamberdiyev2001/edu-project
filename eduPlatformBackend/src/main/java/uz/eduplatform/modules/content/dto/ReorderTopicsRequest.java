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
public class ReorderTopicsRequest {

    @NotEmpty(message = "Topic order list cannot be empty")
    private List<TopicOrderItem> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopicOrderItem {
        private UUID id;
        private Integer sortOrder;
    }
}
