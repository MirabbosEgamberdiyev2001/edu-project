package uz.eduplatform.core.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uz.eduplatform.modules.subscription.config.PaymentProperties;

@Configuration
@RequiredArgsConstructor
public class HealthCheckConfig {

    private final PaymentProperties paymentProperties;

    @Bean
    public HealthIndicator paymeHealthIndicator() {
        return () -> {
            if (!paymentProperties.getPayme().isEnabled()) {
                return Health.up().withDetail("status", "disabled").build();
            }
            boolean hasCredentials = !paymentProperties.getPayme().getMerchantId().isBlank()
                    && !paymentProperties.getPayme().getMerchantKey().isBlank();
            if (hasCredentials) {
                return Health.up().withDetail("status", "configured").build();
            }
            return Health.down().withDetail("status", "enabled but missing credentials").build();
        };
    }

    @Bean
    public HealthIndicator clickHealthIndicator() {
        return () -> {
            if (!paymentProperties.getClick().isEnabled()) {
                return Health.up().withDetail("status", "disabled").build();
            }
            boolean hasCredentials = !paymentProperties.getClick().getServiceId().isBlank()
                    && !paymentProperties.getClick().getSecretKey().isBlank();
            if (hasCredentials) {
                return Health.up().withDetail("status", "configured").build();
            }
            return Health.down().withDetail("status", "enabled but missing credentials").build();
        };
    }

    @Bean
    public HealthIndicator uzumHealthIndicator() {
        return () -> {
            if (!paymentProperties.getUzum().isEnabled()) {
                return Health.up().withDetail("status", "disabled").build();
            }
            boolean hasCredentials = !paymentProperties.getUzum().getServiceId().isBlank()
                    && !paymentProperties.getUzum().getSecretKey().isBlank();
            if (hasCredentials) {
                return Health.up().withDetail("status", "configured").build();
            }
            return Health.down().withDetail("status", "enabled but missing credentials").build();
        };
    }
}
