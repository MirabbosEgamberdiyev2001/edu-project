package uz.eduplatform.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequest {

    @NotBlank(message = "{auth.validation.reset.token.required}")
    private String token;

    @NotBlank(message = "{auth.validation.password.required}")
    @Size(min = 6, max = 64, message = "{auth.validation.password.size}")
    private String newPassword;
}
