package uz.eduplatform.modules.parent.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uz.eduplatform.modules.auth.domain.Role;
import uz.eduplatform.modules.auth.domain.User;
import uz.eduplatform.modules.auth.repository.UserRepository;
import uz.eduplatform.modules.notification.service.NotificationService;
import uz.eduplatform.modules.parent.domain.PairingStatus;
import uz.eduplatform.modules.parent.domain.ParentChild;
import uz.eduplatform.modules.parent.repository.ParentChildRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ParentNotificationServiceTest {

    @Mock private ParentChildRepository parentChildRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private ParentNotificationService parentNotificationService;

    private UUID studentId;
    private UUID parentId;
    private User student;
    private User parent;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        parentId = UUID.randomUUID();

        student = User.builder()
                .id(studentId).firstName("Student").lastName("User")
                .role(Role.STUDENT).build();
        parent = User.builder()
                .id(parentId).firstName("Parent").lastName("User")
                .role(Role.PARENT).email("parent@test.com").build();

        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(userRepository.findById(parentId)).thenReturn(Optional.of(parent));
    }

    @Test
    void notifyParentsOfTestResult_withActivePairing_sendsEmail() {
        ParentChild pairing = ParentChild.builder()
                .id(UUID.randomUUID()).parentId(parentId).childId(studentId)
                .status(PairingStatus.ACTIVE).build();

        when(parentChildRepository.findByChildIdAndStatus(studentId, PairingStatus.ACTIVE))
                .thenReturn(List.of(pairing));

        parentNotificationService.notifyParentsOfTestResult(
                studentId, "Math Test", new BigDecimal("85.00"));

        verify(notificationService).sendEmail(
                eq(parentId), eq("parent@test.com"),
                anyString(), anyString(), anyMap(), any());
    }

    @Test
    void notifyParentsOfTestResult_noPairing_doesNotNotify() {
        when(parentChildRepository.findByChildIdAndStatus(studentId, PairingStatus.ACTIVE))
                .thenReturn(List.of());

        parentNotificationService.notifyParentsOfTestResult(
                studentId, "Math Test", new BigDecimal("85.00"));

        verify(notificationService, never()).sendEmail(any(), any(), any(), any(), any(), any());
    }

    @Test
    void notifyParentsOfLowScore_belowThreshold_sendsAlert() {
        ParentChild pairing = ParentChild.builder()
                .id(UUID.randomUUID()).parentId(parentId).childId(studentId)
                .status(PairingStatus.ACTIVE).build();

        when(parentChildRepository.findByChildIdAndStatus(studentId, PairingStatus.ACTIVE))
                .thenReturn(List.of(pairing));

        parentNotificationService.notifyParentsOfLowScore(
                studentId, "Physics Test", new BigDecimal("30.00"), new BigDecimal("40.00"));

        verify(notificationService).sendEmail(
                eq(parentId), eq("parent@test.com"),
                anyString(), anyString(), anyMap(), any());
    }

    @Test
    void notifyParentsOfLowScore_aboveThreshold_doesNotNotify() {
        parentNotificationService.notifyParentsOfLowScore(
                studentId, "Physics Test", new BigDecimal("75.00"), new BigDecimal("40.00"));

        verify(notificationService, never()).sendEmail(any(), any(), any(), any(), any(), any());
    }
}
