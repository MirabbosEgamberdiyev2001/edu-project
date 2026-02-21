package uz.eduplatform.modules.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import uz.eduplatform.core.common.utils.MessageService;
import uz.eduplatform.core.i18n.AcceptLanguage;
import uz.eduplatform.core.security.UserPrincipal;
import uz.eduplatform.modules.auth.dto.*;
import uz.eduplatform.modules.auth.service.AuthService;
import uz.eduplatform.modules.auth.service.UserService;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autentifikatsiya", description = "Ro'yxatdan o'tish, kirish, chiqish, parol tiklash, OAuth va sessiya boshqaruvi API'lari")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final MessageService messageService;

    @PostMapping("/register")
    @Operation(
            summary = "Yangi user ro'yxatdan o'tkazish",
            description = """
            Email yoki telefon orqali ro'yxatdan o'tish.
            
            **Jarayon:**
            - Email/telefon va parol validatsiya qilinadi
            - 6 raqamli OTP kodi generatsiya qilinadi
            - Registration ma'lumotlari 5 daqiqaga cache'da saqlanadi
            - OTP email yoki SMS orqali yuboriladi
            - Keyingi qadam: `/otp/verify` endpoint'iga OTP yuborish
            
            **Muhim:** User faqat OTP tasdiqlangandan keyin bazaga saqlanadi!
            
            **Rate limit:** 3 ta so'rov / 5 daqiqa
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OTP muvaffaqiyatli yuborildi"),
            @ApiResponse(responseCode = "400", description = "Email/telefon allaqachon ro'yxatdan o'tgan"),
            @ApiResponse(responseCode = "429", description = "Rate limit oshib ketdi")
    })
    public ResponseEntity<uz.eduplatform.core.common.dto.ApiResponse<OtpResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {
        OtpResponse response = authService.register(request);
        return ResponseEntity.ok(uz.eduplatform.core.common.dto.ApiResponse.success(response));
    }

    @PostMapping("/register/phone")
    @Operation(
            summary = "Telefon orqali ro'yxatdan o'tish",
            description = """
            Telefon raqam orqali ro'yxatdan o'tish.

            **Jarayon:**
            - Telefon raqam va parol validatsiya qilinadi
            - 6 raqamli OTP kodi generatsiya qilinadi va SMS orqali yuboriladi
            - Keyingi qadam: `/otp/verify` endpoint'iga OTP yuborish

            **Rate limit:** 3 ta so'rov / 5 daqiqa
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OTP muvaffaqiyatli yuborildi"),
            @ApiResponse(responseCode = "400", description = "Telefon allaqachon ro'yxatdan o'tgan"),
            @ApiResponse(responseCode = "429", description = "Rate limit oshib ketdi")
    })
    public ResponseEntity<uz.eduplatform.core.common.dto.ApiResponse<OtpResponse>> registerByPhone(
            @Valid @RequestBody RegisterRequest request,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {
        if (request.getPhone() == null || request.getPhone().isBlank()) {
            throw uz.eduplatform.core.common.exception.BusinessException.ofKey("auth.validation.phone.required");
        }
        request.setEmail(null);
        OtpResponse response = authService.register(request);
        return ResponseEntity.ok(uz.eduplatform.core.common.dto.ApiResponse.success(response));
    }

    @PostMapping("/login")
    @Operation(
            summary = "Tizimga kirish",
            description = """
            Email/telefon va parol orqali kirish.
            
            **Xavfsizlik:**
            - 5 marta noto'g'ri parol = 15 daqiqaga bloklanadi
            - IP address va device info saqlanadi
            - Access token (15 min) va Refresh token (7 kun) qaytariladi
            
            **Token'lardan foydalanish:**
            ```
            Authorization: Bearer {accessToken}
            ```
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Muvaffaqiyatli kirdi"),
            @ApiResponse(responseCode = "401", description = "Email/parol noto'g'ri"),
            @ApiResponse(responseCode = "423", description = "Account bloklangan")
    })
    public ResponseEntity<uz.eduplatform.core.common.dto.ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {
        LoginResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(uz.eduplatform.core.common.dto.ApiResponse.success(response, messageService.get("auth.login.success")));
    }

    @PostMapping("/otp/verify")
    @Operation(
            summary = "OTP kodni tasdiqlash",
            description = """
            OTP kodni tekshirish. Backend avtomatik aniqlaydi:
            - Ro'yxatdan o'tish uchun → User yaratiladi + token qaytariladi
            - Parol tiklash uchun → resetToken qaytariladi

            **Faqat email/telefon va OTP kodi kerak!**

            **Rate limit:** 5 ta urinish / 5 daqiqa
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OTP tasdiqlandi"),
            @ApiResponse(responseCode = "400", description = "OTP noto'g'ri yoki muddati tugagan"),
            @ApiResponse(responseCode = "429", description = "Juda ko'p urinishlar")
    })
    public ResponseEntity<uz.eduplatform.core.common.dto.ApiResponse<OtpVerifyResponse>> verifyOtp(
            @Valid @RequestBody OtpRequest request,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {
        OtpVerifyResponse result = authService.verifyOtp(request);
        return ResponseEntity.ok(uz.eduplatform.core.common.dto.ApiResponse.success(result, messageService.get("auth.otp.verify.success")));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Access token yangilash",
            description = """
            Refresh token yordamida yangi access token olish.
            
            **Token Rotation:** Har safar yangi access va refresh token qaytadi.
            
            **Xavfsizlik:** Agar token o'g'irlangan deb aniqlansa, barcha sessionlar bekor qilinadi.
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token yangilandi"),
            @ApiResponse(responseCode = "401", description = "Refresh token noto'g'ri"),
            @ApiResponse(responseCode = "403", description = "Token o'g'irligi aniqlandi")
    })
    public ResponseEntity<uz.eduplatform.core.common.dto.ApiResponse<LoginResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {
        LoginResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(uz.eduplatform.core.common.dto.ApiResponse.success(response));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Tizimdan chiqish",
            description = """
            Account'dan chiqish va tokenlarni bekor qilish.
            
            **Bitta qurilmadan chiqish:** Refresh token yuboring
            **Barcha qurilmalardan chiqish:** Refresh token yubormasdan so'rov yuboring
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Muvaffaqiyatli chiqdi"),
            @ApiResponse(responseCode = "401", description = "Autentifikatsiya talab qilinadi")
    })
    public ResponseEntity<uz.eduplatform.core.common.dto.ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest httpRequest,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        String accessToken = extractAccessToken(httpRequest);
        authService.logout(
                principal.getId(),
                request != null ? request.getRefreshToken() : null,
                accessToken
        );
        return ResponseEntity.ok(uz.eduplatform.core.common.dto.ApiResponse.success(null, messageService.get("auth.logout.success")));
    }

    @PostMapping("/password/forgot")
    @Operation(
            summary = "Parolni unutdim",
            description = """
            Parolni tiklash jarayonini boshlash.
            
            **Jarayon:**
            1. Bu endpoint'ga email/telefon yuboring
            2. OTP kodi yuboriladi
            3. `/otp/verify` ga OTP yuboring (purpose: PASSWORD_RESET)
            4. resetToken oling
            5. `/password/reset` ga resetToken va yangi parol yuboring
            
            **Rate limit:** 3 ta so'rov / 5 daqiqa
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OTP yuborildi"),
            @ApiResponse(responseCode = "404", description = "User topilmadi"),
            @ApiResponse(responseCode = "429", description = "Rate limit oshdi")
    })
    public ResponseEntity<uz.eduplatform.core.common.dto.ApiResponse<OtpResponse>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {
        OtpResponse result = authService.forgotPassword(request);
        return ResponseEntity.ok(uz.eduplatform.core.common.dto.ApiResponse.success(result));
    }

    @PostMapping("/password/reset")
    @Operation(
            summary = "Parolni tiklash",
            description = """
            Reset token bilan parolni yangilash.
            
            **Xavfsizlik:** Parol o'zgargach barcha qurilmalardan chiqariladi.
            
            **Parol talablari:**
            - Kamida 8 ta belgi
            - Bitta katta harf
            - Bitta kichik harf
            - Bitta raqam
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Parol yangilandi"),
            @ApiResponse(responseCode = "400", description = "Reset token noto'g'ri"),
            @ApiResponse(responseCode = "422", description = "Parol talablariga javob bermaydi")
    })
    public ResponseEntity<uz.eduplatform.core.common.dto.ApiResponse<Void>> resetPassword(
            @Valid @RequestBody PasswordResetRequest request,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {
        authService.resetPassword(request);
        return ResponseEntity.ok(uz.eduplatform.core.common.dto.ApiResponse.success(null, messageService.get("auth.password.reset.success")));
    }

    @PostMapping("/password/change")
    @Operation(
            summary = "Parolni o'zgartirish",
            description = """
            Autentifikatsiya qilingan foydalanuvchi parolini o'zgartirish.

            **Xavfsizlik:** Parol o'zgargach barcha boshqa qurilmalardan chiqariladi.

            **Parol talablari:**
            - Kamida 8 ta belgi
            - Bitta katta harf
            - Bitta kichik harf
            - Bitta raqam
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Parol muvaffaqiyatli o'zgartirildi"),
            @ApiResponse(responseCode = "400", description = "Joriy parol noto'g'ri"),
            @ApiResponse(responseCode = "401", description = "Autentifikatsiya talab qilinadi")
    })
    public ResponseEntity<uz.eduplatform.core.common.dto.ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {
        authService.changePassword(principal.getId(), request);
        return ResponseEntity.ok(uz.eduplatform.core.common.dto.ApiResponse.success(null, messageService.get("auth.password.change.success")));
    }

    @GetMapping("/me")
    @Operation(
            summary = "Joriy foydalanuvchi ma'lumotlari",
            description = "Autentifikatsiya qilingan foydalanuvchining profil ma'lumotlarini olish."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profil ma'lumotlari", content = @Content(schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "401", description = "Autentifikatsiya talab qilinadi")
    })
    public ResponseEntity<uz.eduplatform.core.common.dto.ApiResponse<UserDto>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {
        UserDto user = userService.getUserById(principal.getId());
        return ResponseEntity.ok(uz.eduplatform.core.common.dto.ApiResponse.success(user));
    }

    @PutMapping("/me")
    @Operation(
            summary = "Profilni yangilash",
            description = """
            Foydalanuvchi profil ma'lumotlarini yangilash.
            
            **Yangilanishi mumkin:**
            - firstName, lastName
            - avatarUrl
            - locale (til)
            - timezone (vaqt zonasi)
            
            **Yangilanmaydi:** email, phone, password, role, status
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profil yangilandi", content = @Content(schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "400", description = "Validatsiya xatosi"),
            @ApiResponse(responseCode = "401", description = "Autentifikatsiya talab qilinadi")
    })
    public ResponseEntity<uz.eduplatform.core.common.dto.ApiResponse<UserDto>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {
        UserDto user = userService.updateProfile(principal.getId(), request);
        return ResponseEntity.ok(uz.eduplatform.core.common.dto.ApiResponse.success(user, messageService.get("auth.profile.update.success")));
    }

    @PostMapping("/google")
    @Operation(
            summary = "Google orqali kirish",
            description = """
            Google OAuth2 orqali tizimga kirish yoki ro'yxatdan o'tish.

            **Jarayon:**
            - Frontend Google Sign-In SDK dan idToken yoki accessToken oladi
            - Bu endpoint ga yuboriladi
            - Backend Google bilan tekshiradi
            - User avtomatik yaratiladi yoki mavjud user bilan bog'lanadi
            """
    )
    public ResponseEntity<uz.eduplatform.core.common.dto.ApiResponse<LoginResponse>> googleAuth(
            @RequestBody GoogleAuthRequest request,
            HttpServletRequest httpRequest,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {
        LoginResponse response = authService.googleAuth(request, httpRequest);
        return ResponseEntity.ok(uz.eduplatform.core.common.dto.ApiResponse.success(response, messageService.get("auth.login.success")));
    }

    @PostMapping("/telegram")
    @Operation(
            summary = "Telegram orqali kirish",
            description = """
            Telegram Login Widget orqali tizimga kirish yoki ro'yxatdan o'tish.

            **Jarayon:**
            - Frontend Telegram Login Widget dan auth data oladi
            - Bu endpoint ga yuboriladi (id, first_name, last_name, username, photo_url, auth_date, hash)
            - Backend Telegram bot token orqali hash tekshiradi
            - User avtomatik yaratiladi yoki mavjud user bilan bog'lanadi
            """
    )
    public ResponseEntity<uz.eduplatform.core.common.dto.ApiResponse<LoginResponse>> telegramAuth(
            @RequestBody TelegramAuthRequest request,
            HttpServletRequest httpRequest,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {
        LoginResponse response = authService.telegramAuth(request, httpRequest);
        return ResponseEntity.ok(uz.eduplatform.core.common.dto.ApiResponse.success(response, messageService.get("auth.login.success")));
    }

    @GetMapping("/sessions")
    @Operation(summary = "Faol sessiyalar ro'yxati", description = "Foydalanuvchining barcha faol sessiyalarini olish — IP manzil, qurilma, yaratilgan sana. Joriy sessiya alohida belgilanadi.")
    public ResponseEntity<uz.eduplatform.core.common.dto.ApiResponse<java.util.List<SessionDto>>> getActiveSessions(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {
        String accessToken = extractAccessToken(httpRequest);
        String currentJti = null;
        if (accessToken != null) {
            try {
                currentJti = authService.getTokenProvider().getJtiFromToken(accessToken);
            } catch (Exception ignored) {}
        }
        java.util.List<SessionDto> sessions = authService.getActiveSessions(principal.getId(), currentJti);
        return ResponseEntity.ok(uz.eduplatform.core.common.dto.ApiResponse.success(sessions));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "Sessiyani bekor qilish", description = "Berilgan ID bo'yicha bitta sessiyani tugatish. Foydalanuvchi o'sha qurilmadan chiqariladi.")
    public ResponseEntity<uz.eduplatform.core.common.dto.ApiResponse<Void>> revokeSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable java.util.UUID sessionId) {
        authService.revokeSession(principal.getId(), sessionId);
        return ResponseEntity.ok(uz.eduplatform.core.common.dto.ApiResponse.success(null));
    }

    @DeleteMapping("/sessions")
    @Operation(summary = "Barcha boshqa sessiyalarni bekor qilish", description = "Joriy sessiyadan tashqari barcha faol sessiyalarni tugatish. Boshqa barcha qurilmalardan chiqariladi.")
    public ResponseEntity<uz.eduplatform.core.common.dto.ApiResponse<Void>> revokeAllOtherSessions(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {
        String accessToken = extractAccessToken(httpRequest);
        String currentJti = null;
        if (accessToken != null) {
            try {
                currentJti = authService.getTokenProvider().getJtiFromToken(accessToken);
            } catch (Exception ignored) {}
        }
        authService.revokeAllOtherSessions(principal.getId(), currentJti);
        return ResponseEntity.ok(uz.eduplatform.core.common.dto.ApiResponse.success(null));
    }

    private String extractAccessToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}