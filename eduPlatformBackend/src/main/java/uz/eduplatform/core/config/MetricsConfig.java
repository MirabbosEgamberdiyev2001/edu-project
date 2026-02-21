package uz.eduplatform.core.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public Counter paymentInitiatedCounter(MeterRegistry registry) {
        return Counter.builder("payment.initiated")
                .description("Number of payment initiations")
                .tag("type", "total")
                .register(registry);
    }

    @Bean
    public Counter paymentCompletedCounter(MeterRegistry registry) {
        return Counter.builder("payment.completed")
                .description("Number of completed payments")
                .tag("type", "total")
                .register(registry);
    }

    @Bean
    public Counter paymentFailedCounter(MeterRegistry registry) {
        return Counter.builder("payment.failed")
                .description("Number of failed payments")
                .tag("type", "total")
                .register(registry);
    }

    @Bean
    public Counter userRegistrationCounter(MeterRegistry registry) {
        return Counter.builder("user.registration")
                .description("Number of user registrations")
                .tag("type", "total")
                .register(registry);
    }

    @Bean
    public Counter authLoginCounter(MeterRegistry registry) {
        return Counter.builder("auth.login")
                .description("Number of login attempts")
                .tag("type", "total")
                .register(registry);
    }

    @Bean
    public Counter authLoginFailedCounter(MeterRegistry registry) {
        return Counter.builder("auth.login.failed")
                .description("Number of failed login attempts")
                .tag("type", "total")
                .register(registry);
    }

    @Bean
    public Timer paymentProcessingTimer(MeterRegistry registry) {
        return Timer.builder("payment.processing.time")
                .description("Time taken to process payment callbacks")
                .register(registry);
    }

    @Bean
    public Counter testGeneratedCounter(MeterRegistry registry) {
        return Counter.builder("test.generated")
                .description("Number of tests generated")
                .tag("type", "total")
                .register(registry);
    }

    @Bean
    public Counter otpSentCounter(MeterRegistry registry) {
        return Counter.builder("otp.sent")
                .description("Number of OTP codes sent")
                .tag("type", "total")
                .register(registry);
    }
}
