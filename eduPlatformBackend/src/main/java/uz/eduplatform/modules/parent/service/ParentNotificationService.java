package uz.eduplatform.modules.parent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uz.eduplatform.modules.auth.domain.User;
import uz.eduplatform.modules.auth.repository.UserRepository;
import uz.eduplatform.modules.notification.service.NotificationService;
import uz.eduplatform.modules.parent.domain.PairingStatus;
import uz.eduplatform.modules.parent.domain.ParentChild;
import uz.eduplatform.modules.parent.repository.ParentChildRepository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParentNotificationService {

    private final ParentChildRepository parentChildRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * Notify all linked parents when a student completes and gets graded on a test.
     */
    @Async("notificationExecutor")
    public void notifyParentsOfTestResult(UUID studentId, String assignmentTitle,
                                           BigDecimal percentage) {
        try {
            List<ParentChild> pairings = parentChildRepository.findByChildIdAndStatus(
                    studentId, PairingStatus.ACTIVE);

            if (pairings.isEmpty()) return;

            User student = userRepository.findById(studentId).orElse(null);
            if (student == null) return;

            String studentName = student.getFirstName() + " " + student.getLastName();

            for (ParentChild pairing : pairings) {
                User parent = userRepository.findById(pairing.getParentId()).orElse(null);
                if (parent == null) continue;

                Map<String, Object> vars = new HashMap<>();
                vars.put("childName", studentName);
                vars.put("assignmentTitle", assignmentTitle);
                vars.put("percentage", percentage != null ? percentage.toString() : "N/A");

                // Send email notification if parent has email
                if (parent.getEmail() != null) {
                    notificationService.sendEmail(
                            parent.getId(),
                            parent.getEmail(),
                            "Test natijasi: " + studentName,
                            "parent.test.result",
                            vars,
                            Locale.forLanguageTag("uz-Latn-UZ")
                    );
                }

                log.info("Notified parent {} about child {} test result: {}%",
                        parent.getId(), studentId, percentage);
            }
        } catch (Exception e) {
            log.error("Failed to notify parents of test result for student {}", studentId, e);
        }
    }

    /**
     * Notify parents when a student scores below threshold (at-risk alert).
     */
    @Async("notificationExecutor")
    public void notifyParentsOfLowScore(UUID studentId, String assignmentTitle,
                                         BigDecimal percentage, BigDecimal threshold) {
        try {
            if (percentage == null || percentage.compareTo(threshold) >= 0) return;

            List<ParentChild> pairings = parentChildRepository.findByChildIdAndStatus(
                    studentId, PairingStatus.ACTIVE);

            if (pairings.isEmpty()) return;

            User student = userRepository.findById(studentId).orElse(null);
            if (student == null) return;

            String studentName = student.getFirstName() + " " + student.getLastName();

            for (ParentChild pairing : pairings) {
                User parent = userRepository.findById(pairing.getParentId()).orElse(null);
                if (parent == null) continue;

                Map<String, Object> vars = new HashMap<>();
                vars.put("childName", studentName);
                vars.put("assignmentTitle", assignmentTitle);
                vars.put("percentage", percentage.toString());
                vars.put("threshold", threshold.toString());

                if (parent.getEmail() != null) {
                    notificationService.sendEmail(
                            parent.getId(),
                            parent.getEmail(),
                            "Diqqat: " + studentName + " past natija",
                            "parent.low.score.alert",
                            vars,
                            Locale.forLanguageTag("uz-Latn-UZ")
                    );
                }

                log.info("Notified parent {} about child {} low score: {}%",
                        parent.getId(), studentId, percentage);
            }
        } catch (Exception e) {
            log.error("Failed to notify parents of low score for student {}", studentId, e);
        }
    }
}
