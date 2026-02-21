package uz.eduplatform.modules.subscription.controller;

import io.github.bucket4j.Bucket;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uz.eduplatform.core.common.dto.ApiResponse;
import uz.eduplatform.core.common.dto.PagedResponse;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.core.security.UserPrincipal;
import uz.eduplatform.modules.subscription.dto.CreatePaymentRequest;
import uz.eduplatform.modules.subscription.dto.PaymentDto;
import uz.eduplatform.modules.subscription.dto.PaymentInitiationResponse;
import uz.eduplatform.modules.subscription.service.PaymentOrchestrationService;
import uz.eduplatform.modules.subscription.service.PaymentService;

import java.util.UUID;
import java.util.function.Function;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "To'lovlar", description = "To'lov API'lari — to'lov boshlash, tarix, tafsilotlar")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentOrchestrationService orchestrationService;
    private final Function<String, Bucket> paymentRateLimiter;

    public PaymentController(PaymentService paymentService,
                             PaymentOrchestrationService orchestrationService,
                             @Qualifier("paymentInitiateRateLimiter") Function<String, Bucket> paymentRateLimiter) {
        this.paymentService = paymentService;
        this.orchestrationService = orchestrationService;
        this.paymentRateLimiter = paymentRateLimiter;
    }

    @PostMapping("/initiate")
    @Operation(summary = "To'lovni boshlash", description = "Obuna uchun to'lov jarayonini boshlash. Tanlangan provayder (Payme/Click/Uzum) checkout sahifasiga yo'naltirish URL qaytariladi.")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentInitiationResponse>> initiatePayment(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreatePaymentRequest request) {
        Bucket bucket = paymentRateLimiter.apply(principal.getId().toString());
        if (!bucket.tryConsume(1)) {
            throw BusinessException.ofKey("auth.otp.rate.limit.send");
        }
        PaymentInitiationResponse response = orchestrationService.initiatePayment(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "payment.initiated.with.redirect"));
    }

    @GetMapping("/my")
    @Operation(summary = "To'lovlar tarixini olish", description = "Foydalanuvchining barcha to'lovlar tarixini sahifalab olish.")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PagedResponse<PaymentDto>>> getMyPayments(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<PaymentDto> response = paymentService.getUserPayments(
                principal.getId(), PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "To'lov tafsilotlari", description = "Berilgan ID bo'yicha to'lov ma'lumotlarini olish — summa, holat, provayder, sana.")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentDto>> getPayment(@PathVariable UUID id) {
        PaymentDto payment = paymentService.getPayment(id);
        return ResponseEntity.ok(ApiResponse.success(payment));
    }
}
