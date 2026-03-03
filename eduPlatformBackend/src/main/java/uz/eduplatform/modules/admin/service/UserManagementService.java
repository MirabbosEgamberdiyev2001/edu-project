package uz.eduplatform.modules.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.eduplatform.core.audit.AuditService;
import uz.eduplatform.core.common.dto.PagedResponse;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.core.common.exception.ResourceNotFoundException;
import uz.eduplatform.modules.admin.dto.AdminUserDto;
import uz.eduplatform.modules.admin.dto.ChangeRoleRequest;
import uz.eduplatform.modules.admin.dto.ChangeStatusRequest;
import uz.eduplatform.modules.auth.domain.Role;
import uz.eduplatform.modules.auth.domain.User;
import uz.eduplatform.modules.auth.domain.UserStatus;
import uz.eduplatform.modules.auth.repository.UserRepository;
import uz.eduplatform.modules.auth.repository.UserSessionRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;

    private static final String TEMP_PWD_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional(readOnly = true)
    public PagedResponse<AdminUserDto> getUsers(String search, Role role, UserStatus status, Pageable pageable) {
        Page<User> page;

        if (search != null && !search.isBlank()) {
            page = userRepository.searchUsers(search.trim(), pageable);
        } else if (role != null) {
            page = userRepository.findByRole(role, pageable);
        } else if (status != null) {
            page = userRepository.findByStatus(status, pageable);
        } else {
            page = userRepository.findAll(pageable);
        }

        List<AdminUserDto> dtos = page.getContent().stream()
                .map(this::mapToAdminDto)
                .toList();

        return PagedResponse.of(dtos, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AdminUserDto getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return mapToAdminDto(user);
    }

    @Transactional
    public AdminUserDto changeRole(UUID targetUserId, ChangeRoleRequest request, UUID adminId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", targetUserId));

        if (user.getRole() == Role.SUPER_ADMIN) {
            throw BusinessException.ofKey("admin.superadmin.role.protected");
        }

        Role oldRole = user.getRole();
        user.setRole(request.getRole());
        userRepository.save(user);

        auditService.log(adminId, "ADMIN", "USER_ROLE_CHANGED", "USER_MANAGEMENT",
                "User", targetUserId,
                Map.of("oldRole", oldRole.name()),
                Map.of("newRole", request.getRole().name()));

        return mapToAdminDto(user);
    }

    @Transactional
    public AdminUserDto changeStatus(UUID targetUserId, ChangeStatusRequest request, UUID adminId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", targetUserId));

        if (user.getRole() == Role.SUPER_ADMIN) {
            throw BusinessException.ofKey("admin.superadmin.status.protected");
        }

        UserStatus oldStatus = user.getStatus();
        user.setStatus(request.getStatus());
        user.setStatusReason(request.getReason());

        if (request.getStatus() == UserStatus.BLOCKED) {
            // Invalidate all sessions
            userSessionRepository.deactivateAllByUserId(targetUserId);
        }

        if (request.getStatus() == UserStatus.ACTIVE) {
            user.resetFailedAttempts();
        }

        userRepository.save(user);

        auditService.log(adminId, "ADMIN", "USER_STATUS_CHANGED", "USER_MANAGEMENT",
                "User", targetUserId,
                Map.of("oldStatus", oldStatus.name()),
                Map.of("newStatus", request.getStatus().name(),
                        "reason", request.getReason() != null ? request.getReason() : ""));

        return mapToAdminDto(user);
    }

    @Transactional
    public void deleteUser(UUID targetUserId, UUID adminId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", targetUserId));

        if (user.getRole() == Role.SUPER_ADMIN) {
            throw BusinessException.ofKey("admin.superadmin.delete.protected");
        }

        user.setDeletedAt(LocalDateTime.now());
        user.setStatus(UserStatus.INACTIVE);
        userSessionRepository.deactivateAllByUserId(targetUserId);
        userRepository.save(user);

        auditService.log(adminId, "ADMIN", "USER_DELETED", "USER_MANAGEMENT",
                "User", targetUserId);
    }

    @Transactional
    public AdminUserDto unlockUser(UUID targetUserId, UUID adminId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", targetUserId));

        user.resetFailedAttempts();
        userRepository.save(user);

        auditService.log(adminId, "ADMIN", "USER_UNLOCKED", "USER_MANAGEMENT",
                "User", targetUserId);

        return mapToAdminDto(user);
    }

    @Transactional
    public String resetPassword(UUID targetUserId, UUID adminId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", targetUserId));

        if (user.getRole() == Role.SUPER_ADMIN) {
            throw BusinessException.ofKey("admin.superadmin.role.protected");
        }

        // Generate a random 12-char temporary password
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(TEMP_PWD_CHARS.charAt(RANDOM.nextInt(TEMP_PWD_CHARS.length())));
        }
        String tempPassword = sb.toString();

        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        // Invalidate all existing sessions so the user must log in again with new password
        userSessionRepository.deactivateAllByUserId(targetUserId);
        userRepository.save(user);

        auditService.log(adminId, "ADMIN", "USER_PASSWORD_RESET", "USER_MANAGEMENT",
                "User", targetUserId);

        return tempPassword;
    }

    private AdminUserDto mapToAdminDto(User user) {
        return AdminUserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .emailVerified(user.getEmailVerified())
                .phoneVerified(user.getPhoneVerified())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .status(user.getStatus())
                .statusReason(user.getStatusReason())
                .failedLoginAttempts(user.getFailedLoginAttempts())
                .lockedUntil(user.getLockedUntil())
                .lastLoginAt(user.getLastLoginAt())
                .lastLoginIp(user.getLastLoginIp())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
