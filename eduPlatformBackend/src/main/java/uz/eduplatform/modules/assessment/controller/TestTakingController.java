package uz.eduplatform.modules.assessment.controller;

import io.github.bucket4j.Bucket;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uz.eduplatform.core.common.dto.ApiResponse;
import uz.eduplatform.core.common.dto.PagedResponse;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.core.common.utils.MessageService;
import uz.eduplatform.core.i18n.AcceptLanguage;
import uz.eduplatform.core.security.UserPrincipal;
import uz.eduplatform.modules.assessment.dto.*;
import uz.eduplatform.modules.assessment.service.AssignmentService;
import uz.eduplatform.modules.assessment.service.GradingService;
import uz.eduplatform.modules.assessment.service.PromoCodeService;
import uz.eduplatform.modules.assessment.service.TestTakingService;

import java.util.UUID;
import java.util.function.Function;

@RestController
@RequestMapping("/api/v1/test-taking")
@RequiredArgsConstructor
@Tag(name = "Test yechish", description = "O'quvchilar uchun test yechish API'lari — boshlash, javob berish, topshirish")
public class TestTakingController {

    private final TestTakingService testTakingService;
    private final AssignmentService assignmentService;
    private final GradingService gradingService;
    private final PromoCodeService promoCodeService;
    private final MessageService messageService;

    @Autowired
    @Qualifier("promoCodeRedeemRateLimiter")
    private Function<String, Bucket> promoCodeRedeemRateLimiter;

    // ==================== Student Endpoints ====================

    @GetMapping("/assignments")
    @Operation(summary = "Mavjud topshiriqlar", description = "O'quvchi uchun ochiq bo'lgan barcha test topshiriqlar ro'yxatini olish.")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<PagedResponse<AssignmentDto>>> getMyAssignments(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Native query requires actual DB column name (created_at, not createdAt)
        PagedResponse<AssignmentDto> response = assignmentService.getStudentAssignments(
                principal.getId(), PageRequest.of(page, size, Sort.by("created_at").descending()));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/assignments/{assignmentId}/start")
    @Operation(summary = "Testni boshlash", description = "Test topshiriqni yechishni boshlash. Vaqt hisobi shu paytdan boshlanadi. IP manzil qayd etiladi.")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<AttemptDto>> startAttempt(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID assignmentId,
            @RequestBody(required = false) StartAttemptRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = extractIpAddress(httpRequest);
        AttemptDto dto = testTakingService.startAttempt(assignmentId, principal.getId(), request, ipAddress);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    private String extractIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/attempts/{attemptId}/answers/batch")
    @Operation(summary = "Javoblarni ommaviy saqlash", description = "Bir nechta javobni birdaniga saqlash (auto-save). Frontend har 30 soniyada avtomatik yuboradi.")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<BatchSaveAnswerResponse>> saveAnswersBatch(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID attemptId,
            @Valid @RequestBody BatchSaveAnswerRequest request) {

        BatchSaveAnswerResponse response = testTakingService.saveAnswersBatch(
                attemptId, principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/attempts/{attemptId}/answer")
    @Operation(summary = "Bitta javobni saqlash", description = "Test davomida bitta savolga javob saqlash yoki mavjud javobni yangilash.")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<AnswerDto>> saveAnswer(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID attemptId,
            @Valid @RequestBody SubmitAnswerRequest request) {

        AnswerDto dto = testTakingService.saveAnswer(attemptId, principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @PostMapping("/attempts/{attemptId}/submit")
    @Operation(summary = "Testni topshirish", description = "Testni yakunlash va baholashga yuborish. Avtomatik baholash darhol amalga oshiriladi.")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<AttemptDto>> submitAttempt(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID attemptId) {

        AttemptDto dto = testTakingService.submitAttempt(attemptId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @PostMapping("/attempts/{attemptId}/tab-switch")
    @Operation(summary = "Tab almashtirishni qayd etish", description = "O'quvchi boshqa tabga o'tganini qayd etish (anti-cheat). Belgilangan chegaradan oshsa, test avtomatik tugatiladi.")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<AttemptDto>> reportTabSwitch(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID attemptId) {

        AttemptDto dto = testTakingService.reportTabSwitch(attemptId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @GetMapping("/attempts/{attemptId}")
    @Operation(summary = "Urinish tafsilotlari", description = "O'quvchining test urinishi haqida to'liq ma'lumot — javoblar, ball, vaqt, holat.")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<AttemptDto>> getAttempt(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID attemptId) {

        AttemptDto dto = testTakingService.getAttempt(attemptId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @GetMapping("/my-attempts")
    @Operation(summary = "Urinishlar tarixini olish", description = "O'quvchining barcha test urinishlari tarixini sahifalab olish.")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<PagedResponse<AttemptDto>>> getMyAttempts(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PagedResponse<AttemptDto> response = testTakingService.getStudentAttempts(
                principal.getId(), PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== Promo Code Endpoint ====================

    @PostMapping("/promo/redeem")
    @Operation(summary = "Promokod bilan ro'yxatdan o'tish", description = "Promokodni kiritib, topshiriqqa ro'yxatdan o'tish. Muvaffaqiyatli bo'lsa, topshiriq talabaga biriktiriladi.")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Void>> redeemPromoCode(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language,
            @Valid @RequestBody RedeemPromoCodeRequest request) {

        Bucket bucket = promoCodeRedeemRateLimiter.apply(principal.getId().toString());
        if (!bucket.tryConsume(1)) {
            throw BusinessException.ofKey("promo.code.rate.limit");
        }

        promoCodeService.redeemCode(request.getCode(), principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null,
                messageService.get("success.promo.code.redeemed", language.toLocale())));
    }

    // ==================== Teacher Grading Endpoint ====================

    @PostMapping("/grade")
    @Operation(summary = "Qo'lda baholash", description = "Ochiq javobli (essay/short-answer) savollarni o'qituvchi tomonidan qo'lda baholash. Faqat TEACHER roli.")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<Void>> gradeAnswer(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody GradeAnswerRequest request) {

        gradingService.gradeManually(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
