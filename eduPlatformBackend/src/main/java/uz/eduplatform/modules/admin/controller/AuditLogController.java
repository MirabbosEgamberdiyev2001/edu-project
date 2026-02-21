package uz.eduplatform.modules.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.eduplatform.core.common.dto.ApiResponse;
import uz.eduplatform.core.common.dto.PagedResponse;
import uz.eduplatform.modules.admin.dto.AuditLogDto;
import uz.eduplatform.modules.admin.service.AuditLogService;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Audit loglar", description = "Tizim audit loglari API'lari — barcha o'zgarishlar va harakatlar tarixi")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @Operation(summary = "Barcha audit loglar", description = "Tizimdagi barcha harakatlar ro'yxatini sahifalab olish — oxirgi yozuvlar birinchi.")
    public ResponseEntity<ApiResponse<PagedResponse<AuditLogDto>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        PagedResponse<AuditLogDto> response = auditLogService.getAuditLogs(
                PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Foydalanuvchi audit loglari", description = "Berilgan foydalanuvchining barcha harakatlari tarixini ko'rish.")
    public ResponseEntity<ApiResponse<PagedResponse<AuditLogDto>>> getAuditLogsByUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        PagedResponse<AuditLogDto> response = auditLogService.getAuditLogsByUserId(
                userId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Kategoriya bo'yicha audit loglar", description = "Berilgan kategoriya (masalan: AUTH, PAYMENT, SUBSCRIPTION) bo'yicha loglarni filtrlash.")
    public ResponseEntity<ApiResponse<PagedResponse<AuditLogDto>>> getAuditLogsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        PagedResponse<AuditLogDto> response = auditLogService.getAuditLogsByCategory(
                category, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Sana oralig'i bo'yicha audit loglar", description = "Berilgan boshlanish va tugash sanasi orasidagi loglarni olish.")
    public ResponseEntity<ApiResponse<PagedResponse<AuditLogDto>>> getAuditLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        PagedResponse<AuditLogDto> response = auditLogService.getAuditLogsByDateRange(
                from, to, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @Operation(summary = "Obyekt bo'yicha audit loglar", description = "Aniq obyekt (masalan: payment, subscription) bo'yicha barcha o'zgarishlar tarixini ko'rish.")
    public ResponseEntity<ApiResponse<PagedResponse<AuditLogDto>>> getAuditLogsByEntity(
            @PathVariable String entityType,
            @PathVariable UUID entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        PagedResponse<AuditLogDto> response = auditLogService.getAuditLogsByEntity(
                entityType, entityId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
