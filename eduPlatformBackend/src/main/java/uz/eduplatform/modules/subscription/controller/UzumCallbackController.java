package uz.eduplatform.modules.subscription.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.eduplatform.modules.subscription.dto.uzum.UzumCallbackRequest;
import uz.eduplatform.modules.subscription.dto.uzum.UzumCallbackResponse;
import uz.eduplatform.modules.subscription.provider.UzumProviderStrategy;
import uz.eduplatform.modules.subscription.security.UzumSignatureVerifier;

@Slf4j
@RestController
@RequestMapping("/api/v1/payment/callback/uzum")
@RequiredArgsConstructor
@Tag(name = "Uzum callback", description = "Uzum to'lov tizimi callback endpointi — tashqi so'rovlar uchun")
public class UzumCallbackController {

    private final UzumProviderStrategy uzumProviderStrategy;
    private final UzumSignatureVerifier signatureVerifier;
    private final ObjectMapper objectMapper;

    @PostMapping
    @Operation(summary = "Uzum callback", description = "Uzum serveridan keladigan so'rovlarni qabul qilish. HMAC-SHA256 imzo tekshiriladi. Metodlar: check, create, confirm, reverse.")
    public ResponseEntity<UzumCallbackResponse> handleCallback(
            @RequestHeader(value = "X-Signature", required = false) String signature,
            @RequestBody String rawBody) {

        log.info("Uzum callback received");

        // Verify HMAC-SHA256 signature
        if (!signatureVerifier.verify(signature, rawBody)) {
            log.warn("Uzum signature verification failed");
            return ResponseEntity.ok(
                    UzumCallbackResponse.error(-1, "Signature verification failed"));
        }

        try {
            UzumCallbackRequest request = objectMapper.readValue(rawBody, UzumCallbackRequest.class);
            log.info("Uzum callback: method={}, merchantTransId={}",
                    request.getMethod(), request.getMerchantTransId());

            UzumCallbackResponse response = uzumProviderStrategy.handleCallback(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing Uzum callback", e);
            return ResponseEntity.ok(
                    UzumCallbackResponse.error(-1, "Internal error"));
        }
    }
}
