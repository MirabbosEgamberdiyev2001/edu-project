package uz.eduplatform.modules.subscription.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uz.eduplatform.modules.subscription.config.PaymentProperties;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class PaymeAuthVerifier {

    private final PaymentProperties paymentProperties;

    /**
     * Verifies Payme Basic Auth header.
     * Expected format: "Basic base64(Paycom:{merchantKey})"
     */
    public boolean verify(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Basic ")) {
            return false;
        }

        try {
            String base64Credentials = authorizationHeader.substring(6);
            String credentials = new String(
                    Base64.getDecoder().decode(base64Credentials),
                    StandardCharsets.UTF_8
            );

            String[] parts = credentials.split(":", 2);
            if (parts.length != 2) {
                return false;
            }

            String login = parts[0];
            String key = parts[1];

            return "Paycom".equals(login)
                    && paymentProperties.getPayme().getMerchantKey().equals(key);
        } catch (Exception e) {
            return false;
        }
    }
}
