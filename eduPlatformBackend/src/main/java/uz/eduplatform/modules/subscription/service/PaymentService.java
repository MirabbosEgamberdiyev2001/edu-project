package uz.eduplatform.modules.subscription.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.eduplatform.core.common.dto.PagedResponse;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.core.common.exception.ResourceNotFoundException;
import uz.eduplatform.modules.subscription.domain.*;
import uz.eduplatform.modules.subscription.dto.CreatePaymentRequest;
import uz.eduplatform.modules.subscription.dto.PaymentDto;
import uz.eduplatform.modules.subscription.repository.PaymentRepository;
import uz.eduplatform.modules.subscription.repository.SubscriptionPlanRepository;
import uz.eduplatform.modules.subscription.repository.UserSubscriptionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SubscriptionPlanRepository planRepository;
    private final UserSubscriptionRepository subscriptionRepository;

    /**
     * Initiate a payment (user chooses plan and provider).
     */
    @Transactional
    public PaymentDto initiatePayment(UUID userId, CreatePaymentRequest request) {
        SubscriptionPlan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionPlan", "id", request.getPlanId()));

        if (plan.getPriceMonthly().compareTo(BigDecimal.ZERO) == 0) {
            throw BusinessException.ofKey("payment.free.plan");
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

        return toDto(paymentRepository.save(payment));
    }

    /**
     * Confirm payment (callback from payment provider or admin manual confirmation).
     */
    @Transactional
    public PaymentDto confirmPayment(UUID paymentId, String externalTransactionId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw BusinessException.ofKey("payment.not.pending");
        }

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setExternalTransactionId(externalTransactionId);
        payment.setPaidAt(LocalDateTime.now());

        return toDto(paymentRepository.save(payment));
    }

    /**
     * Fail a payment.
     */
    @Transactional
    public PaymentDto failPayment(UUID paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw BusinessException.ofKey("payment.not.pending");
        }

        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(reason);

        return toDto(paymentRepository.save(payment));
    }

    /**
     * Refund a payment (admin action).
     */
    @Transactional
    public PaymentDto refundPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw BusinessException.ofKey("payment.only.completed.refundable");
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(LocalDateTime.now());

        return toDto(paymentRepository.save(payment));
    }

    @Transactional(readOnly = true)
    public PagedResponse<PaymentDto> getUserPayments(UUID userId, Pageable pageable) {
        Page<Payment> page = paymentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<PaymentDto> getAllPayments(PaymentStatus status, Pageable pageable) {
        Page<Payment> page;
        if (status != null) {
            page = paymentRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            page = paymentRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PaymentDto getPayment(UUID paymentId) {
        return toDto(paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId)));
    }

    private PaymentDto toDto(Payment p) {
        return PaymentDto.builder()
                .id(p.getId())
                .userId(p.getUserId())
                .subscriptionId(p.getSubscription() != null ? p.getSubscription().getId() : null)
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .provider(p.getProvider())
                .status(p.getStatus())
                .externalTransactionId(p.getExternalTransactionId())
                .paidAt(p.getPaidAt())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private PagedResponse<PaymentDto> toPagedResponse(Page<Payment> page) {
        return PagedResponse.of(
                page.getContent().stream().map(this::toDto).collect(Collectors.toList()),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
