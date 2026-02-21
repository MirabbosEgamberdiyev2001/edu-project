package uz.eduplatform.modules.auth.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uz.eduplatform.core.common.exception.BusinessException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class TelegramAuthService {

    private final String botToken;
    private final long authDataMaxAgeSeconds;
    private final boolean enabled;

    private record TokenEntry(Long telegramUserId, String firstName, String lastName, String username,
                              String photoUrl, Instant expiresAt) {}

    private final Map<String, TokenEntry> oneTimeTokens = new ConcurrentHashMap<>();

    public TelegramAuthService(
            @Value("${app.telegram.bot-token:}") String botToken,
            @Value("${app.telegram.auth-data-max-age-seconds:86400}") long authDataMaxAgeSeconds) {
        this.botToken = botToken;
        this.authDataMaxAgeSeconds = authDataMaxAgeSeconds;
        this.enabled = botToken != null && !botToken.isBlank();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void verifyAuthData(TelegramAuthData request) {
        if (!enabled) {
            throw BusinessException.ofKey("auth.telegram.not.configured");
        }

        long currentTime = Instant.now().getEpochSecond();
        if (currentTime - request.getAuthDate() > authDataMaxAgeSeconds) {
            throw BusinessException.ofKey("auth.telegram.auth.expired");
        }

        String dataCheckString = buildDataCheckString(request);
        String expectedHash = calculateHash(dataCheckString);

        if (!expectedHash.equalsIgnoreCase(request.getHash())) {
            throw BusinessException.ofKey("auth.telegram.auth.invalid");
        }
    }

    public String generateOneTimeToken(Long telegramUserId, String firstName, String lastName,
                                        String username, String photoUrl) {
        String token = UUID.randomUUID().toString();
        oneTimeTokens.put(token, new TokenEntry(
                telegramUserId, firstName, lastName, username, photoUrl,
                Instant.now().plusSeconds(900) // 15 min TTL
        ));
        return token;
    }

    public TokenInfo validateAndConsumeToken(String token) {
        TokenEntry entry = oneTimeTokens.remove(token);
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            return null;
        }
        return new TokenInfo(entry.telegramUserId(), entry.firstName(), entry.lastName(),
                entry.username(), entry.photoUrl());
    }

    @Scheduled(fixedRate = 60_000)
    public void cleanExpiredTokens() {
        Instant now = Instant.now();
        oneTimeTokens.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt()));
    }

    private String buildDataCheckString(TelegramAuthData request) {
        TreeMap<String, String> dataMap = new TreeMap<>();
        dataMap.put("id", String.valueOf(request.getId()));
        if (request.getFirstName() != null) dataMap.put("first_name", request.getFirstName());
        if (request.getLastName() != null) dataMap.put("last_name", request.getLastName());
        if (request.getUsername() != null) dataMap.put("username", request.getUsername());
        if (request.getPhotoUrl() != null) dataMap.put("photo_url", request.getPhotoUrl());
        dataMap.put("auth_date", String.valueOf(request.getAuthDate()));

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : dataMap.entrySet()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    private String calculateHash(String dataCheckString) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] secretKey = sha256.digest(botToken.getBytes(StandardCharsets.UTF_8));

            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secretKey, "HmacSHA256");
            hmac.init(keySpec);
            byte[] hashBytes = hmac.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));

            return bytesToHex(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate Telegram auth hash", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Data
    public static class TelegramAuthData {
        private long id;
        private String firstName;
        private String lastName;
        private String username;
        private String photoUrl;
        private long authDate;
        private String hash;
    }

    public record TokenInfo(Long telegramUserId, String firstName, String lastName,
                            String username, String photoUrl) {}
}
