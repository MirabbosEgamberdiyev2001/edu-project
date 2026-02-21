package uz.eduplatform.modules.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.eduplatform.modules.auth.domain.UserStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeStatusRequest {

    @NotNull(message = "Status is required")
    private UserStatus status;

    private String reason;
}
