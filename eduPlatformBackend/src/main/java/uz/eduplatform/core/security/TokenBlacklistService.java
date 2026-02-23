package uz.eduplatform.core.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uz.eduplatform.modules.auth.domain.UserSession;
import uz.eduplatform.modules.auth.repository.UserSessionRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final ConcurrentHashMap<String, Instant> blacklistedTokens = new ConcurrentHashMap<>();

    private final UserSessionRepository userSessionRepository;

    public void blacklist(String jti, long remainingTtlMillis) {
        if (jti == null || remainingTtlMillis <= 0) return;

        blacklistedTokens.put(jti, Instant.now().plusMillis(remainingTtlMillis));
        log.debug("Blacklisted JTI: {} with TTL: {}ms", jti, remainingTtlMillis);
    }

    public boolean isBlacklisted(String jti) {
        if (jti == null) return false;

        Instant expiry = blacklistedTokens.get(jti);
        if (expiry == null) return false;

        if (Instant.now().isAfter(expiry)) {
            blacklistedTokens.remove(jti);
            return false;
        }
        return true;
    }

    public void blacklistAllForUser(UUID userId, long defaultTtlMillis) {
        List<UserSession> activeSessions = userSessionRepository.findByUserIdAndIsActiveTrue(userId);
        for (UserSession session : activeSessions) {
            if (session.getAccessJti() != null) {
                blacklist(session.getAccessJti(), defaultTtlMillis);
            }
            if (session.getRefreshJti() != null) {
                blacklist(session.getRefreshJti(), defaultTtlMillis);
            }
        }
        log.info("Blacklisted all JTIs for user: {}, sessions: {}", userId, activeSessions.size());
    }

    @Scheduled(fixedRate = 300_000)
    public void cleanupExpired() {
        int before = blacklistedTokens.size();
        Instant now = Instant.now();
        blacklistedTokens.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
        int removed = before - blacklistedTokens.size();
        if (removed > 0) {
            log.debug("Cleaned up {} expired blacklisted tokens, {} remaining", removed, blacklistedTokens.size());
        }
    }
}
