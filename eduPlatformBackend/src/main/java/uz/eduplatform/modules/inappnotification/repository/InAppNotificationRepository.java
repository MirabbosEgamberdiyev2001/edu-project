package uz.eduplatform.modules.inappnotification.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import uz.eduplatform.modules.inappnotification.domain.InAppNotification;

import java.util.UUID;

public interface InAppNotificationRepository extends JpaRepository<InAppNotification, UUID> {

    Page<InAppNotification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByUserIdAndIsReadFalse(UUID userId);

    @Modifying
    @Query("UPDATE InAppNotification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    int markAllReadByUserId(UUID userId);

    @Modifying
    @Query("UPDATE InAppNotification n SET n.isRead = true WHERE n.id = :id AND n.userId = :userId")
    int markReadById(UUID id, UUID userId);
}
