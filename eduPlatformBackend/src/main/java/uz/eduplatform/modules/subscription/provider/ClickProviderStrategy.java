package uz.eduplatform.modules.subscription.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uz.eduplatform.modules.subscription.config.PaymentProperties;
import uz.eduplatform.modules.subscription.domain.*;
import uz.eduplatform.modules.subscription.dto.PaymentInitiationResponse;
import uz.eduplatform.modules.subscription.dto.click.ClickCompleteRequest;
import uz.eduplatform.modules.subscription.dto.click.ClickCompleteResponse;
import uz.eduplatform.modules.subscription.dto.click.ClickPrepareRequest;
import uz.eduplatform.modules.subscription.dto.click.ClickPrepareResponse;
import uz.eduplatform.modules.subscription.repository.PaymentRepository;
import uz.eduplatform.modules.subscription.security.ClickSignatureVerifier;
import uz.eduplatform.modules.subscription.service.PaymentOrchestrationService;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClickProviderStrategy implements PaymentProviderStrategy {

    // Click error codes
    public static final int SUCCESS = 0;
    public static final int SIGN_CHECK_FAILED = -1;
    public static final int INCORRECT_AMOUNT = -2;
    public static final int ACTION_NOT_FOUND = -3;
    public static final int ALREADY_PAID = -4;
    public static final int ORDER_NOT_FOUND = -5;
    public static final int TRANSACTION_ERROR = -6;
    public static final int BAD_REQUEST = -8;
    public static final int TRANSACTION_NOT_FOUND = -9;

    private final PaymentProperties paymentProperties;
    private final PaymentRepository paymentRepository;
    private final ClickSignatureVerifier signatureVerifier;

    private PaymentOrchestrationService orchestrationService;

    public void setOrchestrationService(PaymentOrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.CLICK;
    }

    @Override
    public boolean isEnabled() {
        return paymentProperties.getClick().isEnabled();
    }

    @Override
    public PaymentInitiationResponse initiateCheckout(Payment payment, SubscriptionPlan plan) {
        PaymentProperties.ClickConfig config = paymentProperties.getClick();

        String redirectUrl = "https://my.click.uz/services/pay"
                + "?service_id=" + config.getServiceId()
                + "&merchant_id=" + config.getMerchantId()
                + "&amount=" + payment.getAmount().toPlainString()
                + "&transaction_param=" + URLEncoder.encode(payment.getProviderOrderId(), StandardCharsets.UTF_8)
                + "&return_url=" + URLEncoder.encode("", StandardCharsets.UTF_8);

        return PaymentInitiationResponse.builder()
                .paymentId(payment.getId())
                .providerOrderId(payment.getProviderOrderId())
                .provider(PaymentProvider.CLICK)
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .redirectUrl(redirectUrl)
                .providerParams(Map.of(
                        "serviceId", config.getServiceId(),
                        "merchantId", config.getMerchantId()
                ))
                .build();
    }

    /**
     * Handle Click prepare request (action = 0).
     */
    public ClickPrepareResponse handlePrepare(ClickPrepareRequest request) {
        // Verify signature
        boolean signValid = signatureVerifier.verifyPrepare(
                request.getClickTransId(),
                request.getServiceId(),
                request.getMerchantTransId(),
                request.getAmount(),
                request.getAction(),
                request.getSignTime(),
                request.getSignString()
        );

        if (!signValid) {
            return ClickPrepareResponse.builder()
                    .clickTransId(request.getClickTransId())
                    .merchantTransId(request.getMerchantTransId())
                    .error(SIGN_CHECK_FAILED)
                    .errorNote("Sign check failed")
                    .build();
        }

        // Find order
        Optional<Payment> paymentOpt = paymentRepository.findByProviderAndProviderOrderId(
                PaymentProvider.CLICK, request.getMerchantTransId());

        if (paymentOpt.isEmpty()) {
            return ClickPrepareResponse.builder()
                    .clickTransId(request.getClickTransId())
                    .merchantTransId(request.getMerchantTransId())
                    .error(ORDER_NOT_FOUND)
                    .errorNote("Order not found")
                    .build();
        }

        Payment payment = paymentOpt.get();

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            return ClickPrepareResponse.builder()
                    .clickTransId(request.getClickTransId())
                    .merchantTransId(request.getMerchantTransId())
                    .error(ALREADY_PAID)
                    .errorNote("Already paid")
                    .build();
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            return ClickPrepareResponse.builder()
                    .clickTransId(request.getClickTransId())
                    .merchantTransId(request.getMerchantTransId())
                    .error(TRANSACTION_ERROR)
                    .errorNote("Transaction in invalid state")
                    .build();
        }

        // Check amount
        BigDecimal requestAmount = BigDecimal.valueOf(request.getAmount());
        if (payment.getAmount().compareTo(requestAmount) != 0) {
            return ClickPrepareResponse.builder()
                    .clickTransId(request.getClickTransId())
                    .merchantTransId(request.getMerchantTransId())
                    .error(INCORRECT_AMOUNT)
                    .errorNote("Incorrect amount")
                    .build();
        }

        // Store Click transaction ID
        payment.setExternalTransactionId(String.valueOf(request.getClickTransId()));
        paymentRepository.save(payment);

        return ClickPrepareResponse.builder()
                .clickTransId(request.getClickTransId())
                .merchantTransId(request.getMerchantTransId())
                .merchantPrepareId(payment.getId().toString())
                .error(SUCCESS)
                .errorNote("Success")
                .build();
    }

    /**
     * Handle Click complete request (action = 1).
     */
    public ClickCompleteResponse handleComplete(ClickCompleteRequest request) {
        // Verify signature
        boolean signValid = signatureVerifier.verifyComplete(
                request.getClickTransId(),
                request.getServiceId(),
                request.getMerchantTransId(),
                request.getMerchantPrepareId(),
                request.getAmount(),
                request.getAction(),
                request.getSignTime(),
                request.getSignString()
        );

        if (!signValid) {
            return ClickCompleteResponse.builder()
                    .clickTransId(request.getClickTransId())
                    .merchantTransId(request.getMerchantTransId())
                    .error(SIGN_CHECK_FAILED)
                    .errorNote("Sign check failed")
                    .build();
        }

        // Find order
        Optional<Payment> paymentOpt = paymentRepository.findByProviderAndProviderOrderId(
                PaymentProvider.CLICK, request.getMerchantTransId());

        if (paymentOpt.isEmpty()) {
            return ClickCompleteResponse.builder()
                    .clickTransId(request.getClickTransId())
                    .merchantTransId(request.getMerchantTransId())
                    .error(ORDER_NOT_FOUND)
                    .errorNote("Order not found")
                    .build();
        }

        Payment payment = paymentOpt.get();

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            // Idempotent
            return ClickCompleteResponse.builder()
                    .clickTransId(request.getClickTransId())
                    .merchantTransId(request.getMerchantTransId())
                    .merchantConfirmId(payment.getId().toString())
                    .error(SUCCESS)
                    .errorNote("Success")
                    .build();
        }

        // Check if Click reported an error
        if (request.getError() != null && !request.getError().equals("0") && !request.getError().isEmpty()) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Click error: " + request.getError() + " - " + request.getErrorNote());
            paymentRepository.save(payment);

            return ClickCompleteResponse.builder()
                    .clickTransId(request.getClickTransId())
                    .merchantTransId(request.getMerchantTransId())
                    .error(TRANSACTION_ERROR)
                    .errorNote("Transaction failed from Click side")
                    .build();
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            return ClickCompleteResponse.builder()
                    .clickTransId(request.getClickTransId())
                    .merchantTransId(request.getMerchantTransId())
                    .error(TRANSACTION_NOT_FOUND)
                    .errorNote("Transaction not found or not in pending state")
                    .build();
        }

        // Complete the payment
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(java.time.LocalDateTime.now());
        paymentRepository.save(payment);

        // Activate subscription
        if (orchestrationService != null) {
            orchestrationService.onPaymentConfirmed(payment.getId(),
                    String.valueOf(request.getClickTransId()));
        }

        return ClickCompleteResponse.builder()
                .clickTransId(request.getClickTransId())
                .merchantTransId(request.getMerchantTransId())
                .merchantConfirmId(payment.getId().toString())
                .error(SUCCESS)
                .errorNote("Success")
                .build();
    }
}
