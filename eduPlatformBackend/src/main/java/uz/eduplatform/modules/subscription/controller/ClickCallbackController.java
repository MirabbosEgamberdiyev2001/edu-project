package uz.eduplatform.modules.subscription.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.eduplatform.modules.subscription.dto.click.ClickCompleteRequest;
import uz.eduplatform.modules.subscription.dto.click.ClickCompleteResponse;
import uz.eduplatform.modules.subscription.dto.click.ClickPrepareRequest;
import uz.eduplatform.modules.subscription.dto.click.ClickPrepareResponse;
import uz.eduplatform.modules.subscription.provider.ClickProviderStrategy;

@Slf4j
@RestController
@RequestMapping("/api/v1/payment/callback/click")
@RequiredArgsConstructor
@Tag(name = "Click callback", description = "Click to'lov tizimi callback endpointlari — tashqi so'rovlar uchun")
public class ClickCallbackController {

    private final ClickProviderStrategy clickProviderStrategy;

    @PostMapping("/prepare")
    @Operation(summary = "Click prepare callback", description = "Click serveridan keladigan prepare (action=0) so'rovini qabul qilish. MD5 imzo tekshiriladi.")
    public ResponseEntity<ClickPrepareResponse> handlePrepare(@RequestBody ClickPrepareRequest request) {
        log.info("Click prepare callback received: clickTransId={}, merchantTransId={}",
                request.getClickTransId(), request.getMerchantTransId());

        try {
            ClickPrepareResponse response = clickProviderStrategy.handlePrepare(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing Click prepare callback", e);
            return ResponseEntity.ok(ClickPrepareResponse.builder()
                    .clickTransId(request.getClickTransId())
                    .merchantTransId(request.getMerchantTransId())
                    .error(ClickProviderStrategy.TRANSACTION_ERROR)
                    .errorNote("Internal error")
                    .build());
        }
    }

    @PostMapping("/complete")
    @Operation(summary = "Click complete callback", description = "Click serveridan keladigan complete (action=1) so'rovini qabul qilish. To'lov tasdiqlangach obuna faollashtiriladi.")
    public ResponseEntity<ClickCompleteResponse> handleComplete(@RequestBody ClickCompleteRequest request) {
        log.info("Click complete callback received: clickTransId={}, merchantTransId={}",
                request.getClickTransId(), request.getMerchantTransId());

        try {
            ClickCompleteResponse response = clickProviderStrategy.handleComplete(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing Click complete callback", e);
            return ResponseEntity.ok(ClickCompleteResponse.builder()
                    .clickTransId(request.getClickTransId())
                    .merchantTransId(request.getMerchantTransId())
                    .error(ClickProviderStrategy.TRANSACTION_ERROR)
                    .errorNote("Internal error")
                    .build());
        }
    }
}
