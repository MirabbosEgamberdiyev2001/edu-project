package uz.eduplatform.modules.assessment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uz.eduplatform.core.common.dto.ApiResponse;
import uz.eduplatform.core.common.dto.PagedResponse;
import uz.eduplatform.core.common.utils.MessageService;
import uz.eduplatform.core.i18n.AcceptLanguage;
import uz.eduplatform.core.security.UserPrincipal;
import uz.eduplatform.modules.assessment.domain.AssignmentStatus;
import uz.eduplatform.modules.assessment.dto.*;
import uz.eduplatform.modules.assessment.service.AssignmentService;
import uz.eduplatform.modules.assessment.service.LiveMonitoringService;
import uz.eduplatform.modules.assessment.service.PromoCodeService;
import uz.eduplatform.modules.assessment.service.ResultService;
import uz.eduplatform.modules.assessment.service.export.ResultExportFacade;
import uz.eduplatform.modules.assessment.service.export.ResultExportFormat;

import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assignments")
@RequiredArgsConstructor
@Tag(name = "Test topshiriqlari", description = "O'qituvchi test topshiriqlarini boshqarish API'lari — yaratish, faollashtirish, natijalar")
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final ResultService resultService;
    private final ResultExportFacade resultExportFacade;
    private final LiveMonitoringService liveMonitoringService;
    private final PromoCodeService promoCodeService;
    private final MessageService messageService;

    /** Returns true if the principal has ADMIN or SUPER_ADMIN authority. */
    private boolean isAdmin(UserPrincipal principal) {
        return principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Yangi test topshiriq yaratish")
    public ResponseEntity<ApiResponse<AssignmentDto>> createAssignment(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language,
            @Valid @RequestBody CreateAssignmentRequest request) {

        AssignmentDto dto = assignmentService.createAssignment(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto, messageService.get("success.assignment.created", language.toLocale())));
    }

    @GetMapping
    @Operation(summary = "O'qituvchi topshiriqlari")
    public ResponseEntity<ApiResponse<PagedResponse<AssignmentDto>>> getMyAssignments(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) AssignmentStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID teacherId = isAdmin(principal) ? null : principal.getId();
        PagedResponse<AssignmentDto> response = assignmentService.getTeacherAssignments(
                teacherId, status, search,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Topshiriq tafsilotlari")
    public ResponseEntity<ApiResponse<AssignmentDto>> getAssignment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {

        UUID userId = isAdmin(principal) ? null : principal.getId();
        AssignmentDto dto = assignmentService.getAssignment(id, userId);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Topshiriq sozlamalarini yangilash")
    public ResponseEntity<ApiResponse<AssignmentDto>> updateAssignment(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAssignmentRequest request) {

        AssignmentDto dto = assignmentService.updateAssignment(id, principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(dto, messageService.get("success.assignment.updated", language.toLocale())));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Topshiriqni faollashtirish")
    public ResponseEntity<ApiResponse<AssignmentDto>> activateAssignment(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language,
            @PathVariable UUID id) {

        AssignmentDto dto = assignmentService.activateAssignment(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(dto, messageService.get("success.assignment.activated", language.toLocale())));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Topshiriqni bekor qilish")
    public ResponseEntity<ApiResponse<AssignmentDto>> cancelAssignment(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language,
            @PathVariable UUID id) {

        AssignmentDto dto = assignmentService.cancelAssignment(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(dto, messageService.get("success.assignment.cancelled", language.toLocale())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Topshiriqni o'chirish")
    public ResponseEntity<ApiResponse<Void>> deleteAssignment(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language,
            @PathVariable UUID id) {

        assignmentService.deleteAssignment(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, messageService.get("success.assignment.deleted", language.toLocale())));
    }

    @GetMapping("/{id}/live")
    @Operation(summary = "Jonli monitoring")
    public ResponseEntity<ApiResponse<LiveMonitoringDto>> getLiveMonitoring(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {

        UUID teacherId = isAdmin(principal) ? null : principal.getId();
        LiveMonitoringDto monitoring = liveMonitoringService.getLiveMonitoring(id, teacherId);
        return ResponseEntity.ok(ApiResponse.success(monitoring));
    }

    @GetMapping("/{id}/results")
    @Operation(summary = "Topshiriq natijalari")
    public ResponseEntity<ApiResponse<AssignmentResultDto>> getResults(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {

        UUID teacherId = isAdmin(principal) ? null : principal.getId();
        AssignmentResultDto results = resultService.getAssignmentResults(id, teacherId);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/{id}/results/export")
    @Operation(summary = "Natijalarni eksport qilish")
    public ResponseEntity<byte[]> exportResults(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestParam(defaultValue = "CSV") ResultExportFormat format,
            Locale locale) {

        byte[] data = resultExportFacade.exportAssignmentResults(id, principal.getId(), format, locale);

        String filename = "results-" + id;
        String contentType;
        if (format == ResultExportFormat.EXCEL) {
            filename += ".xlsx";
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else {
            filename += ".csv";
            contentType = "text/csv; charset=UTF-8";
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(data);
    }

    // ── Promo Code Management ──

    @PostMapping("/{id}/promo-code")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Promokod yaratish", description = "Topshiriq uchun yangi promokod generatsiya qilish. Mavjud faol kod avtomatik bekor qilinadi.")
    public ResponseEntity<ApiResponse<PromoCodeDto>> generatePromoCode(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language,
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) GeneratePromoCodeRequest request) {

        PromoCodeDto dto = promoCodeService.generateCode(id, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto, messageService.get("success.promo.code.generated", language.toLocale())));
    }

    @GetMapping("/{id}/promo-code")
    @Operation(summary = "Faol promokodni olish", description = "Topshiriq uchun faol promokod ma'lumotlarini olish.")
    public ResponseEntity<ApiResponse<PromoCodeDto>> getPromoCode(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {

        PromoCodeDto dto = promoCodeService.getActivePromoCode(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @DeleteMapping("/{id}/promo-code")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Promokodni bekor qilish", description = "Topshiriq uchun faol promokodni o'chirib qo'yish.")
    public ResponseEntity<ApiResponse<Void>> revokePromoCode(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language,
            @PathVariable UUID id) {

        promoCodeService.revokeCode(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, messageService.get("success.promo.code.revoked", language.toLocale())));
    }
}
