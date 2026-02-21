package uz.eduplatform.modules.subscription.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uz.eduplatform.modules.subscription.config.PaymentProperties;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
@RequiredArgsConstructor
public class ClickSignatureVerifier {

    private final PaymentProperties paymentProperties;

    /**
     * Verifies Click signature using MD5 hash.
     * For prepare (action=0):
     *   MD5(click_trans_id + service_id + secret_key + merchant_trans_id + amount + action + sign_time)
     * For complete (action=1):
     *   MD5(click_trans_id + service_id + secret_key + merchant_trans_id + merchant_prepare_id + amount + action + sign_time)
     */
    public boolean verifyPrepare(Long clickTransId, Long serviceId,
                                  String merchantTransId, Double amount,
                                  Integer action, String signTime,
                                  String signString) {
        String data = clickTransId + ""
                + serviceId
                + paymentProperties.getClick().getSecretKey()
                + merchantTransId
                + String.format("%.2f", amount)
                + action
                + signTime;
        return md5(data).equals(signString);
    }

    public boolean verifyComplete(Long clickTransId, Long serviceId,
                                   String merchantTransId, String merchantPrepareId,
                                   Double amount, Integer action,
                                   String signTime, String signString) {
        String data = clickTransId + ""
                + serviceId
                + paymentProperties.getClick().getSecretKey()
                + merchantTransId
                + merchantPrepareId
                + String.format("%.2f", amount)
                + action
                + signTime;
        return md5(data).equals(signString);
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }
}
