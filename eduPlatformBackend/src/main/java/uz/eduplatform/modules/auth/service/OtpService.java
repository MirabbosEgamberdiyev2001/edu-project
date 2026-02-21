package uz.eduplatform.modules.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uz.eduplatform.core.audit.AuditService;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.core.common.utils.MessageService;
import uz.eduplatform.modules.auth.dto.OtpResponse;
import uz.eduplatform.modules.auth.dto.RegisterRequest;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.function.Function;

@Slf4j
@Service
public class OtpService {

    private final Cache<String, String> otpCache;
    private final Cache<String, String> registrationDataCache;
    private final Function<String, Bucket> otpSendBucketResolver;
    private final Function<String, Bucket> otpVerifyBucketResolver;
    private final AuditService auditService;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;

    private static final String OTP_PREFIX = "otp:";
    private static final String REG_DATA_PREFIX = "reg_data:";
    private static final int OTP_LENGTH = 6;
    private static final Duration OTP_EXPIRY = Duration.ofMinutes(5);
    private static final String SEPARATOR = ":";

    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public OtpService(@Qualifier("otpSendRateLimiter")
                      Function<String, Bucket> otpSendBucketResolver,

                      @Qualifier("otpVerifyRateLimiter")
                      Function<String, Bucket> otpVerifyBucketResolver,

                      AuditService auditService,
                      MessageService messageService,
                      ObjectMapper objectMapper) {
        this.otpCache = Caffeine.newBuilder()
                .expireAfterWrite(OTP_EXPIRY)
                .maximumSize(10_000)
                .build();

        this.registrationDataCache = Caffeine.newBuilder()
                .expireAfterWrite(OTP_EXPIRY)
                .maximumSize(10_000)
                .build();

        this.otpSendBucketResolver = otpSendBucketResolver;
        this.otpVerifyBucketResolver = otpVerifyBucketResolver;
        this.auditService = auditService;
        this.messageService = messageService;
        this.objectMapper = objectMapper;
    }

    // Test-visible constructor
    OtpService(Cache<String, String> otpCache,
               Cache<String, String> registrationDataCache,
               Function<String, Bucket> otpSendBucketResolver,
               Function<String, Bucket> otpVerifyBucketResolver,
               AuditService auditService,
               MessageService messageService,
               ObjectMapper objectMapper) {
        this.otpCache = otpCache;
        this.registrationDataCache = registrationDataCache;
        this.otpSendBucketResolver = otpSendBucketResolver;
        this.otpVerifyBucketResolver = otpVerifyBucketResolver;
        this.auditService = auditService;
        this.messageService = messageService;
        this.objectMapper = objectMapper;
    }

    public OtpResponse generateAndStore(String identifier, String purpose) {
        Bucket sendBucket = otpSendBucketResolver.apply(identifier);
        if (!sendBucket.tryConsume(1)) {
            log.warn("OTP send rate limit exceeded for: {}", identifier);
            throw BusinessException.ofKey("auth.otp.rate.limit.send");
        }

        String key = OTP_PREFIX + identifier;
        String otp = generateOtp();
        otpCache.put(key, otp + SEPARATOR + purpose);

        log.info("OTP generated for {} ({}): {}", identifier, purpose, otp);
        auditService.log(null, null, "OTP_GENERATED", "AUTH");

        return new OtpResponse(messageService.get("otp.sent.success"));
    }

    public String getStoredOtp(String identifier) {
        String key = OTP_PREFIX + identifier;
        String stored = otpCache.getIfPresent(key);
        return stored != null ? stored.split(SEPARATOR, 2)[0] : null;
    }

    /**
     * Verify OTP and return the purpose it was generated for.
     * Returns purpose string (e.g. "REGISTER_EMAIL", "PASSWORD_RESET") on success.
     * Throws on invalid/expired OTP.
     */
    public String verify(String identifier, String code) {
        Bucket verifyBucket = otpVerifyBucketResolver.apply(identifier);
        if (!verifyBucket.tryConsume(1)) {
            log.warn("OTP verify rate limit exceeded for: {}", identifier);
            throw BusinessException.ofKey("auth.otp.rate.limit.verify");
        }

        String key = OTP_PREFIX + identifier;
        String storedValue = otpCache.getIfPresent(key);

        if (storedValue == null) {
            auditService.log(null, null, "OTP_VERIFY_EXPIRED", "AUTH");
            throw BusinessException.ofKey("auth.otp.expired");
        }

        String[] parts = storedValue.split(SEPARATOR, 2);
        String storedOtp = parts[0];
        String storedPurpose = parts[1];

        if (storedOtp.equals(code)) {
            otpCache.invalidate(key);
            auditService.log(null, null, "OTP_VERIFIED", "AUTH");
            return storedPurpose;
        }

        auditService.log(null, null, "OTP_VERIFY_FAILED", "AUTH");
        throw BusinessException.ofKey("auth.otp.invalid");
    }

    /**
     * Store registration data temporarily until OTP is verified
     */
    public void storeRegistrationData(String identifier, RegisterRequest data) {
        try {
            String key = REG_DATA_PREFIX + identifier;
            String jsonData = objectMapper.writeValueAsString(data);
            registrationDataCache.put(key, jsonData);
            log.debug("Registration data stored for {}", identifier);
        } catch (Exception e) {
            log.error("Failed to store registration data", e);
            throw BusinessException.ofKey("auth.registration.data.store.failed");
        }
    }

    public RegisterRequest getRegistrationData(String identifier) {
        try {
            String key = REG_DATA_PREFIX + identifier;
            String jsonData = registrationDataCache.getIfPresent(key);
            if (jsonData == null) {
                return null;
            }
            return objectMapper.readValue(jsonData, RegisterRequest.class);
        } catch (Exception e) {
            log.error("Failed to retrieve registration data", e);
            return null;
        }
    }

    public void clearRegistrationData(String identifier) {
        String key = REG_DATA_PREFIX + identifier;
        registrationDataCache.invalidate(key);
        log.debug("Registration data cleared for {}", identifier);
    }

    private String generateOtp() {
        int otp = secureRandom.nextInt((int) Math.pow(10, OTP_LENGTH));
        return String.format("%0" + OTP_LENGTH + "d", otp);
    }
}