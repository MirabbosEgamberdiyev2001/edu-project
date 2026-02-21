package uz.eduplatform.modules.admin.dto;

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
public class AdminUserDto {

    private UUID id;
    private String email;
    private String phone;
    private Boolean emailVerified;
    private Boolean phoneVerified;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private Role role;
    private UserStatus status;
    private String statusReason;
    private Integer failedLoginAttempts;
    private LocalDateTime lockedUntil;
    private LocalDateTime lastLoginAt;
    private String lastLoginIp;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
