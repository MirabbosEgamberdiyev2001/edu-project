package uz.eduplatform.core.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uz.eduplatform.modules.auth.domain.Role;

import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "jwtSecret",
                "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha!!");
        ReflectionTestUtils.setField(tokenProvider, "accessExpiration", 900000L);
        ReflectionTestUtils.setField(tokenProvider, "refreshExpiration", 604800000L);
        tokenProvider.init();
    }

    @Test
    void generateTokenPair_containsJtis() {
        UUID userId = UUID.randomUUID();
        TokenPair pair = tokenProvider.generateTokenPair(userId, "test@test.com", "+998901234567", Role.STUDENT);

        assertNotNull(pair.accessJti());
        assertNotNull(pair.refreshJti());
        assertNotEquals(pair.accessJti(), pair.refreshJti());
    }

    @Test
    void generateTokenPair_tokensAreValid() {
        UUID userId = UUID.randomUUID();
        TokenPair pair = tokenProvider.generateTokenPair(userId, "test@test.com", "+998901234567", Role.STUDENT);

        assertTrue(tokenProvider.validateToken(pair.accessToken()));
        assertTrue(tokenProvider.validateToken(pair.refreshToken()));
    }

    @Test
    void getJtiFromToken_extractsCorrectJti() {
        UUID userId = UUID.randomUUID();
        TokenPair pair = tokenProvider.generateTokenPair(userId, "test@test.com", "+998901234567", Role.STUDENT);

        String accessJti = tokenProvider.getJtiFromToken(pair.accessToken());
        String refreshJti = tokenProvider.getJtiFromToken(pair.refreshToken());

        assertEquals(pair.accessJti(), accessJti);
        assertEquals(pair.refreshJti(), refreshJti);
    }

    @Test
    void getUserIdFromToken_extractsCorrectUserId() {
        UUID userId = UUID.randomUUID();
        TokenPair pair = tokenProvider.generateTokenPair(userId, "test@test.com", "+998901234567", Role.STUDENT);

        assertEquals(userId, tokenProvider.getUserIdFromToken(pair.accessToken()));
        assertEquals(userId, tokenProvider.getUserIdFromToken(pair.refreshToken()));
    }

    @Test
    void getTokenType_returnsCorrectType() {
        UUID userId = UUID.randomUUID();
        TokenPair pair = tokenProvider.generateTokenPair(userId, "test@test.com", "+998901234567", Role.STUDENT);

        assertEquals("ACCESS", tokenProvider.getTokenType(pair.accessToken()));
        assertEquals("REFRESH", tokenProvider.getTokenType(pair.refreshToken()));
    }

    @Test
    void getExpirationFromToken_returnsNonNullDate() {
        UUID userId = UUID.randomUUID();
        TokenPair pair = tokenProvider.generateTokenPair(userId, "test@test.com", "+998901234567", Role.STUDENT);

        Date expiration = tokenProvider.getExpirationFromToken(pair.accessToken());
        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }

    @Test
    void validateToken_returnsFalseForInvalidToken() {
        assertFalse(tokenProvider.validateToken("invalid.token.here"));
    }

    @Test
    void validateToken_returnsFalseForEmptyToken() {
        assertFalse(tokenProvider.validateToken(""));
    }

    @Test
    void accessExpiresIn_returnsCorrectValue() {
        UUID userId = UUID.randomUUID();
        TokenPair pair = tokenProvider.generateTokenPair(userId, "test@test.com", "+998901234567", Role.STUDENT);

        assertEquals(900, pair.accessExpiresIn());
    }
}
