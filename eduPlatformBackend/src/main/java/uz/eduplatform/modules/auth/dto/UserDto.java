package uz.eduplatform.modules.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.eduplatform.modules.auth.domain.Role;
import uz.eduplatform.modules.auth.domain.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private UUID id;
    private String email;
    private String phone;
    private Boolean emailVerified;
    private Boolean phoneVerified;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String locale;
    private String timezone;
    private Role role;
    private UserStatus status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}
