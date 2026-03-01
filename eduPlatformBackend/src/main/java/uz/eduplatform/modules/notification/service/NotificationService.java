package uz.eduplatform.modules.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uz.eduplatform.core.common.utils.MessageService;
import uz.eduplatform.modules.notification.client.EmailClient;
import uz.eduplatform.modules.notification.client.EskizClient;
import uz.eduplatform.modules.notification.client.SendResult;
import uz.eduplatform.modules.notification.domain.NotificationChannel;
import uz.eduplatform.modules.notification.domain.NotificationHistory;
import uz.eduplatform.modules.notification.domain.NotificationStatus;
import uz.eduplatform.modules.notification.repository.NotificationHistoryRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final EskizClient eskizClient;
    private final EmailClient emailClient;
    private final NotificationHistoryRepository historyRepository;
    private final MessageService messageService;

    @Value("${app.email.from-name:Test-Pro}")
    private String appName;

    private static final int MAX_RETRIES = 3;

    @Async("notificationExecutor")
    public void sendSms(UUID userId, String phone, String templateCode,
                        Map<String, Object> variables, Locale locale) {
        String body = resolveTemplate(templateCode, variables, locale);

        NotificationHistory history = NotificationHistory.builder()
                .userId(userId)
                .channel(NotificationChannel.SMS)
                .recipient(phone)
                .body(body)
                .provider("eskiz")
                .build();
        history = historyRepository.save(history);

        SendResult result = attemptSmsWithRetry(phone, body);

        history.setStatus(result.success() ? NotificationStatus.SENT : NotificationStatus.FAILED);
        history.setProviderResponse(result.success() ? result.providerResponse() : result.errorMessage());
        if (result.success()) {
            history.setSentAt(LocalDateTime.now());
        }
        historyRepository.save(history);
    }

    @Async("notificationExecutor")
    public void sendEmail(UUID userId, String email, String subject, String templateCode,
                          Map<String, Object> variables, Locale locale) {
        NotificationHistory history = NotificationHistory.builder()
                .userId(userId)
                .channel(NotificationChannel.EMAIL)
                .recipient(email)
                .subject(subject)
                .body(templateCode)
                .provider("smtp")
                .build();
        history = historyRepository.save(history);

        SendResult result = emailClient.sendEmail(email, subject, templateCode, variables, locale);

        history.setStatus(result.success() ? NotificationStatus.SENT : NotificationStatus.FAILED);
        history.setProviderResponse(result.success() ? result.providerResponse() : result.errorMessage());
        if (result.success()) {
            history.setSentAt(LocalDateTime.now());
        }
        historyRepository.save(history);
    }

    @Async("notificationExecutor")
    public void sendOtp(String identifier, String otp, Locale locale) {
        boolean isEmail = identifier.contains("@");

        if (isEmail) {
            String subject = messageService.get("otp.email.subject", locale);

            Map<String, Object> vars = new HashMap<>();
            vars.put("otpCode", otp);
            vars.put("expiryMinutes", 5);
            vars.put("appName", appName);

            NotificationHistory history = createHistory(null, NotificationChannel.EMAIL, identifier, subject);
            SendResult result = emailClient.sendEmail(identifier, subject, "otp", vars, locale);
            updateHistory(history, result);

        } else {
            String smsBody = messageService.get("otp.sms.body", locale, otp);

            NotificationHistory history = createHistory(null, NotificationChannel.SMS, identifier, null);
            history.setBody(smsBody);
            history = historyRepository.save(history);

            SendResult result = attemptSmsWithRetry(identifier, smsBody);
            updateHistory(history, result);

            if (!result.success()) {
                log.warn("SMS failed for {}, no email fallback available in OTP context", identifier);
            }
        }
    }

    @Async("notificationExecutor")
    public void sendPasswordResetOtp(String identifier, String otp, Locale locale) {
        boolean isEmail = identifier.contains("@");

        if (isEmail) {
            String subject = messageService.get("notification.password.reset.subject", locale);

            Map<String, Object> vars = new HashMap<>();
            vars.put("otpCode", otp);
            vars.put("expiryMinutes", 5);
            vars.put("appName", appName);

            NotificationHistory history = createHistory(null, NotificationChannel.EMAIL, identifier, subject);
            SendResult result = emailClient.sendEmail(identifier, subject, "password-reset", vars, locale);
            updateHistory(history, result);
        } else {
            String smsBody = messageService.get("otp.sms.body", locale, otp);

            NotificationHistory history = createHistory(null, NotificationChannel.SMS, identifier, null);
            history.setBody(smsBody);
            history = historyRepository.save(history);

            SendResult result = attemptSmsWithRetry(identifier, smsBody);
            updateHistory(history, result);
        }
    }

    private SendResult attemptSmsWithRetry(String phone, String message) {
        return eskizClient.sendSms(phone, message);
    }

    private String resolveTemplate(String templateCode, Map<String, Object> variables, Locale locale) {
        try {
            Object[] args = variables != null ? variables.values().toArray() : new Object[0];
            return messageService.get(templateCode, locale, args);
        } catch (Exception e) {
            log.warn("Could not resolve template {}: {}", templateCode, e.getMessage());
            return templateCode;
        }
    }

    private NotificationHistory createHistory(UUID userId, NotificationChannel channel,
                                               String recipient, String subject) {
        NotificationHistory history = NotificationHistory.builder()
                .userId(userId)
                .channel(channel)
                .recipient(recipient)
                .subject(subject)
                .provider(channel == NotificationChannel.SMS ? "eskiz" : "smtp")
                .build();
        return historyRepository.save(history);
    }

    private void updateHistory(NotificationHistory history, SendResult result) {
        history.setStatus(result.success() ? NotificationStatus.SENT : NotificationStatus.FAILED);
        history.setProviderResponse(result.success() ? result.providerResponse() : result.errorMessage());
        if (result.success()) {
            history.setSentAt(LocalDateTime.now());
        }
        historyRepository.save(history);
    }
}
