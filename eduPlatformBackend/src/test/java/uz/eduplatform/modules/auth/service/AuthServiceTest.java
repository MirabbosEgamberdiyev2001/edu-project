package uz.eduplatform.modules.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import uz.eduplatform.core.audit.AuditService;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.core.common.utils.MessageService;
import uz.eduplatform.core.security.JwtTokenProvider;
import uz.eduplatform.core.security.TokenBlacklistService;
import uz.eduplatform.core.security.TokenPair;
import uz.eduplatform.modules.notification.service.NotificationService;
import uz.eduplatform.modules.auth.domain.Role;
import uz.eduplatform.modules.auth.domain.User;
import uz.eduplatform.modules.auth.domain.UserSession;
import uz.eduplatform.modules.auth.domain.UserStatus;
import uz.eduplatform.modules.auth.dto.*;
import uz.eduplatform.modules.auth.repository.UserRepository;
import uz.eduplatform.modules.auth.repository.UserSessionRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserSessionRepository sessionRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider tokenProvider;
    @Mock private OtpService otpService;
    @Mock private UserService userService;
    @Mock private AuditService auditService;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private MessageService messageService;
    @Mock private NotificationService notificationService;
    @Mock private HttpServletRequest httpRequest;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_storesJtisInSession() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId).email("test@test.com").phone("+998901234567")
                .passwordHash("hash").role(Role.STUDENT).status(UserStatus.ACTIVE)
                .failedLoginAttempts(0).build();

        TokenPair tokenPair = new TokenPair("access", "refresh", "acc-jti", "ref-jti", 900);

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hash")).thenReturn(true);
        when(tokenProvider.generateTokenPair(userId, "test@test.com", "+998901234567", Role.STUDENT))
                .thenReturn(tokenPair);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(sessionRepository.save(any(UserSession.class))).thenAnswer(i -> i.getArgument(0));
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpRequest.getHeader("User-Agent")).thenReturn("Test");
        when(userService.mapToDto(any())).thenReturn(null);

        LoginRequest request = LoginRequest.builder().email("test@test.com").password("password").build();
        LoginResponse response = authService.login(request, httpRequest);

        assertEquals("access", response.getAccessToken());
        assertEquals("refresh", response.getRefreshToken());

        verify(sessionRepository).save(argThat(session ->
                "acc-jti".equals(session.getAccessJti()) && "ref-jti".equals(session.getRefreshJti())));
    }

    @Test
    void refreshToken_withValidToken_rotatesTokens() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId).email("test@test.com").phone("+998901234567")
                .role(Role.STUDENT).status(UserStatus.ACTIVE).build();

        UserSession session = UserSession.builder()
                .userId(userId).refreshToken("old-refresh").accessJti("old-acc-jti")
                .refreshJti("old-ref-jti").isActive(true)
                .expiresAt(LocalDateTime.now().plusDays(1)).build();

        TokenPair newPair = new TokenPair("new-access", "new-refresh", "new-acc-jti", "new-ref-jti", 900);

        when(tokenProvider.validateToken("old-refresh")).thenReturn(true);
        when(tokenProvider.getTokenType("old-refresh")).thenReturn("REFRESH");
        when(tokenProvider.getJtiFromToken("old-refresh")).thenReturn("old-ref-jti");
        when(tokenProvider.getAccessExpiration()).thenReturn(900000L);
        when(tokenProvider.getRefreshExpiration()).thenReturn(604800000L);
        when(sessionRepository.findByRefreshJtiAndIsActiveTrue("old-ref-jti")).thenReturn(Optional.of(session));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenProvider.generateTokenPair(userId, "test@test.com", "+998901234567", Role.STUDENT))
                .thenReturn(newPair);
        when(sessionRepository.save(any(UserSession.class))).thenAnswer(i -> i.getArgument(0));
        when(userService.mapToDto(any())).thenReturn(null);

        LoginResponse response = authService.refreshToken("old-refresh");

        assertEquals("new-access", response.getAccessToken());
        assertEquals("new-refresh", response.getRefreshToken());
        verify(tokenBlacklistService).blacklist("old-acc-jti", 900000L);
        verify(tokenBlacklistService).blacklist("old-ref-jti", 604800000L);
    }

    @Test
    void refreshToken_withReusedToken_revokesAllSessions() {
        UUID userId = UUID.randomUUID();

        when(tokenProvider.validateToken("stolen-refresh")).thenReturn(true);
        when(tokenProvider.getTokenType("stolen-refresh")).thenReturn("REFRESH");
        when(tokenProvider.getJtiFromToken("stolen-refresh")).thenReturn("unknown-jti");
        when(tokenProvider.getUserIdFromToken("stolen-refresh")).thenReturn(userId);
        when(tokenProvider.getRefreshExpiration()).thenReturn(604800000L);
        when(sessionRepository.findByRefreshJtiAndIsActiveTrue("unknown-jti")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> authService.refreshToken("stolen-refresh"));

        verify(tokenBlacklistService).blacklistAllForUser(userId, 604800000L);
        verify(sessionRepository).deactivateAllByUserId(userId);
    }

    @Test
    void logout_blacklistsAccessToken() {
        UUID userId = UUID.randomUUID();
        String accessToken = "access-token";

        when(tokenProvider.getJtiFromToken(accessToken)).thenReturn("acc-jti");
        when(tokenProvider.getExpirationFromToken(accessToken))
                .thenReturn(new java.util.Date(System.currentTimeMillis() + 300000));

        authService.logout(userId, null, accessToken);

        verify(tokenBlacklistService).blacklist(eq("acc-jti"), anyLong());
        verify(tokenBlacklistService).blacklistAllForUser(eq(userId), anyLong());
        verify(sessionRepository).deactivateAllByUserId(userId);
    }

    @Test
    void register_withEmail_autoSendsOtp() {
        RegisterRequest request = RegisterRequest.builder()
                .email("new@test.com").password("Password1")
                .firstName("Test").lastName("User").build();

        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(otpService.generateAndStore("new@test.com", "REGISTER_EMAIL"))
                .thenReturn(new OtpResponse("sent"));
        when(otpService.getStoredOtp("new@test.com")).thenReturn("123456");

        authService.register(request);

        verify(otpService).generateAndStore("new@test.com", "REGISTER_EMAIL");
        verify(otpService).storeRegistrationData(eq("new@test.com"), any(RegisterRequest.class));
        verify(notificationService).sendOtp(eq("new@test.com"), eq("123456"), any());
    }

    @Test
    void register_withPhone_autoSendsOtp() {
        RegisterRequest request = RegisterRequest.builder()
                .phone("+998901234567").password("Password1")
                .firstName("Test").lastName("User").build();

        when(userRepository.existsByPhone("+998901234567")).thenReturn(false);
        when(otpService.generateAndStore("+998901234567", "REGISTER_PHONE"))
                .thenReturn(new OtpResponse("sent"));
        when(otpService.getStoredOtp("+998901234567")).thenReturn("654321");

        authService.register(request);

        verify(otpService).generateAndStore("+998901234567", "REGISTER_PHONE");
        verify(otpService).storeRegistrationData(eq("+998901234567"), any(RegisterRequest.class));
        verify(notificationService).sendOtp(eq("+998901234567"), eq("654321"), any());
    }

    @Test
    void verifyOtp_passwordReset_returnsResetToken() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId).email("test@test.com")
                .role(Role.STUDENT).status(UserStatus.ACTIVE).build();

        OtpRequest request = OtpRequest.builder()
                .email("test@test.com").code("123456").build();

        when(otpService.verify("test@test.com", "123456")).thenReturn("PASSWORD_RESET");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(tokenProvider.generateResetToken(userId)).thenReturn("reset-jwt-token");

        OtpVerifyResponse response = authService.verifyOtp(request);

        assertTrue(response.isVerified());
        assertEquals("reset-jwt-token", response.getResetToken());
        verify(tokenProvider).generateResetToken(userId);
    }

    @Test
    void verifyOtp_verifyEmail_returnsNoResetToken() {
        OtpRequest request = OtpRequest.builder()
                .email("test@test.com").code("123456").build();

        when(otpService.verify("test@test.com", "123456")).thenReturn("VERIFY_EMAIL");

        OtpVerifyResponse response = authService.verifyOtp(request);

        assertTrue(response.isVerified());
        assertNull(response.getResetToken());
        verify(tokenProvider, never()).generateResetToken(any());
    }
}
