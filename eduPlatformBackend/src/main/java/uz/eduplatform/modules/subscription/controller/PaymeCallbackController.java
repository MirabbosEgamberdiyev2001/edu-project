package uz.eduplatform.modules.subscription.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.eduplatform.modules.subscription.dto.payme.PaymeError;
import uz.eduplatform.modules.subscription.dto.payme.PaymeRequest;
import uz.eduplatform.modules.subscription.dto.payme.PaymeResponse;
import uz.eduplatform.modules.subscription.provider.PaymeProviderStrategy;
import uz.eduplatform.modules.subscription.security.PaymeAuthVerifier;

@Slf4j
@RestController
@RequestMapping("/api/v1/payment/callback/payme")
@RequiredArgsConstructor
@Tag(name = "Payme callback", description = "Payme to'lov tizimi callback endpointi — tashqi so'rovlar uchun")
public class PaymeCallbackController {

    private final PaymeProviderStrategy paymeProviderStrategy;
    private final PaymeAuthVerifier authVerifier;

    @PostMapping
    @Operation(summary = "Payme JSON-RPC callback", description = "Payme serveridan keladigan JSON-RPC so'rovlarni qabul qilish. Basic Auth bilan himoyalangan. Metodlar: CheckPerformTransaction, CreateTransaction, PerformTransaction, CancelTransaction, CheckTransaction, GetStatement.")
    public ResponseEntity<PaymeResponse> handleCallback(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody PaymeRequest request) {

        log.info("Payme callback received: method={}", request.getMethod());

        // Verify Basic Auth
        if (!authVerifier.verify(authorization)) {
            log.warn("Payme auth verification failed");
            return ResponseEntity.ok(
                    PaymeResponse.error(request.getId(), PaymeError.invalidAuth()));
        }

        try {
            PaymeResponse response = paymeProviderStrategy.handleJsonRpc(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing Payme callback: method={}", request.getMethod(), e);
            return ResponseEntity.ok(
                    PaymeResponse.error(request.getId(), PaymeError.cantPerform()));
        }
    }
}
