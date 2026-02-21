package uz.eduplatform.modules.notification.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uz.eduplatform.core.common.utils.MessageService;
import uz.eduplatform.modules.notification.client.EmailClient;
import uz.eduplatform.modules.notification.client.EskizClient;
import uz.eduplatform.modules.notification.client.SendResult;
import uz.eduplatform.modules.notification.domain.NotificationChannel;
import uz.eduplatform.modules.notification.domain.NotificationHistory;
import uz.eduplatform.modules.notification.domain.NotificationStatus;
import uz.eduplatform.modules.notification.repository.NotificationHistoryRepository;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private EskizClient eskizClient;
    @Mock private EmailClient emailClient;
    @Mock private NotificationHistoryRepository historyRepository;
    @Mock private MessageService messageService;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void sendSms_success_setsStatusToSent() {
        UUID userId = UUID.randomUUID();
        String phone = "+998901234567";

        when(messageService.get(eq("otp.sms.body"), any(Locale.class), any()))
                .thenReturn("Your OTP: 123456");
        when(eskizClient.sendSms(phone, "Your OTP: 123456"))
                .thenReturn(SendResult.ok("Message sent"));
        when(historyRepository.save(any(NotificationHistory.class)))
                .thenAnswer(i -> i.getArgument(0));

        notificationService.sendSms(userId, phone, "otp.sms.body",
                Map.of("code", "123456"), Locale.forLanguageTag("uz"));

        ArgumentCaptor<NotificationHistory> captor = ArgumentCaptor.forClass(NotificationHistory.class);
        verify(historyRepository, times(2)).save(captor.capture());

        NotificationHistory saved = captor.getAllValues().get(1);
        assertEquals(NotificationStatus.SENT, saved.getStatus());
        assertNotNull(saved.getSentAt());
        assertEquals("eskiz", saved.getProvider());
        assertEquals(NotificationChannel.SMS, saved.getChannel());
    }

    @Test
    void sendSms_failure_setsStatusToFailed() {
        UUID userId = UUID.randomUUID();
        String phone = "+998901234567";

        when(messageService.get(eq("otp.sms.body"), any(Locale.class), any()))
                .thenReturn("Your OTP: 123456");
        when(eskizClient.sendSms(phone, "Your OTP: 123456"))
                .thenReturn(SendResult.fail("Connection timeout"));
        when(historyRepository.save(any(NotificationHistory.class)))
                .thenAnswer(i -> i.getArgument(0));

        notificationService.sendSms(userId, phone, "otp.sms.body",
                Map.of("code", "123456"), Locale.forLanguageTag("uz"));

        ArgumentCaptor<NotificationHistory> captor = ArgumentCaptor.forClass(NotificationHistory.class);
        verify(historyRepository, times(2)).save(captor.capture());

        NotificationHistory saved = captor.getAllValues().get(1);
        assertEquals(NotificationStatus.FAILED, saved.getStatus());
        assertNull(saved.getSentAt());
        assertEquals("Connection timeout", saved.getProviderResponse());
    }

    @Test
    void sendEmail_success_setsStatusToSent() {
        UUID userId = UUID.randomUUID();
        String email = "user@test.com";
        String subject = "Welcome";
        Map<String, Object> vars = Map.of("firstName", "John");

        when(emailClient.sendEmail(email, subject, "welcome", vars, Locale.ENGLISH))
                .thenReturn(SendResult.ok("Email sent successfully"));
        when(historyRepository.save(any(NotificationHistory.class)))
                .thenAnswer(i -> i.getArgument(0));

        notificationService.sendEmail(userId, email, subject, "welcome", vars, Locale.ENGLISH);

        ArgumentCaptor<NotificationHistory> captor = ArgumentCaptor.forClass(NotificationHistory.class);
        verify(historyRepository, times(2)).save(captor.capture());

        NotificationHistory firstSave = captor.getAllValues().get(0);
        assertEquals(NotificationChannel.EMAIL, firstSave.getChannel());
        assertEquals("smtp", firstSave.getProvider());
        assertEquals(email, firstSave.getRecipient());

        NotificationHistory secondSave = captor.getAllValues().get(1);
        assertEquals(NotificationStatus.SENT, secondSave.getStatus());
        assertNotNull(secondSave.getSentAt());
    }

    @Test
    void sendEmail_failure_setsStatusToFailed() {
        UUID userId = UUID.randomUUID();
        String email = "user@test.com";

        when(emailClient.sendEmail(anyString(), anyString(), anyString(), any(), any(Locale.class)))
                .thenReturn(SendResult.fail("SMTP connection refused"));
        when(historyRepository.save(any(NotificationHistory.class)))
                .thenAnswer(i -> i.getArgument(0));

        notificationService.sendEmail(userId, email, "Welcome", "welcome",
                Map.of(), Locale.ENGLISH);

        ArgumentCaptor<NotificationHistory> captor = ArgumentCaptor.forClass(NotificationHistory.class);
        verify(historyRepository, times(2)).save(captor.capture());

        NotificationHistory saved = captor.getAllValues().get(1);
        assertEquals(NotificationStatus.FAILED, saved.getStatus());
        assertNull(saved.getSentAt());
    }

    @Test
    void sendOtp_withEmail_sendsViaEmailClient() {
        String emailIdentifier = "user@test.com";
        String otp = "123456";

        when(messageService.get("otp.email.subject", Locale.ENGLISH)).thenReturn("OTP Code");
        when(emailClient.sendEmail(eq(emailIdentifier), eq("OTP Code"), eq("otp"), anyMap(), eq(Locale.ENGLISH)))
                .thenReturn(SendResult.ok("Sent"));
        when(historyRepository.save(any(NotificationHistory.class)))
                .thenAnswer(i -> i.getArgument(0));

        notificationService.sendOtp(emailIdentifier, otp, Locale.ENGLISH);

        verify(emailClient).sendEmail(eq(emailIdentifier), eq("OTP Code"), eq("otp"), anyMap(), eq(Locale.ENGLISH));
        verify(eskizClient, never()).sendSms(anyString(), anyString());
    }

    @Test
    void sendOtp_withPhone_sendsViaSmsClient() {
        String phoneIdentifier = "+998901234567";
        String otp = "654321";

        when(messageService.get("otp.sms.body", Locale.ENGLISH, otp)).thenReturn("Your code: 654321");
        when(eskizClient.sendSms(eq(phoneIdentifier), anyString()))
                .thenReturn(SendResult.ok("Sent"));
        when(historyRepository.save(any(NotificationHistory.class)))
                .thenAnswer(i -> i.getArgument(0));

        notificationService.sendOtp(phoneIdentifier, otp, Locale.ENGLISH);

        verify(eskizClient).sendSms(eq(phoneIdentifier), anyString());
        verify(emailClient, never()).sendEmail(anyString(), anyString(), anyString(), anyMap(), any());
    }

    @Test
    void sendOtp_smsFailure_logsWarning() {
        String phone = "+998901234567";
        String otp = "111111";

        when(messageService.get("otp.sms.body", Locale.ENGLISH, otp)).thenReturn("Code: 111111");
        when(eskizClient.sendSms(eq(phone), anyString()))
                .thenReturn(SendResult.fail("Network error"));
        when(historyRepository.save(any(NotificationHistory.class)))
                .thenAnswer(i -> i.getArgument(0));

        notificationService.sendOtp(phone, otp, Locale.ENGLISH);

        ArgumentCaptor<NotificationHistory> captor = ArgumentCaptor.forClass(NotificationHistory.class);
        verify(historyRepository, atLeast(2)).save(captor.capture());

        NotificationHistory lastSaved = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertEquals(NotificationStatus.FAILED, lastSaved.getStatus());
    }

    @Test
    void sendSms_createsHistoryWithCorrectChannel() {
        UUID userId = UUID.randomUUID();
        String phone = "+998907654321";

        when(messageService.get(anyString(), any(Locale.class), any()))
                .thenReturn("Test message");
        when(eskizClient.sendSms(anyString(), anyString()))
                .thenReturn(SendResult.ok("OK"));
        when(historyRepository.save(any(NotificationHistory.class)))
                .thenAnswer(i -> i.getArgument(0));

        notificationService.sendSms(userId, phone, "test.template",
                Map.of(), Locale.ENGLISH);

        ArgumentCaptor<NotificationHistory> captor = ArgumentCaptor.forClass(NotificationHistory.class);
        verify(historyRepository, atLeast(1)).save(captor.capture());

        NotificationHistory initial = captor.getAllValues().get(0);
        assertEquals(userId, initial.getUserId());
        assertEquals(phone, initial.getRecipient());
        assertEquals(NotificationChannel.SMS, initial.getChannel());
        assertEquals("eskiz", initial.getProvider());
    }
}
