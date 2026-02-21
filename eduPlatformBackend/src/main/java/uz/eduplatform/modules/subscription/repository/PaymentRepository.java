package uz.eduplatform.modules.subscription.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.eduplatform.modules.subscription.domain.Payment;
import uz.eduplatform.modules.subscription.domain.PaymentProvider;
import uz.eduplatform.modules.subscription.domain.PaymentStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Page<Payment> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Payment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Payment> findByStatusOrderByCreatedAtDesc(PaymentStatus status, Pageable pageable);

    Optional<Payment> findByExternalTransactionId(String externalTransactionId);

    Optional<Payment> findByProviderOrderId(String providerOrderId);

    long countByStatus(PaymentStatus status);

    Optional<Payment> findByProviderAndProviderOrderId(PaymentProvider provider, String providerOrderId);

    Optional<Payment> findByProviderAndExternalTransactionId(PaymentProvider provider, String externalTransactionId);

    List<Payment> findByProviderAndStatusAndCreatedAtBetween(
            PaymentProvider provider, PaymentStatus status,
            LocalDateTime from, LocalDateTime to);

    List<Payment> findByStatusAndCreatedAtBefore(PaymentStatus status, LocalDateTime before);

    List<Payment> findByStatusAndCreatedAtBetween(PaymentStatus status, LocalDateTime from, LocalDateTime to);
}
