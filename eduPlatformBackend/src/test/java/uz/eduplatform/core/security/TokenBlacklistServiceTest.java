package uz.eduplatform.core.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import uz.eduplatform.modules.auth.domain.UserSession;
import uz.eduplatform.modules.auth.repository.UserSessionRepository;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        tokenBlacklistService = new TokenBlacklistService(redisTemplate, userSessionRepository);
    }

    @Test
    void blacklist_storesKey() {
        String jti = "test-jti-123";
        long ttl = 900000;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        tokenBlacklistService.blacklist(jti, ttl);

        verify(valueOperations).set("bl:" + jti, "1", ttl, TimeUnit.MILLISECONDS);
    }

    @Test
    void blacklist_ignoresNullJti() {
        tokenBlacklistService.blacklist(null, 900000);

        verifyNoInteractions(valueOperations);
    }

    @Test
    void blacklist_ignoresNonPositiveTtl() {
        tokenBlacklistService.blacklist("jti", 0);

        verifyNoInteractions(valueOperations);
    }

    @Test
    void isBlacklisted_returnsTrueWhenKeyExists() {
        when(redisTemplate.hasKey("bl:test-jti")).thenReturn(true);

        assertTrue(tokenBlacklistService.isBlacklisted("test-jti"));
    }

    @Test
    void isBlacklisted_returnsFalseWhenKeyDoesNotExist() {
        when(redisTemplate.hasKey("bl:test-jti")).thenReturn(false);

        assertFalse(tokenBlacklistService.isBlacklisted("test-jti"));
    }

    @Test
    void isBlacklisted_returnsFalseForNullJti() {
        assertFalse(tokenBlacklistService.isBlacklisted(null));
    }

    @Test
    void blacklistAllForUser_blacklistsAllSessionJtis() {
        UUID userId = UUID.randomUUID();
        UserSession session1 = UserSession.builder()
                .accessJti("access-1").refreshJti("refresh-1").build();
        UserSession session2 = UserSession.builder()
                .accessJti("access-2").refreshJti("refresh-2").build();

        when(userSessionRepository.findByUserIdAndIsActiveTrue(userId))
                .thenReturn(List.of(session1, session2));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        tokenBlacklistService.blacklistAllForUser(userId, 900000);

        verify(valueOperations).set("bl:access-1", "1", 900000, TimeUnit.MILLISECONDS);
        verify(valueOperations).set("bl:refresh-1", "1", 900000, TimeUnit.MILLISECONDS);
        verify(valueOperations).set("bl:access-2", "1", 900000, TimeUnit.MILLISECONDS);
        verify(valueOperations).set("bl:refresh-2", "1", 900000, TimeUnit.MILLISECONDS);
    }
}
