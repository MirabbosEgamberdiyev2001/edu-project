package uz.eduplatform.modules.content.controller;

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
import uz.eduplatform.core.i18n.AcceptLanguage;
import uz.eduplatform.core.security.UserPrincipal;
import uz.eduplatform.modules.content.dto.BulkModerationRequest;
import uz.eduplatform.modules.content.dto.BulkModerationResponse;
import uz.eduplatform.modules.content.dto.ModerationRequest;
import uz.eduplatform.modules.content.dto.QuestionDto;
import uz.eduplatform.modules.content.service.QuestionModerationService;
import uz.eduplatform.modules.test.dto.TestHistoryDto;
import uz.eduplatform.modules.test.service.TestHistoryService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/moderation")
@RequiredArgsConstructor
@Tag(name = "Moderatsiya", description = "Savollar va global testlarni moderatsiya qilish. Faqat MODERATOR va ADMIN.")
public class ModerationController {

    private final QuestionModerationService moderationService;
    private final TestHistoryService testHistoryService;

    // ===== QUESTION MODERATION =====

    @GetMapping("/questions")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Tekshiruv kutayotgan savollar")
    public ResponseEntity<ApiResponse<PagedResponse<QuestionDto>>> getPendingQuestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        PagedResponse<QuestionDto> response = moderationService.getPendingQuestions(
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt")), language);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/questions/{id}/approve")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Savolni tasdiqlash")
    public ResponseEntity<ApiResponse<QuestionDto>> approveQuestion(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        QuestionDto question = moderationService.approveQuestion(id, principal.getId(), language);
        return ResponseEntity.ok(ApiResponse.success(question));
    }

    @PostMapping("/questions/{id}/reject")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Savolni rad etish")
    public ResponseEntity<ApiResponse<QuestionDto>> rejectQuestion(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ModerationRequest request,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        QuestionDto question = moderationService.rejectQuestion(id, principal.getId(), request.getReason(), language);
        return ResponseEntity.ok(ApiResponse.success(question));
    }

    @PostMapping("/questions/bulk-approve")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Savollarni ommaviy tasdiqlash")
    public ResponseEntity<ApiResponse<BulkModerationResponse>> bulkApprove(
            @Valid @RequestBody BulkModerationRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        BulkModerationResponse response = moderationService.bulkApprove(
                request.getQuestionIds(), principal.getId(), language);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/questions/bulk-reject")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Savollarni ommaviy rad etish")
    public ResponseEntity<ApiResponse<BulkModerationResponse>> bulkReject(
            @Valid @RequestBody BulkModerationRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        BulkModerationResponse response = moderationService.bulkReject(
                request.getQuestionIds(), principal.getId(), request.getReason(), language);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ===== GLOBAL TEST MODERATION =====

    @GetMapping("/tests")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Global tasdiqlash kutayotgan testlar",
            description = "O'qituvchilar global qilish uchun yuborgan testlar ro'yxati.")
    public ResponseEntity<ApiResponse<PagedResponse<TestHistoryDto>>> getPendingGlobalTests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        PagedResponse<TestHistoryDto> response = testHistoryService.getPendingGlobalTests(
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "globalSubmittedAt")), language);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/tests/{id}/approve")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Global testni tasdiqlash",
            description = "Testni global holatga o'tkazish. Barcha foydalanuvchilarga ko'rinadi.")
    public ResponseEntity<ApiResponse<TestHistoryDto>> approveGlobalTest(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        TestHistoryDto dto = testHistoryService.approveGlobal(id, principal.getId(), language);
        return ResponseEntity.ok(ApiResponse.success(dto, "Test global qilindi"));
    }

    @PostMapping("/tests/{id}/reject")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Global testni rad etish",
            description = "Testni rad etish, o'qituvchiga sabab yuboriladi.")
    public ResponseEntity<ApiResponse<TestHistoryDto>> rejectGlobalTest(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ModerationRequest request,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        TestHistoryDto dto = testHistoryService.rejectGlobal(id, principal.getId(), request.getReason(), language);
        return ResponseEntity.ok(ApiResponse.success(dto, "Test rad etildi"));
    }
}
