package uz.eduplatform.modules.content.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkCreateTopicRequest {

    @NotEmpty(message = "Items list must not be empty")
    @Valid
    private List<CreateTopicRequest> items;

    @Builder.Default
    private boolean skipDuplicates = false;
}
