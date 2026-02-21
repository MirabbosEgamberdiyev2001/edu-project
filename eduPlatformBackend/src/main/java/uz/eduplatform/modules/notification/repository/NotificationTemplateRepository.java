package uz.eduplatform.modules.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.eduplatform.modules.notification.domain.NotificationChannel;
import uz.eduplatform.modules.notification.domain.NotificationTemplate;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    Optional<NotificationTemplate> findByCodeAndChannelAndLocale(String code, NotificationChannel channel, String locale);
}
