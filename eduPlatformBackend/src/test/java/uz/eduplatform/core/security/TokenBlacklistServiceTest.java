package uz.eduplatform.core.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uz.eduplatform.modules.auth.domain.UserSession;
import uz.eduplatform.modules.auth.repository.UserSessionRepository;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private UserSessionRepository userSessionRepository;

    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        tokenBlacklistService = new TokenBlacklistService(userSessionRepository);
    }

    @Test
    void blacklist_storesKey() {
        String jti = "test-jti-123";
        long ttl = 900000;

        tokenBlacklistService.blacklist(jti, ttl);

        assertTrue(tokenBlacklistService.isBlacklisted(jti));
    }

    @Test
    void blacklist_ignoresNullJti() {
        tokenBlacklistService.blacklist(null, 900000);
        assertFalse(tokenBlacklistService.isBlacklisted(null));
    }

    @Test
    void blacklist_ignoresNonPositiveTtl() {
        tokenBlacklistService.blacklist("jti", 0);
        assertFalse(tokenBlacklistService.isBlacklisted("jti"));
    }

    @Test
    void isBlacklisted_returnsTrueWhenKeyExists() {
        tokenBlacklistService.blacklist("test-jti", 900000);
        assertTrue(tokenBlacklistService.isBlacklisted("test-jti"));
    }

    @Test
    void isBlacklisted_returnsFalseWhenKeyDoesNotExist() {
        assertFalse(tokenBlacklistService.isBlacklisted("test-jti"));
    }

    @Test
    void isBlacklisted_returnsFalseForNullJti() {
        assertFalse(tokenBlacklistService.isBlacklisted(null));
    }

    @Test
    void isBlacklisted_returnsFalseAfterExpiry() {
        tokenBlacklistService.blacklist("expired-jti", 1);

        // Wait for token to expire
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}

        assertFalse(tokenBlacklistService.isBlacklisted("expired-jti"));
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

        tokenBlacklistService.blacklistAllForUser(userId, 900000);

        assertTrue(tokenBlacklistService.isBlacklisted("access-1"));
        assertTrue(tokenBlacklistService.isBlacklisted("refresh-1"));
        assertTrue(tokenBlacklistService.isBlacklisted("access-2"));
        assertTrue(tokenBlacklistService.isBlacklisted("refresh-2"));
    }

    @Test
    void cleanupExpired_removesExpiredTokens() {
        tokenBlacklistService.blacklist("expired-1", 1);
        tokenBlacklistService.blacklist("valid-1", 900000);

        // Wait for expired token
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}

        tokenBlacklistService.cleanupExpired();

        assertFalse(tokenBlacklistService.isBlacklisted("expired-1"));
        assertTrue(tokenBlacklistService.isBlacklisted("valid-1"));
    }
}
