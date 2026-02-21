package uz.eduplatform.modules.subscription.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.eduplatform.core.common.dto.PagedResponse;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.core.common.exception.ResourceNotFoundException;
import uz.eduplatform.core.i18n.TranslatedField;
import uz.eduplatform.modules.auth.repository.UserRepository;
import uz.eduplatform.modules.subscription.domain.Payment;
import uz.eduplatform.modules.subscription.domain.SubscriptionPlan;
import uz.eduplatform.modules.subscription.domain.SubscriptionStatus;
import uz.eduplatform.modules.subscription.domain.UserSubscription;
import uz.eduplatform.modules.subscription.dto.AssignSubscriptionRequest;
import uz.eduplatform.modules.subscription.dto.UserSubscriptionDto;
import uz.eduplatform.modules.subscription.repository.SubscriptionPlanRepository;
import uz.eduplatform.modules.subscription.repository.UserSubscriptionRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final UserSubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserSubscriptionDto getActiveSubscription(UUID userId) {
        return subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .map(this::toDto)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserSubscriptionDto> getUserSubscriptions(UUID userId, Pageable pageable) {
        Page<UserSubscription> page = subscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserSubscriptionDto> getAllSubscriptions(SubscriptionStatus status, Pageable pageable) {
        Page<UserSubscription> page;
        if (status != null) {
            page = subscriptionRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            page = subscriptionRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return toPagedResponse(page);
    }

    /**
     * Admin assigns a subscription to a user (ADMIN/SUPER_ADMIN full control).
     */
    @Transactional
    public UserSubscriptionDto assignSubscription(UUID adminId, AssignSubscriptionRequest request) {
        userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));

        SubscriptionPlan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionPlan", "id", request.getPlanId()));

        // Cancel any existing active subscription
        Optional<UserSubscription> existing = subscriptionRepository
                .findByUserIdAndStatus(request.getUserId(), SubscriptionStatus.ACTIVE);
        existing.ifPresent(sub -> {
            sub.setStatus(SubscriptionStatus.CANCELLED);
            sub.setCancelledAt(LocalDateTime.now());
            subscriptionRepository.save(sub);
        });

        int months = request.getDurationMonths() != null ? request.getDurationMonths() : 1;
        LocalDateTime now = LocalDateTime.now();

        UserSubscription subscription = UserSubscription.builder()
                .userId(request.getUserId())
                .plan(plan)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(now)
                .endDate(now.plusMonths(months))
                .autoRenew(request.getAutoRenew() != null ? request.getAutoRenew() : true)
                .amountPaid(plan.getPriceMonthly().multiply(java.math.BigDecimal.valueOf(months)))
                .assignedBy(adminId)
                .build();

        return toDto(subscriptionRepository.save(subscription));
    }

    /**
     * Cancel a user's subscription (admin or user themselves).
     */
    @Transactional
    public UserSubscriptionDto cancelSubscription(UUID subscriptionId) {
        UserSubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", subscriptionId));

        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw BusinessException.ofKey("subscription.not.active");
        }

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setCancelledAt(LocalDateTime.now());
        return toDto(subscriptionRepository.save(subscription));
    }

    /**
     * Suspend a user's subscription (admin action).
     */
    @Transactional
    public UserSubscriptionDto suspendSubscription(UUID subscriptionId) {
        UserSubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", subscriptionId));

        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw BusinessException.ofKey("subscription.not.active");
        }

        subscription.setStatus(SubscriptionStatus.SUSPENDED);
        return toDto(subscriptionRepository.save(subscription));
    }

    /**
     * Reactivate a suspended subscription (admin action).
     */
    @Transactional
    public UserSubscriptionDto reactivateSubscription(UUID subscriptionId) {
        UserSubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", subscriptionId));

        if (subscription.getStatus() != SubscriptionStatus.SUSPENDED) {
            throw BusinessException.ofKey("subscription.not.suspended");
        }

        subscription.setStatus(SubscriptionStatus.ACTIVE);
        return toDto(subscriptionRepository.save(subscription));
    }

    /**
     * Activate subscription from a confirmed payment.
     */
    @Transactional
    public UserSubscriptionDto activateFromPayment(Payment payment) {
        SubscriptionPlan plan = planRepository.findById(payment.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionPlan", "id", payment.getPlanId()));

        // Cancel any existing active subscription
        Optional<UserSubscription> existing = subscriptionRepository
                .findByUserIdAndStatus(payment.getUserId(), SubscriptionStatus.ACTIVE);
        existing.ifPresent(sub -> {
            sub.setStatus(SubscriptionStatus.CANCELLED);
            sub.setCancelledAt(LocalDateTime.now());
            subscriptionRepository.save(sub);
        });

        int months = payment.getDurationMonths() != null ? payment.getDurationMonths() : 1;
        LocalDateTime now = LocalDateTime.now();

        UserSubscription subscription = UserSubscription.builder()
                .userId(payment.getUserId())
                .plan(plan)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(now)
                .endDate(now.plusMonths(months))
                .autoRenew(true)
                .amountPaid(payment.getAmount())
                .paymentId(payment.getId())
                .build();

        subscription = subscriptionRepository.save(subscription);

        // Link payment to subscription
        payment.setSubscription(subscription);

        return toDto(subscription);
    }

    /**
     * Get the active plan for a user. Returns null if user has no active subscription (FREE tier).
     */
    @Transactional(readOnly = true)
    public SubscriptionPlan getActivePlan(UUID userId) {
        return subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .map(UserSubscription::getPlan)
                .orElse(null);
    }

    private UserSubscriptionDto toDto(UserSubscription sub) {
        String userName = userRepository.findById(sub.getUserId())
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse("Unknown");

        return UserSubscriptionDto.builder()
                .id(sub.getId())
                .userId(sub.getUserId())
                .userName(userName)
                .planId(sub.getPlan().getId())
                .planName(TranslatedField.resolve(sub.getPlan().getName()))
                .status(sub.getStatus())
                .startDate(sub.getStartDate())
                .endDate(sub.getEndDate())
                .autoRenew(sub.getAutoRenew())
                .amountPaid(sub.getAmountPaid())
                .assignedBy(sub.getAssignedBy())
                .createdAt(sub.getCreatedAt())
                .build();
    }

    private PagedResponse<UserSubscriptionDto> toPagedResponse(Page<UserSubscription> page) {
        return PagedResponse.of(
                page.getContent().stream().map(this::toDto).collect(java.util.stream.Collectors.toList()),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
