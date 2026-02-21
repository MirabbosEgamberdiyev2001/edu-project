package uz.eduplatform.modules.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.eduplatform.core.audit.AuditService;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.core.common.exception.ResourceNotFoundException;
import uz.eduplatform.core.common.utils.MessageService;
import uz.eduplatform.core.security.JwtTokenProvider;
import uz.eduplatform.core.security.TokenBlacklistService;
import uz.eduplatform.core.security.TokenPair;
import uz.eduplatform.modules.auth.domain.Role;
import uz.eduplatform.modules.auth.domain.User;
import uz.eduplatform.modules.auth.domain.UserSession;
import uz.eduplatform.modules.auth.domain.UserStatus;
import uz.eduplatform.modules.auth.dto.*;
import uz.eduplatform.modules.auth.repository.UserRepository;
import uz.eduplatform.modules.auth.repository.UserSessionRepository;
import uz.eduplatform.modules.notification.service.NotificationService;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    @lombok.Getter
    private final JwtTokenProvider tokenProvider;
    private final OtpService otpService;
    private final UserService userService;
    private final AuditService auditService;
    private final TokenBlacklistService tokenBlacklistService;
    private final MessageService messageService;
    private final NotificationService notificationService;
    private final GoogleOAuthService googleOAuthService;
    private final TelegramAuthService telegramAuthService;

    /**
     * Register new user and send OTP for verification
     * User is NOT saved to database until OTP is verified
     * Stores registration data temporarily in OTP cache
     */
    public OtpResponse register(RegisterRequest request) {
        if (request.getEmail() == null && request.getPhone() == null) {
            throw BusinessException.ofKey("auth.email.or.phone.required");
        }

        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw BusinessException.ofKey("auth.email.already.registered");
        }

        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw BusinessException.ofKey("auth.phone.already.registered");
        }

        // Determine identifier and purpose
        String identifier = request.getEmail() != null ? request.getEmail() : request.getPhone();
        String purpose = request.getEmail() != null ? "REGISTER_EMAIL" : "REGISTER_PHONE";

        // Generate and store OTP
        OtpResponse otpResponse = otpService.generateAndStore(identifier, purpose);

        // Store registration data temporarily until OTP is verified
        otpService.storeRegistrationData(identifier, request);

        // Send OTP notification
        String otp = otpService.getStoredOtp(identifier);
        notificationService.sendOtp(identifier, otp, LocaleContextHolder.getLocale());

        log.info("Registration OTP sent to {}", identifier);

        return otpResponse;
    }

    /**
     * Complete registration after OTP verification
     * Called internally after OTP is verified in verifyOtp method
     */
    @Transactional
    public UserDto completeRegistration(RegisterRequest request) {
        User user = User.builder()
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(request.getRole() != null ? request.getRole() : Role.STUDENT)
                .status(UserStatus.ACTIVE) // Directly active since OTP verified
                .emailVerified(request.getEmail() != null)
                .phoneVerified(request.getPhone() != null)
                .passwordChangedAt(LocalDateTime.now())
                .build();

        user = userRepository.save(user);

        auditService.log(user.getId(), user.getRole().name(),
                "USER_REGISTERED", "AUTH", "User", user.getId());

        log.info("User registration completed: {}", user.getId());

        return userService.mapToDto(user);
    }

    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        User user = findUserByIdentifier(request.getEmail(), request.getPhone());

        if (user.isLocked()) {
            throw new LockedException(messageService.get("auth.account.locked"));
        }

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw BusinessException.ofKey("auth.account.blocked");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            user.incrementFailedAttempts();
            userRepository.save(user);

            auditService.log(user.getId(), user.getRole().name(),
                    "LOGIN_FAILED", "AUTH");

            int remaining = 5 - user.getFailedLoginAttempts();
            if (remaining > 0) {
                throw BusinessException.ofKey("auth.invalid.credentials.remaining", remaining);
            } else {
                throw new LockedException(messageService.get("auth.account.locked"));
            }
        }

        user.resetFailedAttempts();
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(getClientIp(httpRequest));
        userRepository.save(user);

        TokenPair tokenPair = tokenProvider.generateTokenPair(
                user.getId(), user.getEmail(), user.getPhone(), user.getRole());

        UserSession session = UserSession.builder()
                .userId(user.getId())
                .refreshToken(tokenPair.refreshToken())
                .accessJti(tokenPair.accessJti())
                .refreshJti(tokenPair.refreshJti())
                .ipAddress(getClientIp(httpRequest))
                .userAgent(httpRequest.getHeader("User-Agent"))
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        sessionRepository.save(session);

        auditService.log(user.getId(), user.getRole().name(),
                "LOGIN_SUCCESS", "AUTH");

        return LoginResponse.builder()
                .accessToken(tokenPair.accessToken())
                .refreshToken(tokenPair.refreshToken())
                .tokenType("Bearer")
                .expiresIn(tokenPair.accessExpiresIn())
                .user(userService.mapToDto(user))
                .build();
    }

    @Transactional
    public LoginResponse refreshToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw BusinessException.ofKey("auth.refresh.token.invalid");
        }

        String tokenType = tokenProvider.getTokenType(refreshToken);
        if (!"REFRESH".equals(tokenType)) {
            throw BusinessException.ofKey("auth.token.type.invalid");
        }

        String refreshJti = tokenProvider.getJtiFromToken(refreshToken);
        UserSession session = sessionRepository.findByRefreshJtiAndIsActiveTrue(refreshJti)
                .orElse(null);

        if (session == null) {
            UUID userId = tokenProvider.getUserIdFromToken(refreshToken);
            log.warn("TOKEN THEFT DETECTED for user: {}. Revoking all sessions.", userId);

            tokenBlacklistService.blacklistAllForUser(userId, tokenProvider.getRefreshExpiration());
            sessionRepository.deactivateAllByUserId(userId);

            auditService.log(userId, null, "TOKEN_THEFT_DETECTED", "AUTH");
            throw BusinessException.ofKey("auth.token.theft.detected");
        }

        if (session.isExpired()) {
            session.setIsActive(false);
            sessionRepository.save(session);
            throw BusinessException.ofKey("auth.refresh.token.expired");
        }

        User user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", session.getUserId()));

        session.setIsActive(false);
        sessionRepository.save(session);

        if (session.getAccessJti() != null) {
            tokenBlacklistService.blacklist(session.getAccessJti(), tokenProvider.getAccessExpiration());
        }
        if (session.getRefreshJti() != null) {
            tokenBlacklistService.blacklist(session.getRefreshJti(), tokenProvider.getRefreshExpiration());
        }

        TokenPair tokenPair = tokenProvider.generateTokenPair(
                user.getId(), user.getEmail(), user.getPhone(), user.getRole());

        UserSession newSession = UserSession.builder()
                .userId(user.getId())
                .refreshToken(tokenPair.refreshToken())
                .accessJti(tokenPair.accessJti())
                .refreshJti(tokenPair.refreshJti())
                .ipAddress(session.getIpAddress())
                .userAgent(session.getUserAgent())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        sessionRepository.save(newSession);

        return LoginResponse.builder()
                .accessToken(tokenPair.accessToken())
                .refreshToken(tokenPair.refreshToken())
                .tokenType("Bearer")
                .expiresIn(tokenPair.accessExpiresIn())
                .user(userService.mapToDto(user))
                .build();
    }

    @Transactional
    public void logout(UUID userId, String refreshToken, String accessToken) {
        if (accessToken != null) {
            try {
                String accessJti = tokenProvider.getJtiFromToken(accessToken);
                Date expiration = tokenProvider.getExpirationFromToken(accessToken);
                long remainingTtl = expiration.getTime() - System.currentTimeMillis();
                if (remainingTtl > 0) {
                    tokenBlacklistService.blacklist(accessJti, remainingTtl);
                }
            } catch (Exception e) {
                log.warn("Could not blacklist access token on logout: {}", e.getMessage());
            }
        }

        if (refreshToken != null) {
            sessionRepository.findByRefreshTokenAndIsActiveTrue(refreshToken).ifPresent(session -> {
                session.setIsActive(false);
                sessionRepository.save(session);
                if (session.getRefreshJti() != null) {
                    tokenBlacklistService.blacklist(session.getRefreshJti(), tokenProvider.getRefreshExpiration());
                }
            });
        } else {
            tokenBlacklistService.blacklistAllForUser(userId, tokenProvider.getAccessExpiration());
            sessionRepository.deactivateAllByUserId(userId);
        }

        auditService.log(userId, null, "LOGOUT", "AUTH");
    }

    /**
     * Verify OTP - backend automatically determines the purpose.
     * Frontend only sends: email/phone + code
     */
    @Transactional
    public OtpVerifyResponse verifyOtp(OtpRequest request) {
        String identifier = request.getEmail() != null ? request.getEmail() : request.getPhone();
        if (identifier == null) {
            throw BusinessException.ofKey("auth.email.or.phone.required");
        }

        String purpose = otpService.verify(identifier, request.getCode());

        // Handle REGISTER purposes - complete registration and auto-login
        if ("REGISTER_EMAIL".equals(purpose) || "REGISTER_PHONE".equals(purpose)) {
            RegisterRequest registrationData = otpService.getRegistrationData(identifier);
            if (registrationData == null) {
                throw BusinessException.ofKey("auth.registration.data.expired");
            }

            UserDto regUser = completeRegistration(registrationData);
            otpService.clearRegistrationData(identifier);

            // Auto-login: generate tokens for the newly registered user
            User newUser = userRepository.findById(regUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", regUser.getId()));

            TokenPair tokenPair = tokenProvider.generateTokenPair(
                    newUser.getId(), newUser.getEmail(), newUser.getPhone(), newUser.getRole());

            UserSession session = UserSession.builder()
                    .userId(newUser.getId())
                    .refreshToken(tokenPair.refreshToken())
                    .accessJti(tokenPair.accessJti())
                    .refreshJti(tokenPair.refreshJti())
                    .ipAddress("registration")
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();
            sessionRepository.save(session);

            return OtpVerifyResponse.builder()
                    .verified(true)
                    .registeredUser(regUser)
                    .accessToken(tokenPair.accessToken())
                    .refreshToken(tokenPair.refreshToken())
                    .tokenType("Bearer")
                    .expiresIn(tokenPair.accessExpiresIn())
                    .build();
        }

        if ("PASSWORD_RESET".equals(purpose)) {
            User user = findUserByIdentifier(request.getEmail(), request.getPhone());
            String resetToken = tokenProvider.generateResetToken(user.getId());

            return OtpVerifyResponse.builder()
                    .verified(true)
                    .resetToken(resetToken)
                    .build();
        }

        return OtpVerifyResponse.builder()
                .verified(true)
                .build();
    }

    @Transactional
    public OtpResponse forgotPassword(ForgotPasswordRequest request) {
        String identifier = request.getEmail() != null ? request.getEmail() : request.getPhone();
        if (identifier == null) {
            throw BusinessException.ofKey("auth.email.or.phone.required");
        }

        User user = findUserByIdentifier(request.getEmail(), request.getPhone());

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw BusinessException.ofKey("auth.account.blocked");
        }

        OtpResponse otpResponse = otpService.generateAndStore(identifier, "PASSWORD_RESET");
        String otp = otpService.getStoredOtp(identifier);

        notificationService.sendPasswordResetOtp(identifier, otp, LocaleContextHolder.getLocale());

        auditService.log(user.getId(), user.getRole().name(),
                "PASSWORD_RESET_REQUESTED", "AUTH");

        log.info("Password reset OTP sent to {}", identifier);
        return otpResponse;
    }

    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        if (!tokenProvider.validateToken(request.getToken())) {
            throw BusinessException.ofKey("auth.reset.token.invalid");
        }

        String tokenType = tokenProvider.getTokenType(request.getToken());
        if (!"RESET".equals(tokenType)) {
            throw BusinessException.ofKey("auth.reset.token.invalid");
        }

        UUID userId = tokenProvider.getUserIdFromToken(request.getToken());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        tokenBlacklistService.blacklistAllForUser(userId, tokenProvider.getAccessExpiration());
        sessionRepository.deactivateAllByUserId(userId);

        auditService.log(user.getId(), user.getRole().name(),
                "PASSWORD_RESET_COMPLETED", "AUTH");
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw BusinessException.ofKey("auth.password.change.wrong");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        // Invalidate all other sessions
        tokenBlacklistService.blacklistAllForUser(userId, tokenProvider.getAccessExpiration());
        sessionRepository.deactivateAllByUserId(userId);

        auditService.log(userId, user.getRole().name(),
                "PASSWORD_CHANGED", "AUTH");
    }

    @Transactional
    public LoginResponse googleAuth(GoogleAuthRequest request, HttpServletRequest httpRequest) {
        GoogleOAuthService.GoogleUserInfo google;
        if (request.getAccessToken() != null && !request.getAccessToken().isBlank()) {
            google = googleOAuthService.verifyAccessToken(request.getAccessToken());
        } else if (request.getIdToken() != null && !request.getIdToken().isBlank()) {
            google = googleOAuthService.verifyIdToken(request.getIdToken());
        } else {
            throw BusinessException.ofKey("auth.google.token.required");
        }

        if (google.getEmail() == null || !Boolean.TRUE.equals(google.getEmailVerified())) {
            throw BusinessException.ofKey("auth.google.email.not.verified");
        }

        User user = userRepository.findByGoogleId(google.getId())
                .orElseGet(() -> linkOrCreateGoogleUser(google));

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw BusinessException.ofKey("auth.account.blocked");
        }

        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(getClientIp(httpRequest));
        user.resetFailedAttempts();
        userRepository.save(user);

        TokenPair tokenPair = tokenProvider.generateTokenPair(
                user.getId(), user.getEmail(), user.getPhone(), user.getRole());

        UserSession session = UserSession.builder()
                .userId(user.getId())
                .refreshToken(tokenPair.refreshToken())
                .accessJti(tokenPair.accessJti())
                .refreshJti(tokenPair.refreshJti())
                .ipAddress(getClientIp(httpRequest))
                .userAgent(httpRequest.getHeader("User-Agent"))
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        sessionRepository.save(session);

        auditService.log(user.getId(), user.getRole().name(), "GOOGLE_LOGIN", "AUTH");

        return LoginResponse.builder()
                .accessToken(tokenPair.accessToken())
                .refreshToken(tokenPair.refreshToken())
                .tokenType("Bearer")
                .expiresIn(tokenPair.accessExpiresIn())
                .user(userService.mapToDto(user))
                .build();
    }

    private User linkOrCreateGoogleUser(GoogleOAuthService.GoogleUserInfo google) {
        // Try to link to existing user by email
        User existing = userRepository.findByEmail(google.getEmail()).orElse(null);
        if (existing != null) {
            existing.setGoogleId(google.getId());
            existing.setEmailVerified(true);
            if (existing.getAvatarUrl() == null && google.getPicture() != null) {
                existing.setAvatarUrl(google.getPicture());
            }
            return userRepository.save(existing);
        }

        // Create new user
        User user = User.builder()
                .email(google.getEmail())
                .googleId(google.getId())
                .firstName(google.getGivenName() != null ? google.getGivenName() : google.getName())
                .lastName(google.getFamilyName() != null ? google.getFamilyName() : "")
                .avatarUrl(google.getPicture())
                .emailVerified(true)
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .build();
        user = userRepository.save(user);

        auditService.log(user.getId(), user.getRole().name(),
                "USER_REGISTERED_GOOGLE", "AUTH", "User", user.getId());

        return user;
    }

    @Transactional
    public LoginResponse telegramAuth(TelegramAuthRequest request, HttpServletRequest httpRequest) {
        TelegramAuthService.TelegramAuthData authData = new TelegramAuthService.TelegramAuthData();
        authData.setId(request.getId());
        authData.setFirstName(request.getFirstName());
        authData.setLastName(request.getLastName());
        authData.setUsername(request.getUsername());
        authData.setPhotoUrl(request.getPhotoUrl());
        authData.setAuthDate(request.getAuthDate());
        authData.setHash(request.getHash());

        telegramAuthService.verifyAuthData(authData);

        String telegramId = String.valueOf(request.getId());
        User user = userRepository.findByTelegramId(telegramId)
                .orElseGet(() -> createTelegramUser(request));

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw BusinessException.ofKey("auth.account.blocked");
        }

        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(getClientIp(httpRequest));
        user.resetFailedAttempts();
        userRepository.save(user);

        TokenPair tokenPair = tokenProvider.generateTokenPair(
                user.getId(), user.getEmail(), user.getPhone(), user.getRole());

        UserSession session = UserSession.builder()
                .userId(user.getId())
                .refreshToken(tokenPair.refreshToken())
                .accessJti(tokenPair.accessJti())
                .refreshJti(tokenPair.refreshJti())
                .ipAddress(getClientIp(httpRequest))
                .userAgent(httpRequest.getHeader("User-Agent"))
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        sessionRepository.save(session);

        auditService.log(user.getId(), user.getRole().name(), "TELEGRAM_LOGIN", "AUTH");

        return LoginResponse.builder()
                .accessToken(tokenPair.accessToken())
                .refreshToken(tokenPair.refreshToken())
                .tokenType("Bearer")
                .expiresIn(tokenPair.accessExpiresIn())
                .user(userService.mapToDto(user))
                .build();
    }

    private User createTelegramUser(TelegramAuthRequest request) {
        User user = User.builder()
                .telegramId(String.valueOf(request.getId()))
                .firstName(request.getFirstName() != null ? request.getFirstName() : "Telegram")
                .lastName(request.getLastName() != null ? request.getLastName() : "User")
                .avatarUrl(request.getPhotoUrl())
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .build();
        user = userRepository.save(user);

        auditService.log(user.getId(), user.getRole().name(),
                "USER_REGISTERED_TELEGRAM", "AUTH", "User", user.getId());

        return user;
    }

    public java.util.List<SessionDto> getActiveSessions(UUID userId, String currentAccessJti) {
        java.util.List<UserSession> sessions = sessionRepository.findByUserIdAndIsActiveTrue(userId);
        return sessions.stream()
                .filter(s -> !s.isExpired())
                .map(s -> SessionDto.builder()
                        .id(s.getId())
                        .ipAddress(s.getIpAddress())
                        .userAgent(s.getUserAgent())
                        .createdAt(s.getCreatedAt())
                        .lastUsedAt(s.getLastUsedAt())
                        .expiresAt(s.getExpiresAt())
                        .current(currentAccessJti != null && currentAccessJti.equals(s.getAccessJti()))
                        .build())
                .toList();
    }

    @Transactional
    public void revokeSession(UUID userId, UUID sessionId) {
        UserSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", "id", sessionId));

        if (!session.getUserId().equals(userId)) {
            throw BusinessException.ofKey("error.access.denied");
        }

        session.setIsActive(false);
        sessionRepository.save(session);

        if (session.getAccessJti() != null) {
            tokenBlacklistService.blacklist(session.getAccessJti(), tokenProvider.getAccessExpiration());
        }
        if (session.getRefreshJti() != null) {
            tokenBlacklistService.blacklist(session.getRefreshJti(), tokenProvider.getRefreshExpiration());
        }

        auditService.log(userId, null, "SESSION_REVOKED", "AUTH",
                "UserSession", sessionId);
    }

    @Transactional
    public void revokeAllOtherSessions(UUID userId, String currentAccessJti) {
        java.util.List<UserSession> sessions = sessionRepository.findByUserIdAndIsActiveTrue(userId);
        for (UserSession session : sessions) {
            if (currentAccessJti != null && currentAccessJti.equals(session.getAccessJti())) {
                continue; // skip current session
            }
            session.setIsActive(false);
            sessionRepository.save(session);
            if (session.getAccessJti() != null) {
                tokenBlacklistService.blacklist(session.getAccessJti(), tokenProvider.getAccessExpiration());
            }
            if (session.getRefreshJti() != null) {
                tokenBlacklistService.blacklist(session.getRefreshJti(), tokenProvider.getRefreshExpiration());
            }
        }

        auditService.log(userId, null, "ALL_OTHER_SESSIONS_REVOKED", "AUTH");
    }

    private User findUserByIdentifier(String email, String phone) {
        if (email != null) {
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        }
        if (phone != null) {
            return userRepository.findByPhone(phone)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "phone", phone));
        }
        throw BusinessException.ofKey("auth.email.or.phone.required");
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}