package uz.eduplatform.modules.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleAuthRequest {
    private String idToken;
    private String accessToken;
    /** Optional: STUDENT | TEACHER | PARENT. Used only when creating a brand-new user. */
    private String role;
}
