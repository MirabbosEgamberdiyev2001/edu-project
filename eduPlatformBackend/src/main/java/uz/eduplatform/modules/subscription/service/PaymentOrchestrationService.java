package uz.eduplatform.modules.subscription.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.eduplatform.core.audit.AuditService;
import uz.eduplatform.core.common.exception.ResourceNotFoundException;
import uz.eduplatform.modules.subscription.domain.*;
import uz.eduplatform.modules.subscription.dto.CreatePaymentRequest;
import uz.eduplatform.modules.subscription.dto.PaymentInitiationResponse;
import uz.eduplatform.modules.subscription.provider.ClickProviderStrategy;
import uz.eduplatform.modules.subscription.provider.PaymeProviderStrategy;
import uz.eduplatform.modules.subscription.provider.PaymentProviderFactory;
import uz.eduplatform.modules.subscription.provider.PaymentProviderStrategy;
import uz.eduplatform.modules.subscription.provider.UzumProviderStrategy;
import uz.eduplatform.modules.subscription.repository.PaymentRepository;
import uz.eduplatform.modules.subscription.repository.SubscriptionPlanRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentOrchestrationService {

    private final PaymentRepository paymentRepository;
    private final SubscriptionPlanRepository planRepository;
    private final PaymentProviderFactory providerFactory;
    private final SubscriptionService subscriptionService;
    private final AuditService auditService;
    private final PaymeProviderStrategy paymeProviderStrategy;
    private final ClickProviderStrategy clickProviderStrategy;
    private final UzumProviderStrategy uzumProviderStrategy;

    @PostConstruct
    public void init() {
        // Resolve circular dependency: strategies need orchestration service for callbacks
        paymeProviderStrategy.setOrchestrationService(this);
        clickProviderStrategy.setOrchestrationService(this);
        uzumProviderStrategy.setOrchestrationService(this);
    }

    /**
     * Initiate a payment: create Payment record and generate provider checkout URL.
     */
    @Transactional
    public PaymentInitiationResponse initiatePayment(UUID userId, CreatePaymentRequest request) {
        SubscriptionPlan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionPlan", "id", request.getPlanId()));

        if (plan.getPriceMonthly().compareTo(BigDecimal.ZERO) == 0) {
            throw uz.eduplatform.core.common.exception.BusinessException.ofKey("payment.free.plan");
        }

        int months = request.getDurationMonths() != null ? request.getDurationMonths() : 1;
        BigDecimal amount = plan.getPriceMonthly().multiply(BigDecimal.valueOf(months));

        Payment payment = Payment.builder()
                .userId(userId)
                .amount(amount)
                .currency(plan.getCurrency())
                .provider(request.getProvider())
                .status(PaymentStatus.PENDING)
                .providerOrderId(UUID.randomUUID().toString())
                .planId(plan.getId())
                .durationMonths(months)
                .build();

        payment = paymentRepository.save(payment);

        // Get provider strategy and generate checkout URL
        PaymentProviderStrategy strategy = providerFactory.getStrategy(request.getProvider());
        PaymentInitiationResponse response = strategy.initiateCheckout(payment, plan);

        auditService.log(userId, null, "PAYMENT_INITIATED", "PAYMENT",
                "Payment", payment.getId(),
                null,
                Map.of("provider", request.getProvider().name(),
                        "amount", amount.toString(),
                        "planId", plan.getId().toString()));

        log.info("Payment initiated: paymentId={}, provider={}, amount={}, userId={}",
                payment.getId(), request.getProvider(), amount, userId);

        return response;
    }

    /**
     * Called when payment is confirmed by provider callback.
     */
    @Transactional
    public void onPaymentConfirmed(UUID paymentId, String externalTransactionId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        // Already handled (idempotent)
        if (payment.getStatus() == PaymentStatus.COMPLETED && payment.getSubscription() != null) {
            return;
        }

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setExternalTransactionId(externalTransactionId);
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);
        }

        // Activate subscription from payment
        if (payment.getPlanId() != null) {
            subscriptionService.activateFromPayment(payment);
        }

        auditService.log(payment.getUserId(), null, "PAYMENT_CONFIRMED", "PAYMENT",
                "Payment", paymentId,
                Map.of("previousStatus", "PENDING"),
                Map.of("newStatus", "COMPLETED", "externalTransactionId",
                        externalTransactionId != null ? externalTransactionId : ""));

        log.info("Payment confirmed and subscription activated: paymentId={}, extTxId={}",
                paymentId, externalTransactionId);
    }

    /**
     * Called when payment fails.
     */
    @Transactional
    public void onPaymentFailed(UUID paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(reason);
        paymentRepository.save(payment);

        auditService.log(payment.getUserId(), null, "PAYMENT_FAILED", "PAYMENT",
                "Payment", paymentId,
                null, Map.of("reason", reason != null ? reason : "unknown"));

        log.info("Payment failed: paymentId={}, reason={}", paymentId, reason);
    }

    /**
     * Called when payment is cancelled by provider.
     */
    @Transactional
    public void onPaymentCancelled(UUID paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        payment.setStatus(PaymentStatus.CANCELLED);
        payment.setFailureReason(reason);
        paymentRepository.save(payment);

        auditService.log(payment.getUserId(), null, "PAYMENT_CANCELLED", "PAYMENT",
                "Payment", paymentId,
                null, Map.of("reason", reason != null ? reason : "unknown"));

        log.info("Payment cancelled: paymentId={}, reason={}", paymentId, reason);
    }
}
