package uz.eduplatform.modules.subscription.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uz.eduplatform.modules.auth.repository.UserSessionRepository;
import uz.eduplatform.modules.subscription.domain.Payment;
import uz.eduplatform.modules.subscription.domain.PaymentStatus;
import uz.eduplatform.modules.subscription.domain.SubscriptionStatus;
import uz.eduplatform.modules.subscription.domain.UserSubscription;
import uz.eduplatform.modules.subscription.repository.PaymentRepository;
import uz.eduplatform.modules.subscription.repository.UserSubscriptionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionRenewalTask {

    private final UserSubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final UserSessionRepository sessionRepository;

    /**
     * Expire active subscriptions whose end date has passed.
     * Runs every hour.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expireSubscriptions() {
        LocalDateTime now = LocalDateTime.now();
        List<UserSubscription> expired = subscriptionRepository.findExpiredSubscriptions(now);

        for (UserSubscription sub : expired) {
            sub.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(sub);
            log.info("Subscription expired: subscriptionId={}, userId={}", sub.getId(), sub.getUserId());
        }

        if (!expired.isEmpty()) {
            log.info("Expired {} subscriptions", expired.size());
        }
    }

    /**
     * Fail stale PENDING payments older than 12 hours.
     * Runs every hour.
     */
    @Scheduled(cron = "0 30 * * * *")
    @Transactional
    public void cleanupStalePayments() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(12);
        List<Payment> stalePayments = paymentRepository.findByStatusAndCreatedAtBefore(
                PaymentStatus.PENDING, cutoff);

        for (Payment payment : stalePayments) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Stale payment cleanup: no callback received within 12 hours");
            paymentRepository.save(payment);
            log.info("Stale payment cleaned up: paymentId={}", payment.getId());
        }

        if (!stalePayments.isEmpty()) {
            log.info("Cleaned up {} stale payments", stalePayments.size());
        }
    }

    /**
     * Daily payment reconciliation: checks for inconsistencies between
     * payment status and subscription status.
     * Runs daily at 3 AM.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void reconcilePayments() {
        log.info("Starting daily payment reconciliation");

        // Find COMPLETED payments without active subscription
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        List<Payment> completedPayments = paymentRepository.findByStatusAndCreatedAtBetween(
                PaymentStatus.COMPLETED, yesterday.minusDays(30), yesterday);

        int orphanCount = 0;
        for (Payment payment : completedPayments) {
            if (payment.getSubscription() == null && payment.getPlanId() != null) {
                orphanCount++;
                log.warn("RECONCILIATION: Completed payment {} has no subscription. userId={}, amount={}",
                        payment.getId(), payment.getUserId(), payment.getAmount());
            }
        }

        // Find ACTIVE subscriptions with no COMPLETED payment
        List<UserSubscription> activeSubscriptions = subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE);
        int noPaymentCount = 0;
        for (UserSubscription sub : activeSubscriptions) {
            if (sub.getPaymentId() != null) {
                Payment payment = paymentRepository.findById(sub.getPaymentId()).orElse(null);
                if (payment != null && payment.getStatus() != PaymentStatus.COMPLETED) {
                    noPaymentCount++;
                    log.warn("RECONCILIATION: Active subscription {} has non-completed payment {}. Status={}",
                            sub.getId(), payment.getId(), payment.getStatus());
                }
            }
        }

        log.info("Payment reconciliation complete: {} orphan payments, {} subscriptions with non-completed payments",
                orphanCount, noPaymentCount);
    }

    /**
     * Clean up expired sessions older than 30 days.
     * Runs daily at 4 AM.
     */
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void cleanupExpiredSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        sessionRepository.deleteByExpiresAtBefore(cutoff);
        log.info("Expired session cleanup completed for sessions older than {}", cutoff);
    }
}
