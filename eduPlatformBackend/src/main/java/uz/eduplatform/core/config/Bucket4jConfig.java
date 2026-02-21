package uz.eduplatform.core.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Configuration
public class Bucket4jConfig {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Bean(name = "otpSendRateLimiter")
    public Function<String, Bucket> otpSendBucketResolver() {
        return key -> buckets.computeIfAbsent("otp_send:" + key, k ->
                Bucket.builder()
                        .addLimit(Bandwidth.classic(
                                3, Refill.intervally(3, Duration.ofMinutes(5))
                        ))
                        .build()
        );
    }

    @Bean(name = "otpVerifyRateLimiter")
    public Function<String, Bucket> otpVerifyBucketResolver() {
        return key -> buckets.computeIfAbsent("otp_verify:" + key, k ->
                Bucket.builder()
                        .addLimit(Bandwidth.classic(
                                5, Refill.intervally(5, Duration.ofMinutes(5))
                        ))
                        .build()
        );
    }

    @Bean(name = "paymentInitiateRateLimiter")
    public Function<String, Bucket> paymentInitiateBucketResolver() {
        return key -> buckets.computeIfAbsent("payment_initiate:" + key, k ->
                Bucket.builder()
                        .addLimit(Bandwidth.classic(
                                5, Refill.intervally(5, Duration.ofMinutes(15))
                        ))
                        .build()
        );
    }
}
