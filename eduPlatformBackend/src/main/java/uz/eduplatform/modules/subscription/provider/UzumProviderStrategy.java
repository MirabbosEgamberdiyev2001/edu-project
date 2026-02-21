package uz.eduplatform.modules.subscription.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uz.eduplatform.modules.subscription.config.PaymentProperties;
import uz.eduplatform.modules.subscription.domain.*;
import uz.eduplatform.modules.subscription.dto.PaymentInitiationResponse;
import uz.eduplatform.modules.subscription.dto.uzum.UzumCallbackRequest;
import uz.eduplatform.modules.subscription.dto.uzum.UzumCallbackResponse;
import uz.eduplatform.modules.subscription.repository.PaymentRepository;
import uz.eduplatform.modules.subscription.service.PaymentOrchestrationService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UzumProviderStrategy implements PaymentProviderStrategy {

    private final PaymentProperties paymentProperties;
    private final PaymentRepository paymentRepository;

    private PaymentOrchestrationService orchestrationService;

    public void setOrchestrationService(PaymentOrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.UZUM;
    }

    @Override
    public boolean isEnabled() {
        return paymentProperties.getUzum().isEnabled();
    }

    @Override
    public PaymentInitiationResponse initiateCheckout(Payment payment, SubscriptionPlan plan) {
        // Uzum checkout URL (merchant-specific — to be configured)
        String redirectUrl = "https://www.uzumbank.uz/pay"
                + "?serviceId=" + paymentProperties.getUzum().getServiceId()
                + "&merchantTransId=" + payment.getProviderOrderId()
                + "&amount=" + payment.getAmount().toPlainString();

        return PaymentInitiationResponse.builder()
                .paymentId(payment.getId())
                .providerOrderId(payment.getProviderOrderId())
                .provider(PaymentProvider.UZUM)
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .redirectUrl(redirectUrl)
                .providerParams(Map.of(
                        "serviceId", paymentProperties.getUzum().getServiceId()
                ))
                .build();
    }

    /**
     * Handle Uzum callback — routes by method field.
     */
    public UzumCallbackResponse handleCallback(UzumCallbackRequest request) {
        String method = request.getMethod();

        return switch (method) {
            case "check" -> handleCheck(request);
            case "create" -> handleCreate(request);
            case "confirm" -> handleConfirm(request);
            case "reverse" -> handleReverse(request);
            default -> UzumCallbackResponse.error(-1, "Unknown method: " + method);
        };
    }

    private UzumCallbackResponse handleCheck(UzumCallbackRequest request) {
        Optional<Payment> paymentOpt = paymentRepository.findByProviderAndProviderOrderId(
                PaymentProvider.UZUM, request.getMerchantTransId());

        if (paymentOpt.isEmpty()) {
            return UzumCallbackResponse.error(-1, "Order not found");
        }

        Payment payment = paymentOpt.get();

        if (payment.getStatus() != PaymentStatus.PENDING) {
            return UzumCallbackResponse.error(-2, "Order not available for payment");
        }

        // Check amount
        long expectedAmount = payment.getAmount().longValue();
        if (request.getAmount() == null || request.getAmount() != expectedAmount) {
            return UzumCallbackResponse.error(-3, "Amount mismatch");
        }

        return UzumCallbackResponse.success(request.getTransactionId());
    }

    private UzumCallbackResponse handleCreate(UzumCallbackRequest request) {
        Optional<Payment> paymentOpt = paymentRepository.findByProviderAndProviderOrderId(
                PaymentProvider.UZUM, request.getMerchantTransId());

        if (paymentOpt.isEmpty()) {
            return UzumCallbackResponse.error(-1, "Order not found");
        }

        Payment payment = paymentOpt.get();

        // Idempotent: if already has this ext transaction id
        if (payment.getExternalTransactionId() != null
                && payment.getExternalTransactionId().equals(request.getTransactionId())) {
            return UzumCallbackResponse.success(request.getTransactionId());
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            return UzumCallbackResponse.error(-2, "Order not available");
        }

        payment.setExternalTransactionId(request.getTransactionId());
        paymentRepository.save(payment);

        return UzumCallbackResponse.success(request.getTransactionId());
    }

    private UzumCallbackResponse handleConfirm(UzumCallbackRequest request) {
        Optional<Payment> paymentOpt = paymentRepository.findByProviderAndExternalTransactionId(
                PaymentProvider.UZUM, request.getTransactionId());

        if (paymentOpt.isEmpty()) {
            return UzumCallbackResponse.error(-1, "Transaction not found");
        }

        Payment payment = paymentOpt.get();

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            // Idempotent
            long confirmTime = payment.getPaidAt() != null
                    ? payment.getPaidAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000
                    : System.currentTimeMillis();
            return UzumCallbackResponse.success(request.getTransactionId(), confirmTime);
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            return UzumCallbackResponse.error(-2, "Transaction not available for confirmation");
        }

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        long confirmTime = System.currentTimeMillis();

        // Activate subscription
        if (orchestrationService != null) {
            orchestrationService.onPaymentConfirmed(payment.getId(), request.getTransactionId());
        }

        return UzumCallbackResponse.success(request.getTransactionId(), confirmTime);
    }

    private UzumCallbackResponse handleReverse(UzumCallbackRequest request) {
        Optional<Payment> paymentOpt = paymentRepository.findByProviderAndExternalTransactionId(
                PaymentProvider.UZUM, request.getTransactionId());

        if (paymentOpt.isEmpty()) {
            return UzumCallbackResponse.error(-1, "Transaction not found");
        }

        Payment payment = paymentOpt.get();

        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            // Idempotent
            return UzumCallbackResponse.success(request.getTransactionId());
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        payment.setFailureReason("Uzum reverse");
        paymentRepository.save(payment);

        if (orchestrationService != null) {
            orchestrationService.onPaymentCancelled(payment.getId(), "Uzum reverse");
        }

        return UzumCallbackResponse.success(request.getTransactionId());
    }
}
