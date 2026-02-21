package uz.eduplatform.modules.subscription.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uz.eduplatform.modules.subscription.config.PaymentProperties;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class UzumSignatureVerifier {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final PaymentProperties paymentProperties;

    /**
     * Verifies Uzum callback signature using HMAC-SHA256.
     * The signature header contains Base64(HMAC-SHA256(requestBody, secretKey)).
     */
    public boolean verify(String signatureHeader, String requestBody) {
        if (signatureHeader == null || requestBody == null) {
            return false;
        }

        try {
            String secretKey = paymentProperties.getUzum().getSecretKey();
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);

            byte[] hmacBytes = mac.doFinal(requestBody.getBytes(StandardCharsets.UTF_8));
            String computed = Base64.getEncoder().encodeToString(hmacBytes);

            return computed.equals(signatureHeader);
        } catch (Exception e) {
            return false;
        }
    }
}
