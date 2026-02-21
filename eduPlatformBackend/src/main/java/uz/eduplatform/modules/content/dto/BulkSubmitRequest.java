package uz.eduplatform.modules.content.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkSubmitRequest {

    @NotEmpty
    @Size(max = 100)
    private List<UUID> questionIds;
}
