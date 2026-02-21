package uz.eduplatform.modules.subscription.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uz.eduplatform.core.common.dto.ApiResponse;
import uz.eduplatform.core.common.dto.PagedResponse;
import uz.eduplatform.core.security.UserPrincipal;
import uz.eduplatform.modules.subscription.domain.PaymentStatus;
import uz.eduplatform.modules.subscription.domain.SubscriptionStatus;
import uz.eduplatform.modules.subscription.dto.*;
import uz.eduplatform.modules.subscription.service.PaymentService;
import uz.eduplatform.modules.subscription.service.SubscriptionService;
import uz.eduplatform.modules.subscription.service.UsageTrackingService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/subscriptions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Admin obunalar", description = "Admin obuna va to'lov boshqaruvi — tayinlash, bekor qilish, to'xtatish, to'lovlarni boshqarish")
public class AdminSubscriptionController {

    private final SubscriptionService subscriptionService;
    private final PaymentService paymentService;
    private final UsageTrackingService usageTrackingService;

    // ── Subscription Management ──

    @GetMapping
    @Operation(summary = "Barcha obunalar ro'yxati", description = "Tizimdagi barcha obunalarni sahifalab olish. Holat bo'yicha filtrlash mumkin.")
    public ResponseEntity<ApiResponse<PagedResponse<UserSubscriptionDto>>> getAllSubscriptions(
            @RequestParam(required = false) SubscriptionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<UserSubscriptionDto> response = subscriptionService.getAllSubscriptions(
                status, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/assign")
    @Operation(summary = "Foydalanuvchiga obuna tayinlash", description = "Admin tomonidan foydalanuvchiga obuna rejasini qo'lda tayinlash — to'lovsiz.")
    public ResponseEntity<ApiResponse<UserSubscriptionDto>> assignSubscription(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AssignSubscriptionRequest request) {
        UserSubscriptionDto sub = subscriptionService.assignSubscription(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(sub));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Obunani bekor qilish (admin)", description = "Istalgan foydalanuvchining obunasini admin tomonidan bekor qilish.")
    public ResponseEntity<ApiResponse<UserSubscriptionDto>> cancelSubscription(
            @PathVariable UUID id) {
        UserSubscriptionDto sub = subscriptionService.cancelSubscription(id);
        return ResponseEntity.ok(ApiResponse.success(sub));
    }

    @PostMapping("/{id}/suspend")
    @Operation(summary = "Obunani to'xtatish", description = "Foydalanuvchi obunasini vaqtinchalik to'xtatish. Cheklovlar darhol kuchga kiradi.")
    public ResponseEntity<ApiResponse<UserSubscriptionDto>> suspendSubscription(
            @PathVariable UUID id) {
        UserSubscriptionDto sub = subscriptionService.suspendSubscription(id);
        return ResponseEntity.ok(ApiResponse.success(sub));
    }

    @PostMapping("/{id}/reactivate")
    @Operation(summary = "Obunani qayta faollashtirish", description = "To'xtatilgan obunani qayta faollashtirish.")
    public ResponseEntity<ApiResponse<UserSubscriptionDto>> reactivateSubscription(
            @PathVariable UUID id) {
        UserSubscriptionDto sub = subscriptionService.reactivateSubscription(id);
        return ResponseEntity.ok(ApiResponse.success(sub));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Foydalanuvchi obuna tarixi", description = "Berilgan foydalanuvchining barcha obunalar tarixini ko'rish.")
    public ResponseEntity<ApiResponse<PagedResponse<UserSubscriptionDto>>> getUserSubscriptions(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<UserSubscriptionDto> response = subscriptionService.getUserSubscriptions(
                userId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/user/{userId}/usage")
    @Operation(summary = "Foydalanuvchi foydalanish statistikasi", description = "Berilgan foydalanuvchining obuna foydalanish ma'lumotlarini ko'rish.")
    public ResponseEntity<ApiResponse<List<UsageDto>>> getUserUsage(@PathVariable UUID userId) {
        List<UsageDto> usage = usageTrackingService.getUserUsageSummary(userId);
        return ResponseEntity.ok(ApiResponse.success(usage));
    }

    // ── Payment Management ──

    @GetMapping("/payments")
    @Operation(summary = "Barcha to'lovlar ro'yxati", description = "Tizimdagi barcha to'lovlarni sahifalab olish. Holat bo'yicha filtrlash mumkin.")
    public ResponseEntity<ApiResponse<PagedResponse<PaymentDto>>> getAllPayments(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<PaymentDto> response = paymentService.getAllPayments(
                status, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/payments/{id}/confirm")
    @Operation(summary = "To'lovni qo'lda tasdiqlash", description = "Admin tomonidan to'lovni qo'lda tasdiqlash. Obuna avtomatik faollashtiriladi.")
    public ResponseEntity<ApiResponse<PaymentDto>> confirmPayment(
            @PathVariable UUID id,
            @RequestParam(required = false) String externalTransactionId) {
        PaymentDto payment = paymentService.confirmPayment(id, externalTransactionId);
        return ResponseEntity.ok(ApiResponse.success(payment));
    }

    @PostMapping("/payments/{id}/fail")
    @Operation(summary = "To'lovni muvaffaqiyatsiz deb belgilash", description = "Admin tomonidan to'lovni FAILED holatiga o'tkazish. Sabab ko'rsatilishi mumkin.")
    public ResponseEntity<ApiResponse<PaymentDto>> failPayment(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        PaymentDto payment = paymentService.failPayment(id, reason);
        return ResponseEntity.ok(ApiResponse.success(payment));
    }

    @PostMapping("/payments/{id}/refund")
    @Operation(summary = "To'lovni qaytarish (refund)", description = "Tasdiqlangan to'lovni qaytarish. Obuna avtomatik bekor qilinadi.")
    public ResponseEntity<ApiResponse<PaymentDto>> refundPayment(@PathVariable UUID id) {
        PaymentDto payment = paymentService.refundPayment(id);
        return ResponseEntity.ok(ApiResponse.success(payment));
    }
}
