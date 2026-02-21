package uz.eduplatform.modules.notification.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.eduplatform.modules.notification.domain.NotificationChannel;
import uz.eduplatform.modules.notification.domain.NotificationHistory;
import uz.eduplatform.modules.notification.domain.NotificationStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, UUID> {

    Page<NotificationHistory> findByUserId(UUID userId, Pageable pageable);

    List<NotificationHistory> findByStatusAndNextRetryAtBefore(NotificationStatus status, LocalDateTime now);

    Page<NotificationHistory> findByUserIdAndChannel(UUID userId, NotificationChannel channel, Pageable pageable);

    long countByStatus(NotificationStatus status);

    long countByChannel(NotificationChannel channel);
}
