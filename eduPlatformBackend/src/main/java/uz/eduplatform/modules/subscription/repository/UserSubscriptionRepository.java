package uz.eduplatform.modules.subscription.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.eduplatform.modules.subscription.domain.SubscriptionStatus;
import uz.eduplatform.modules.subscription.domain.UserSubscription;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, UUID> {

    Optional<UserSubscription> findByUserIdAndStatus(UUID userId, SubscriptionStatus status);

    List<UserSubscription> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Page<UserSubscription> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<UserSubscription> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<UserSubscription> findByStatusOrderByCreatedAtDesc(SubscriptionStatus status, Pageable pageable);

    @Query("SELECT s FROM UserSubscription s WHERE s.status = 'ACTIVE' AND s.endDate < :now")
    List<UserSubscription> findExpiredSubscriptions(@Param("now") LocalDateTime now);

    List<UserSubscription> findByStatus(SubscriptionStatus status);

    long countByStatus(SubscriptionStatus status);
}
