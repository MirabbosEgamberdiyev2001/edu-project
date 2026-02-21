package uz.eduplatform.modules.subscription.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.payment")
public class PaymentProperties {

    private boolean testMode = false;
    private PaymeConfig payme = new PaymeConfig();
    private ClickConfig click = new ClickConfig();
    private UzumConfig uzum = new UzumConfig();

    @Data
    public static class PaymeConfig {
        private boolean enabled = false;
        private String merchantId = "";
        private String merchantKey = "";
        private String checkoutUrl = "https://checkout.paycom.uz";
        private String testCheckoutUrl = "https://checkout.test.paycom.uz";
        private long transactionTimeout = 43200000; // 12 hours in ms

        public String getEffectiveCheckoutUrl(boolean testMode) {
            return testMode ? testCheckoutUrl : checkoutUrl;
        }
    }

    @Data
    public static class ClickConfig {
        private boolean enabled = false;
        private String merchantId = "";
        private String serviceId = "";
        private String secretKey = "";
        private String merchantUserId = "";
        private String checkoutUrl = "https://my.click.uz/services/pay";
        private String testCheckoutUrl = "https://my.click.uz/services/pay";

        public String getEffectiveCheckoutUrl(boolean testMode) {
            return testMode ? testCheckoutUrl : checkoutUrl;
        }
    }

    @Data
    public static class UzumConfig {
        private boolean enabled = false;
        private String serviceId = "";
        private String secretKey = "";
        private String checkoutUrl = "https://www.uzumbank.uz/pay";
        private String testCheckoutUrl = "https://www.uzumbank.uz/pay";

        public String getEffectiveCheckoutUrl(boolean testMode) {
            return testMode ? testCheckoutUrl : checkoutUrl;
        }
    }
}
