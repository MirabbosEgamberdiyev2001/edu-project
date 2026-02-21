package uz.eduplatform.modules.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uz.eduplatform.core.common.dto.ApiResponse;
import uz.eduplatform.core.common.dto.PagedResponse;
import uz.eduplatform.core.common.utils.MessageService;
import uz.eduplatform.core.i18n.AcceptLanguage;
import uz.eduplatform.core.security.UserPrincipal;
import uz.eduplatform.modules.admin.dto.AdminUserDto;
import uz.eduplatform.modules.admin.dto.ChangeRoleRequest;
import uz.eduplatform.modules.admin.dto.ChangeStatusRequest;
import uz.eduplatform.modules.admin.service.UserManagementService;
import uz.eduplatform.modules.auth.domain.Role;
import uz.eduplatform.modules.auth.domain.UserStatus;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Foydalanuvchilarni boshqarish", description = "Admin foydalanuvchilarni boshqarish API'lari — ro'yxat, rol o'zgartirish, bloklash")
public class UserManagementController {

    private final UserManagementService userManagementService;
    private final MessageService messageService;

    @GetMapping
    @Operation(summary = "Foydalanuvchilar ro'yxati", description = "Barcha foydalanuvchilarni filtrlash — qidiruv, rol va holat bo'yicha. Sahifalash qo'llab-quvvatlanadi.")
    public ResponseEntity<ApiResponse<PagedResponse<AdminUserDto>>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PagedResponse<AdminUserDto> response = userManagementService.getUsers(
                search, role, status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Foydalanuvchi tafsilotlari", description = "Berilgan ID bo'yicha foydalanuvchining to'liq ma'lumotlarini olish.")
    public ResponseEntity<ApiResponse<AdminUserDto>> getUserById(@PathVariable UUID id) {
        AdminUserDto user = userManagementService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PutMapping("/{id}/role")
    @Operation(summary = "Foydalanuvchi rolini o'zgartirish", description = "Foydalanuvchiga yangi rol tayinlash (STUDENT, TEACHER, MODERATOR, ADMIN). Audit logga yoziladi.")
    public ResponseEntity<ApiResponse<AdminUserDto>> changeRole(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeRoleRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        AdminUserDto user = userManagementService.changeRole(id, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(user, messageService.get("admin.user.role.updated")));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Foydalanuvchi holatini o'zgartirish", description = "Foydalanuvchini bloklash yoki blokdan chiqarish. Bloklangan foydalanuvchi tizimga kira olmaydi.")
    public ResponseEntity<ApiResponse<AdminUserDto>> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        AdminUserDto user = userManagementService.changeStatus(id, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(user, messageService.get("admin.user.status.updated")));
    }

    @PostMapping("/{id}/unlock")
    @Operation(summary = "Qulflangan hisobni ochish", description = "Ko'p marta noto'g'ri parol kiritish tufayli qulflangan hisobni ochish.")
    public ResponseEntity<ApiResponse<AdminUserDto>> unlockUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        AdminUserDto user = userManagementService.unlockUser(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(user, messageService.get("admin.user.unlocked")));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Foydalanuvchini o'chirish", description = "Foydalanuvchini yumshoq o'chirish. Faqat SUPER_ADMIN roli. Audit logga yoziladi.")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        userManagementService.deleteUser(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, messageService.get("admin.user.deleted")));
    }
}
