package uz.eduplatform.modules.content.controller;

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
import org.springframework.web.multipart.MultipartFile;
import uz.eduplatform.core.common.dto.ApiResponse;
import uz.eduplatform.core.common.dto.PagedResponse;
import uz.eduplatform.core.common.utils.MessageService;
import uz.eduplatform.core.i18n.AcceptLanguage;
import uz.eduplatform.core.security.UserPrincipal;
import uz.eduplatform.modules.content.domain.Difficulty;
import uz.eduplatform.modules.content.domain.QuestionStatus;
import uz.eduplatform.modules.content.domain.QuestionType;
import uz.eduplatform.modules.content.dto.*;
import uz.eduplatform.modules.content.service.QuestionImportService;
import uz.eduplatform.modules.content.service.QuestionSearchService;
import uz.eduplatform.modules.content.service.QuestionService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Savollar", description = "Savollarni boshqarish API'lari — yaratish, tahrirlash, import, moderatsiya")
public class QuestionController {

    private final QuestionService questionService;
    private final QuestionImportService importService;
    private final QuestionSearchService searchService;
    private final MessageService messageService;

    @GetMapping("/questions")
    @Operation(summary = "Savollarni filtrlash", description = "Foydalanuvchi savollarini filtrlash: fan, mavzu, tur, qiyinlik, holat va qidiruv bo'yicha. Sahifalash va saralash qo'llab-quvvatlanadi.")
    public ResponseEntity<ApiResponse<PagedResponse<QuestionDto>>> getQuestions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UUID subjectId,
            @RequestParam(required = false) UUID topicId,
            @RequestParam(required = false) QuestionType questionType,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) QuestionStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        QuestionFilterRequest filter = QuestionFilterRequest.builder()
                .subjectId(subjectId)
                .topicId(topicId)
                .questionType(questionType)
                .difficulty(difficulty)
                .status(status)
                .search(search)
                .build();

        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        PagedResponse<QuestionDto> response = questionService.getQuestions(
                principal.getId(), filter, PageRequest.of(page, size, sort), language);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/questions")
    @Operation(summary = "Yangi savol yaratish", description = "Yangi savol yaratish. Ko'p tilli matn (uzl, uzc, ru, en), javob variantlari, qiyinlik darajasi va izoh kiritiladi.")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<QuestionDto>> createQuestion(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateQuestionRequest request,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        QuestionDto question = questionService.createQuestion(principal.getId(), request, language);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(question, messageService.get("question.created", language.toLocale())));
    }

    @PostMapping("/questions/bulk")
    @Operation(summary = "Bir nechta savol yaratish", description = "Bir so'rovda bir nechta savolni ommaviy yaratish. Har bir savol alohida validatsiya qilinadi.")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<BulkCreateResponse>> createQuestionsBulk(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody BulkCreateQuestionRequest request,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        BulkCreateResponse response = questionService.createQuestionsBulk(principal.getId(), request, language);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, messageService.get("question.bulk.created", language.toLocale(),
                        response.getCreated(), response.getSkipped())));
    }

    @GetMapping("/questions/{id}")
    @Operation(summary = "Savolni ID bo'yicha olish", description = "Berilgan ID bo'yicha savol ma'lumotlarini to'liq olish — matn, variantlar, to'g'ri javob, izoh.")
    public ResponseEntity<ApiResponse<QuestionDto>> getQuestion(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        QuestionDto question = questionService.getQuestionById(id, principal.getId(), language);
        return ResponseEntity.ok(ApiResponse.success(question));
    }

    @PutMapping("/questions/{id}")
    @Operation(summary = "Savolni yangilash", description = "Savol ma'lumotlarini to'liq yangilash. Yangilanganda versiya tarixi avtomatik saqlanadi.")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<QuestionDto>> updateQuestion(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateQuestionRequest request,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        QuestionDto question = questionService.updateQuestion(id, principal.getId(), request, language, true);
        return ResponseEntity.ok(ApiResponse.success(question, messageService.get("question.updated", language.toLocale())));
    }

    @PatchMapping("/questions/{id}")
    @Operation(summary = "Savolni qisman yangilash", description = "Savolning faqat yuborilgan maydonlarini yangilash. Versiya tarixi saqlanadi.")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<QuestionDto>> patchQuestion(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateQuestionRequest request,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        QuestionDto question = questionService.updateQuestion(id, principal.getId(), request, language, false);
        return ResponseEntity.ok(ApiResponse.success(question, messageService.get("question.updated", language.toLocale())));
    }

    @DeleteMapping("/questions/{id}")
    @Operation(summary = "Savolni o'chirish", description = "Savolni yumshoq o'chirish. Testlarda ishlatilgan savollar saqlanib qoladi.")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        questionService.deleteQuestion(id, principal.getId(), language);
        return ResponseEntity.ok(ApiResponse.success(null, messageService.get("question.deleted", language.toLocale())));
    }

    @PostMapping("/questions/{id}/submit")
    @Operation(summary = "Savolni moderatsiyaga yuborish", description = "Yaratilgan savolni moderator tekshiruviga yuborish. Holat DRAFT dan PENDING ga o'zgaradi.")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<QuestionDto>> submitForModeration(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        QuestionDto question = questionService.submitForModeration(id, principal.getId(), language);
        return ResponseEntity.ok(ApiResponse.success(question, messageService.get("question.submitted", language.toLocale())));
    }

    @PostMapping("/questions/by-ids")
    @Operation(summary = "ID ro'yxati bo'yicha savollarni olish", description = "Berilgan ID lar ro'yxati bo'yicha savollarni bir so'rovda olish.")
    public ResponseEntity<ApiResponse<List<QuestionDto>>> getQuestionsByIds(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody BulkSubmitRequest request,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        List<QuestionDto> questions = questionService.getQuestionsByIds(
                request.getQuestionIds(), principal.getId(), language);
        return ResponseEntity.ok(ApiResponse.success(questions));
    }

    @PostMapping("/questions/bulk-submit")
    @Operation(summary = "Savollarni ommaviy moderatsiyaga yuborish", description = "Bir nechta savolni birdaniga moderatsiyaga yuborish. Har bir savol alohida tekshiriladi.")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<BulkModerationResponse>> bulkSubmitForModeration(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody BulkSubmitRequest request,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        BulkModerationResponse response = questionService.bulkSubmitForModeration(
                principal.getId(), request.getQuestionIds(), language);
        return ResponseEntity.ok(ApiResponse.success(response,
                messageService.get("question.bulk.submitted", language.toLocale(),
                        response.getSuccessCount(), response.getFailedCount())));
    }

    @GetMapping("/questions/{id}/versions")
    @Operation(summary = "Savol versiyalari tarixini olish", description = "Savolning barcha o'zgarishlar tarixini ko'rish — kim, qachon, qanday o'zgartirgan.")
    public ResponseEntity<ApiResponse<List<QuestionVersionDto>>> getVersionHistory(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        List<QuestionVersionDto> versions = questionService.getVersionHistory(id, principal.getId(), language);
        return ResponseEntity.ok(ApiResponse.success(versions));
    }

    @PostMapping("/questions/{id}/rollback/{version}")
    @Operation(summary = "Savolni avvalgi versiyaga qaytarish", description = "Savolni tanlangan versiya holatiga qaytarish. Joriy holat yangi versiya sifatida saqlanadi.")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<QuestionDto>> rollbackToVersion(
            @PathVariable UUID id,
            @PathVariable Integer version,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        QuestionDto question = questionService.rollbackToVersion(id, version, principal.getId(), language);
        return ResponseEntity.ok(ApiResponse.success(question,
                messageService.get("question.rollback", language.toLocale(), version)));
    }

    @PostMapping("/questions/import")
    @Operation(summary = "Excel dan savollar import qilish", description = "Excel (.xlsx) fayldan savollarni ommaviy import qilish. Shablon formatiga mos bo'lishi shart.")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ImportResult>> importQuestions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam UUID topicId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        ImportResult result = importService.importFromExcel(principal.getId(), topicId, file);
        return ResponseEntity.ok(ApiResponse.success(result,
                messageService.get("question.import.result", language.toLocale(),
                        result.getSuccessCount(), result.getErrorCount())));
    }

    @GetMapping("/questions/export-template")
    @Operation(summary = "Import shablonini yuklab olish", description = "Savollar importi uchun tayyor Excel shablon faylini yuklab olish.")
    public ResponseEntity<byte[]> downloadExportTemplate() {
        byte[] template = importService.generateExportTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "questions_template.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .body(template);
    }

    @GetMapping("/topics/{topicId}/questions")
    @Operation(summary = "Mavzu bo'yicha savollarni olish", description = "Berilgan mavzu ID si bo'yicha barcha savollarni sahifalab olish.")
    public ResponseEntity<ApiResponse<PagedResponse<QuestionDto>>> getQuestionsByTopic(
            @PathVariable UUID topicId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        PagedResponse<QuestionDto> response = questionService.getQuestionsByTopic(
                topicId, principal.getId(), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")), language);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
