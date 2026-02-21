package uz.eduplatform.core.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import uz.eduplatform.modules.auth.domain.UserSession;
import uz.eduplatform.modules.auth.repository.UserSessionRepository;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "bl:";

    private final StringRedisTemplate redisTemplate;
    private final UserSessionRepository userSessionRepository;

    public void blacklist(String jti, long remainingTtlMillis) {
        if (jti == null || remainingTtlMillis <= 0) return;

        try {
            redisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + jti,
                    "1",
                    remainingTtlMillis,
                    TimeUnit.MILLISECONDS
            );
            log.debug("Blacklisted JTI: {} with TTL: {}ms", jti, remainingTtlMillis);
        } catch (Exception e) {
            log.warn("Redis unavailable, could not blacklist JTI {}: {}", jti, e.getMessage());
        }
    }

    public boolean isBlacklisted(String jti) {
        if (jti == null) return false;
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti));
        } catch (Exception e) {
            log.warn("Redis unavailable, could not check blacklist for JTI {}: {}", jti, e.getMessage());
            return false;
        }
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
}
