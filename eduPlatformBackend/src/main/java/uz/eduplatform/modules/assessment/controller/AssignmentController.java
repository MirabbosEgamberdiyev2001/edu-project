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
import uz.eduplatform.core.security.UserPrincipal;
import uz.eduplatform.modules.assessment.domain.AssignmentStatus;
import uz.eduplatform.modules.assessment.dto.*;
import uz.eduplatform.modules.assessment.service.AssignmentService;
import uz.eduplatform.modules.assessment.service.LiveMonitoringService;
import uz.eduplatform.modules.assessment.service.ResultService;
import uz.eduplatform.modules.assessment.service.export.ResultExportFacade;
import uz.eduplatform.modules.assessment.service.export.ResultExportFormat;

import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assignments")
@RequiredArgsConstructor
@Tag(name = "Test topshiriqlari", description = "O'qituvchi test topshiriqlarini boshqarish API'lari — yaratish, faollashtirish, natijalar")
@PreAuthorize("hasRole('TEACHER')")
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final ResultService resultService;
    private final ResultExportFacade resultExportFacade;
    private final LiveMonitoringService liveMonitoringService;

    @PostMapping
    @Operation(summary = "Yangi test topshiriq yaratish", description = "Guruhga test topshiriq tayinlash — testni tanlash, muddatni belgilash, urinishlar sonini cheklash.")
    public ResponseEntity<ApiResponse<AssignmentDto>> createAssignment(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateAssignmentRequest request) {

        AssignmentDto dto = assignmentService.createAssignment(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto));
    }

    @GetMapping
    @Operation(summary = "O'qituvchi topshiriqlari", description = "Joriy o'qituvchining barcha test topshiriqlarini sahifalab olish. Holat bo'yicha filtrlash mumkin.")
    public ResponseEntity<ApiResponse<PagedResponse<AssignmentDto>>> getMyAssignments(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) AssignmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PagedResponse<AssignmentDto> response = assignmentService.getTeacherAssignments(
                principal.getId(), status, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Topshiriq tafsilotlari", description = "Test topshiriqning to'liq ma'lumotlarini olish — sozlamalar, guruh, muddat, urinishlar.")
    public ResponseEntity<ApiResponse<AssignmentDto>> getAssignment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {

        AssignmentDto dto = assignmentService.getAssignment(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Topshiriq sozlamalarini yangilash", description = "Test topshiriq sozlamalarini o'zgartirish — muddat, urinishlar soni, anti-cheat parametrlari.")
    public ResponseEntity<ApiResponse<AssignmentDto>> updateAssignment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAssignmentRequest request) {

        AssignmentDto dto = assignmentService.updateAssignment(id, principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Topshiriqni faollashtirish", description = "Test topshiriqni o'quvchilarga ochish. Faollashtirilgandan so'ng o'quvchilar testni boshlashlari mumkin.")
    public ResponseEntity<ApiResponse<AssignmentDto>> activateAssignment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {

        AssignmentDto dto = assignmentService.activateAssignment(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Topshiriqni bekor qilish", description = "Test topshiriqni bekor qilish. Davom etayotgan urinishlar avtomatik tugatiladi.")
    public ResponseEntity<ApiResponse<AssignmentDto>> cancelAssignment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {

        AssignmentDto dto = assignmentService.cancelAssignment(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Topshiriqni o'chirish", description = "Test topshiriqni yumshoq o'chirish. Natijalar saqlanib qoladi.")
    public ResponseEntity<ApiResponse<Void>> deleteAssignment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {

        assignmentService.deleteAssignment(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{id}/live")
    @Operation(summary = "Jonli monitoring", description = "Faol test topshiriqning real vaqtdagi holatini ko'rish — kim yechayapti, nechta savol javoblangan.")
    public ResponseEntity<ApiResponse<LiveMonitoringDto>> getLiveMonitoring(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {

        LiveMonitoringDto monitoring = liveMonitoringService.getLiveMonitoring(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(monitoring));
    }

    @GetMapping("/{id}/results")
    @Operation(summary = "Topshiriq natijalari", description = "Test topshiriq natijalari — o'quvchilar baholari, o'rtacha ball, eng yuqori/past natijalar.")
    public ResponseEntity<ApiResponse<AssignmentResultDto>> getResults(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {

        AssignmentResultDto results = resultService.getAssignmentResults(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/{id}/results/export")
    @Operation(summary = "Natijalarni eksport qilish", description = "Test natijalari jadvalini CSV yoki Excel formatida yuklab olish.")
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
}
