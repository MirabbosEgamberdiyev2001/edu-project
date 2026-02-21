package uz.eduplatform.modules.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uz.eduplatform.modules.auth.repository.UserSessionRepository;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionCleanupTask {

    private final UserSessionRepository sessionRepository;

    /**
     * Clean up expired and inactive sessions daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        sessionRepository.deleteByExpiresAtBefore(cutoff);
        log.info("Cleaned up expired sessions older than {}", cutoff);
    }
}
