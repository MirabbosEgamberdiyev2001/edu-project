package uz.eduplatform.modules.notification.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class EskizClient {

    private final AtomicReference<String> cachedToken = new AtomicReference<>();
    private final RestTemplate restTemplate;

    private static final int MAX_RETRIES = 3;

    @Value("${app.eskiz.enabled:false}")
    private boolean eskizEnabled;

    @Value("${app.eskiz.base-url:https://notify.eskiz.uz}")
    private String baseUrl;

    @Value("${app.eskiz.email:}")
    private String eskizEmail;

    @Value("${app.eskiz.password:}")
    private String eskizPassword;

    @Value("${app.eskiz.sender:4546}")
    private String sender;

    public EskizClient() {
        this.restTemplate = new RestTemplate();
    }

    public SendResult sendSms(String phone, String message) {
        if (!eskizEnabled) {
            log.warn("Eskiz SMS is disabled. Message for {} would be: {}", phone, message);
            return SendResult.ok("SMS disabled - simulated success");
        }

        String normalizedPhone = normalizePhone(phone);
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String token = getOrRefreshToken();

                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(token);
                headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
                body.add("mobile_phone", normalizedPhone);
                body.add("message", message);
                body.add("from", sender);

                HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(
                        baseUrl + "/api/message/sms/send", request, String.class);

                log.info("SMS sent to {} on attempt {}", normalizedPhone, attempt);
                return SendResult.ok(response.getBody());

            } catch (Exception e) {
                lastException = e;
                log.warn("SMS send attempt {}/{} failed for {}: {}", attempt, MAX_RETRIES, normalizedPhone, e.getMessage());

                if (attempt == 1) {
                    refreshToken();
                }

                if (attempt < MAX_RETRIES) {
                    try {
                        long backoff = (long) Math.pow(2, attempt - 1) * 1000;
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        String errorMsg = lastException != null ? lastException.getMessage() : "Unknown error";
        return SendResult.fail("Failed after " + MAX_RETRIES + " attempts: " + errorMsg);
    }

    public String normalizePhone(String phone) {
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.startsWith("998")) {
            return "+" + digits;
        }
        if (digits.length() == 9) {
            return "+998" + digits;
        }
        return "+" + digits;
    }

    private synchronized String getOrRefreshToken() {
        String cached = cachedToken.get();
        if (cached != null) {
            return cached;
        }
        return refreshToken();
    }

    @SuppressWarnings("unchecked")
    private synchronized String refreshToken() {
        String cached = cachedToken.get();
        if (cached != null) {
            return cached;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("email", eskizEmail);
        body.add("password", eskizPassword);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl + "/api/auth/login", request, Map.class);

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null || !responseBody.containsKey("data")) {
            throw new uz.eduplatform.core.common.exception.BusinessException(
                    "eskiz.auth.failed", null, org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE);
        }

        Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
        String token = (String) data.get("token");

        cachedToken.set(token);
        log.info("Eskiz auth token refreshed");

        return token;
    }
}
