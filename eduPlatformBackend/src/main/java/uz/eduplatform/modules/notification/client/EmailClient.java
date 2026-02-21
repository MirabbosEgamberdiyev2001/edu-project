package uz.eduplatform.modules.notification.client;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailClient {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.email.from-name:EduPlatform}")
    private String fromName;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${app.email.test-mode.log-only:false}")
    private boolean testModeLogOnly;

    public SendResult sendEmail(String to, String subject, String templateName,
                                Map<String, Object> variables, Locale locale) {
        if (!emailEnabled) {
            log.warn("Email sending is disabled. Would send '{}' to {}", subject, to);
            return SendResult.ok("Email disabled - simulated success");
        }

        try {
            Context context = new Context(locale);
            if (variables != null) {
                context.setVariables(variables);
            }

            String htmlBody = templateEngine.process("email/" + templateName, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // ✅ FROM name bilan to'g'ri format
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            if (testModeLogOnly) {
                log.info("TEST MODE: Would send email to {} with subject '{}'", to, subject);
                log.debug("Email body: {}", htmlBody);
                return SendResult.ok("Test mode - email logged only");
            }

            mailSender.send(message);
            log.info("Email sent to {} from '{}': {}", to, fromName, subject);
            return SendResult.ok("Email sent successfully");

        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            return SendResult.fail(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error sending email to {}: {}", to, e.getMessage());
            return SendResult.fail(e.getMessage());
        }
    }
}