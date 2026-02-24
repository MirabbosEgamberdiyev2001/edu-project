package uz.eduplatform.modules.test.controller;

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
import uz.eduplatform.modules.content.domain.Difficulty;
import uz.eduplatform.modules.content.domain.QuestionStatus;
import uz.eduplatform.modules.content.dto.QuestionDto;
import uz.eduplatform.modules.test.dto.*;
import uz.eduplatform.modules.test.service.TestGenerationService;
import uz.eduplatform.modules.test.service.TestHistoryService;
import uz.eduplatform.modules.test.service.TestValidationService;
import uz.eduplatform.modules.test.service.export.ExportFormat;
import uz.eduplatform.modules.test.service.export.TestExportFacade;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tests")
@RequiredArgsConstructor
@Tag(name = "Testlar", description = "Test generatsiyasi va tarix API'lari — yaratish, eksport qilish, takrorlash")
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
public class TestController {

    private final TestGenerationService generationService;
    private final TestHistoryService historyService;
    private final TestValidationService validationService;
    private final TestExportFacade exportFacade;
    private final MessageService messageService;

    private static final String DOCX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    @PostMapping("/generate")
    @Operation(summary = "Yangi test generatsiya qilish", description = "Tanlangan mavzular, qiyinlik darajasi va savollar soniga asoslanib yangi test yaratish. Savollar tasodifiy tanlanadi.")
    public ResponseEntity<ApiResponse<GenerateTestResponse>> generateTest(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language,
            @Valid @RequestBody GenerateTestRequest request) {

        GenerateTestResponse response = generationService.generateTest(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, messageService.get("success.test.generated", language.toLocale())));
    }

    @PostMapping("/generate/preview")
    @Operation(summary = "Test preview ko'rish", description = "Tanlangan parametrlar asosida test preview yaratish — savollar va variantlarni ko'rish, lekin saqlash yo'q.")
    public ResponseEntity<ApiResponse<GenerateTestResponse>> previewTest(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody GenerateTestRequest request) {

        GenerateTestResponse response = generationService.previewTest(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/generate/validate")
    @Operation(summary = "Test parametrlarini tekshirish", description = "Test yaratishdan oldin parametrlarni tekshirish — yetarli savol bormi, sozlamalar to'g'rimi.")
    public ResponseEntity<ApiResponse<AvailableQuestionsResponse>> validateParameters(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language,
            @Valid @RequestBody GenerateTestRequest request) {

        validationService.validateRequest(request);
        AvailableQuestionsResponse available = validationService.getAvailableQuestions(request.getTopicIds(), principal.getId());
        return ResponseEntity.ok(ApiResponse.success(available, messageService.get("test.parameters.valid", language.toLocale())));
    }

    @GetMapping("/generate/available")
    @Operation(summary = "Mavzulardagi mavjud savollar statistikasi", description = "Tanlangan mavzulardagi savollar soni, qiyinlik darajalari bo'yicha taqsimotni olish.")
    public ResponseEntity<ApiResponse<AvailableQuestionsResponse>> getAvailableQuestions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language,
            @RequestParam @NotEmpty @Size(max = 50) List<UUID> topicIds) {

        AvailableQuestionsResponse response = validationService.getAvailableQuestions(topicIds, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/generate/questions")
    @Operation(summary = "Qo'lda tanlash uchun savollar", description = "Test uchun savollarni qo'lda tanlash. Mavzu, qiyinlik, holat va matn bo'yicha filtrlash mumkin.")
    public ResponseEntity<ApiResponse<PagedResponse<QuestionDto>>> getQuestionsForSelection(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language,
            @RequestParam @NotEmpty @Size(max = 50) List<UUID> topicIds,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) QuestionStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        PagedResponse<QuestionDto> response = validationService.getQuestionsForSelection(
                topicIds, difficulty, status, search,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")),
                language, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/history")
    @Operation(summary = "Test yaratish tarixini olish", description = "Foydalanuvchi yaratgan barcha testlar tarixini sahifalab olish.")
    public ResponseEntity<ApiResponse<PagedResponse<TestHistoryDto>>> getTestHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PagedResponse<TestHistoryDto> response = historyService.getTestHistory(
                principal.getId(), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")), language);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/history/{id}")
    @Operation(summary = "Test tafsilotlarini olish", description = "Yaratilgan testning to'liq ma'lumotlarini olish — savollar, variantlar, javoblar kaliti.")
    public ResponseEntity<ApiResponse<TestHistoryDto>> getTestDetails(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        TestHistoryDto test = historyService.getTestById(id, principal.getId(), language);
        return ResponseEntity.ok(ApiResponse.success(test));
    }

    @PutMapping("/history/{id}")
    @Operation(summary = "Testni yangilash", description = "Test sarlavhasi, kategoriyasi va sarlavha sozlamalarini yangilash.")
    public ResponseEntity<ApiResponse<TestHistoryDto>> updateTest(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language,
            @Valid @RequestBody UpdateTestRequest request) {

        TestHistoryDto dto = historyService.updateTest(id, principal.getId(), request, language);
        return ResponseEntity.ok(ApiResponse.success(dto, messageService.get("success.test.updated", language.toLocale())));
    }

    @DeleteMapping("/history/{id}")
    @Operation(summary = "Testni tarixdan o'chirish", description = "Yaratilgan testni tarixdan yumshoq o'chirish.")
    public ResponseEntity<ApiResponse<Void>> deleteTest(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        historyService.deleteTest(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, messageService.get("success.test.deleted", language.toLocale())));
    }

    @PostMapping("/history/{id}/duplicate")
    @Operation(summary = "Testni nusxalash (yangi tasodifiy)", description = "Mavjud test parametrlari asosida yangi tasodifiy savollar bilan test yaratish.")
    public ResponseEntity<ApiResponse<GenerateTestResponse>> duplicateTest(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        GenerateTestResponse response = historyService.duplicateTest(id, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, messageService.get("success.test.duplicated", language.toLocale())));
    }

    @PostMapping("/history/{id}/regenerate")
    @Operation(summary = "Testni qayta generatsiya qilish", description = "Xuddi shu parametrlar va seed bilan testni qayta yaratish. Bir xil savollar tanlanadi.")
    public ResponseEntity<ApiResponse<GenerateTestResponse>> regenerateTest(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        GenerateTestResponse response = historyService.regenerateTest(id, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, messageService.get("success.test.regenerated", language.toLocale())));
    }

    // ===== Publish Endpoints =====

    @PostMapping("/history/{id}/publish")
    @Operation(summary = "Testni ochiq qilish", description = "Testni public qilish, slug generatsiya qilish va davomiylik (daqiqa) belgilash.")
    public ResponseEntity<ApiResponse<TestHistoryDto>> publishTest(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language,
            @RequestParam(required = false) Integer durationMinutes) {

        TestHistoryDto dto = historyService.publishTest(id, principal.getId(), durationMinutes, language);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @DeleteMapping("/history/{id}/publish")
    @Operation(summary = "Testni yopish", description = "Testni public holatidan olib tashlash.")
    public ResponseEntity<ApiResponse<TestHistoryDto>> unpublishTest(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        TestHistoryDto dto = historyService.unpublishTest(id, principal.getId(), language);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    // ===== Unified Export Endpoints =====

    @GetMapping("/history/{id}/export/test")
    @Operation(summary = "Testni eksport qilish", description = "Testni PDF yoki DOCX formatida yuklab olish. Faqat savollar va variantlar chiqariladi.")
    public ResponseEntity<byte[]> exportTest(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language,
            @RequestParam(defaultValue = "PDF") ExportFormat format) {

        byte[] data = exportFacade.exportTest(id, principal.getId(), format, language.toLocale());
        return buildExportResponse(data, "test_" + id, format);
    }

    @GetMapping("/history/{id}/export/answer-key")
    @Operation(summary = "Javoblar kalitini eksport qilish", description = "Test javoblari kalitini PDF yoki DOCX formatida yuklab olish.")
    public ResponseEntity<byte[]> exportAnswerKey(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language,
            @RequestParam(defaultValue = "PDF") ExportFormat format) {

        byte[] data = exportFacade.exportAnswerKey(id, principal.getId(), format, language.toLocale());
        return buildExportResponse(data, "answer_key_" + id, format);
    }

    @GetMapping("/history/{id}/export/combined")
    @Operation(summary = "Test + javoblar kalitini birga eksport qilish", description = "Test va javoblar kalitini bitta faylda PDF yoki DOCX formatida yuklab olish.")
    public ResponseEntity<byte[]> exportCombined(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language,
            @RequestParam(defaultValue = "PDF") ExportFormat format) {

        byte[] data = exportFacade.exportCombined(id, principal.getId(), format, language.toLocale());
        return buildExportResponse(data, "combined_" + id, format);
    }

    @GetMapping("/history/{id}/export/proofs")
    @Operation(summary = "Yechimlar (proof) ni eksport qilish", description = "Savollarning batafsil yechimlari va izohlarini PDF yoki DOCX formatida yuklab olish.")
    public ResponseEntity<byte[]> exportProofs(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language,
            @RequestParam(defaultValue = "PDF") ExportFormat format) {

        byte[] data = exportFacade.exportProofs(id, principal.getId(), format, language.toLocale());
        return buildExportResponse(data, "proofs_" + id, format);
    }

    private ResponseEntity<byte[]> buildExportResponse(byte[] data, String filename, ExportFormat format) {
        String extension = format == ExportFormat.PDF ? ".pdf" : ".docx";
        MediaType mediaType = format == ExportFormat.PDF
                ? MediaType.APPLICATION_PDF
                : MediaType.parseMediaType(DOCX_CONTENT_TYPE);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + extension + "\"")
                .contentType(mediaType)
                .body(data);
    }
}
