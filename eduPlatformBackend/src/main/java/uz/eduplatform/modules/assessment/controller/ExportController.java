package uz.eduplatform.modules.assessment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uz.eduplatform.core.common.dto.ApiResponse;
import uz.eduplatform.core.security.UserPrincipal;
import uz.eduplatform.modules.assessment.domain.ExportJob;
import uz.eduplatform.modules.assessment.dto.ExportJobDto;
import uz.eduplatform.modules.assessment.service.export.AsyncExportService;
import uz.eduplatform.modules.assessment.service.export.ResultExportFormat;

import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/export-jobs")
@RequiredArgsConstructor
@Tag(name = "Asinxron eksport", description = "Natijalarni asinxron eksport qilish — ishni boshlash, holatni tekshirish, faylni yuklab olish")
@PreAuthorize("hasRole('TEACHER')")
public class ExportController {

    private final AsyncExportService asyncExportService;

    @PostMapping
    @Operation(summary = "Eksport ishini boshlash",
            description = "Natijalar eksportini asinxron boshlash. Job ID qaytariladi, so'ngra holatni polling orqali tekshiring.")
    public ResponseEntity<ApiResponse<ExportJobDto>> startExport(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam UUID assignmentId,
            @RequestParam(defaultValue = "CSV") ResultExportFormat format,
            Locale locale) {

        ExportJobDto job = asyncExportService.startExport(
                assignmentId, principal.getId(), format, locale);
        return ResponseEntity.accepted()
                .body(ApiResponse.success(job));
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "Eksport holati",
            description = "Eksport ishining joriy holatini tekshirish — PENDING, PROCESSING, COMPLETED yoki FAILED.")
    public ResponseEntity<ApiResponse<ExportJobDto>> getJobStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID jobId) {

        ExportJobDto job = asyncExportService.getJobStatus(jobId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(job));
    }

    @GetMapping("/{jobId}/download")
    @Operation(summary = "Eksport faylini yuklab olish",
            description = "Tayyor bo'lgan eksport faylini yuklab olish. Faqat COMPLETED holatdagi ishlar uchun ishlaydi.")
    public ResponseEntity<byte[]> downloadExport(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID jobId) {

        ExportJob job = asyncExportService.getJobForDownload(jobId, principal.getId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + job.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(job.getContentType()))
                .body(job.getFileData());
    }
}
