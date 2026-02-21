package uz.eduplatform.modules.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uz.eduplatform.core.audit.AuditService;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.core.common.utils.MessageService;
import uz.eduplatform.modules.auth.dto.OtpResponse;

import java.time.Duration;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    private Cache<String, String> otpCache;
    private Cache<String, String> registrationDataCache;

    @Mock
    private AuditService auditService;

    @Mock
    private MessageService messageService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private OtpService otpService;

    private final Function<String, Bucket> unlimitedSendBucket = key ->
            Bucket.builder().addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(5)))).build();

    private final Function<String, Bucket> unlimitedVerifyBucket = key ->
            Bucket.builder().addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(5)))).build();

    @BeforeEach
    void setUp() {
        otpCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5))
                .maximumSize(1000)
                .build();
        registrationDataCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5))
                .maximumSize(1000)
                .build();
        lenient().when(messageService.get(anyString())).thenReturn("mocked message");
        otpService = new OtpService(otpCache, registrationDataCache,
                unlimitedSendBucket, unlimitedVerifyBucket,
                auditService, messageService, objectMapper);
    }

    @Test
    void generateAndStore_returnsOtpResponse() {
        OtpResponse response = otpService.generateAndStore("test@test.com", "REGISTER");

        assertNotNull(response);
        assertNotNull(response.message());
        assertNotNull(otpCache.getIfPresent("otp:test@test.com"));
    }

    @Test
    void generateAndStore_rateLimited() {
        Bucket exhaustedBucket = Bucket.builder()
                .addLimit(Bandwidth.classic(1, Refill.intervally(1, Duration.ofMinutes(5)))).build();
        exhaustedBucket.tryConsume(1);

        Function<String, Bucket> limitedBucket = key -> exhaustedBucket;
        OtpService limitedOtpService = new OtpService(otpCache, registrationDataCache,
                limitedBucket, unlimitedVerifyBucket,
                auditService, messageService, objectMapper);

        assertThrows(BusinessException.class, () ->
                limitedOtpService.generateAndStore("test@test.com", "REGISTER"));
    }

    @Test
    void verify_withCorrectCode_returnsPurpose() {
        // Cache value format: "otp_code:purpose"
        otpCache.put("otp:test@test.com", "123456:REGISTER");

        String purpose = otpService.verify("test@test.com", "123456");

        assertEquals("REGISTER", purpose);
        assertNull(otpCache.getIfPresent("otp:test@test.com"));
    }

    @Test
    void verify_withWrongCode_throwsException() {
        otpCache.put("otp:test@test.com", "123456:REGISTER");

        assertThrows(BusinessException.class, () ->
                otpService.verify("test@test.com", "999999"));
    }

    @Test
    void verify_withExpiredOtp_throwsException() {
        // No value in cache = expired
        assertThrows(BusinessException.class, () ->
                otpService.verify("test@test.com", "123456"));
    }

    @Test
    void verify_rateLimited() {
        Bucket exhaustedBucket = Bucket.builder()
                .addLimit(Bandwidth.classic(1, Refill.intervally(1, Duration.ofMinutes(5)))).build();
        exhaustedBucket.tryConsume(1);

        Function<String, Bucket> limitedBucket = key -> exhaustedBucket;
        OtpService limitedOtpService = new OtpService(otpCache, registrationDataCache,
                unlimitedSendBucket, limitedBucket,
                auditService, messageService, objectMapper);

        assertThrows(BusinessException.class, () ->
                limitedOtpService.verify("test@test.com", "123456"));
    }
}
