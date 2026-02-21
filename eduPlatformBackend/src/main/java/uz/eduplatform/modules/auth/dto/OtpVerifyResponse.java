package uz.eduplatform.modules.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OtpVerifyResponse {

    private boolean verified;
    private String resetToken;
    private UserDto registeredUser;

    // Auto-login tokens returned after registration OTP verification
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
}