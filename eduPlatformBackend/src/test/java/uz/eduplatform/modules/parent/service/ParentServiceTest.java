package uz.eduplatform.modules.parent.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.modules.assessment.domain.AttemptStatus;
import uz.eduplatform.modules.assessment.domain.TestAssignment;
import uz.eduplatform.modules.assessment.domain.TestAttempt;
import uz.eduplatform.modules.assessment.repository.TestAssignmentRepository;
import uz.eduplatform.modules.assessment.repository.TestAttemptRepository;
import uz.eduplatform.modules.content.repository.SubjectRepository;
import uz.eduplatform.modules.test.repository.TestHistoryRepository;
import uz.eduplatform.modules.auth.domain.Role;
import uz.eduplatform.modules.auth.domain.User;
import uz.eduplatform.modules.auth.repository.UserRepository;
import uz.eduplatform.modules.parent.domain.PairingStatus;
import uz.eduplatform.modules.parent.domain.ParentChild;
import uz.eduplatform.modules.parent.dto.*;
import uz.eduplatform.modules.parent.repository.ParentChildRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ParentServiceTest {

    @Mock private ParentChildRepository parentChildRepository;
    @Mock private UserRepository userRepository;
    @Mock private TestAttemptRepository attemptRepository;
    @Mock private TestAssignmentRepository assignmentRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private TestHistoryRepository testHistoryRepository;
    @Mock private QRCodeService qrCodeService;

    @InjectMocks private ParentService parentService;

    private UUID parentId;
    private UUID studentId;
    private UUID pairingId;
    private User parent;
    private User student;

    @BeforeEach
    void setUp() {
        parentId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        pairingId = UUID.randomUUID();

        parent = User.builder()
                .id(parentId).firstName("Parent").lastName("User")
                .role(Role.PARENT).email("parent@test.com").build();

        student = User.builder()
                .id(studentId).firstName("Student").lastName("User")
                .role(Role.STUDENT).email("student@test.com").build();

        when(userRepository.findById(parentId)).thenReturn(Optional.of(parent));
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    }

    // ── Generate Pairing Code ──

    @Test
    void generatePairingCode_success() {
        when(parentChildRepository.findByPairingCode(anyString())).thenReturn(Optional.empty());
        when(parentChildRepository.save(any(ParentChild.class))).thenAnswer(inv -> {
            ParentChild pc = inv.getArgument(0);
            pc.setId(UUID.randomUUID());
            return pc;
        });

        GeneratePairingCodeResponse response = parentService.generatePairingCode(studentId);

        assertThat(response).isNotNull();
        assertThat(response.getPairingCode()).hasSize(8);
        assertThat(response.getExpiresAt()).isAfter(LocalDateTime.now());
        verify(parentChildRepository).save(any(ParentChild.class));
    }

    @Test
    void generatePairingCode_notStudent_throws() {
        assertThatThrownBy(() -> parentService.generatePairingCode(parentId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("parent.only.students.generate.code");
    }

    // ── Pair With Code ──

    @Test
    void pairWithCode_success() {
        ParentChild pairing = ParentChild.builder()
                .id(pairingId).parentId(studentId).childId(studentId)
                .pairingCode("ABCD1234").status(PairingStatus.PENDING)
                .pairingCodeExpiresAt(LocalDateTime.now().plusHours(24)).build();

        when(parentChildRepository.findByPairingCode("ABCD1234")).thenReturn(Optional.of(pairing));
        when(parentChildRepository.existsByParentIdAndChildIdAndStatus(
                parentId, studentId, PairingStatus.ACTIVE)).thenReturn(false);
        when(parentChildRepository.save(any(ParentChild.class))).thenReturn(pairing);

        PairWithCodeRequest request = PairWithCodeRequest.builder()
                .pairingCode("ABCD1234").build();

        ParentChildDto result = parentService.pairWithCode(parentId, request);

        assertThat(result).isNotNull();
        assertThat(pairing.getStatus()).isEqualTo(PairingStatus.ACTIVE);
        assertThat(pairing.getPairingCode()).isNull(); // consumed
    }

    @Test
    void pairWithCode_invalidCode_throws() {
        when(parentChildRepository.findByPairingCode("INVALID1")).thenReturn(Optional.empty());

        PairWithCodeRequest request = PairWithCodeRequest.builder()
                .pairingCode("INVALID1").build();

        assertThatThrownBy(() -> parentService.pairWithCode(parentId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("parent.invalid.pairing.code");
    }

    @Test
    void pairWithCode_expiredCode_throws() {
        ParentChild pairing = ParentChild.builder()
                .id(pairingId).parentId(studentId).childId(studentId)
                .pairingCode("EXPIRED1").status(PairingStatus.PENDING)
                .pairingCodeExpiresAt(LocalDateTime.now().minusHours(1)).build();

        when(parentChildRepository.findByPairingCode("EXPIRED1")).thenReturn(Optional.of(pairing));

        PairWithCodeRequest request = PairWithCodeRequest.builder()
                .pairingCode("EXPIRED1").build();

        assertThatThrownBy(() -> parentService.pairWithCode(parentId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("parent.code.expired");
    }

    @Test
    void pairWithCode_alreadyUsed_throws() {
        ParentChild pairing = ParentChild.builder()
                .id(pairingId).parentId(studentId).childId(studentId)
                .pairingCode("USED1234").status(PairingStatus.ACTIVE).build();

        when(parentChildRepository.findByPairingCode("USED1234")).thenReturn(Optional.of(pairing));

        PairWithCodeRequest request = PairWithCodeRequest.builder()
                .pairingCode("USED1234").build();

        assertThatThrownBy(() -> parentService.pairWithCode(parentId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("parent.code.already.used");
    }

    @Test
    void pairWithCode_alreadyPaired_throws() {
        ParentChild pairing = ParentChild.builder()
                .id(pairingId).parentId(studentId).childId(studentId)
                .pairingCode("DUPE1234").status(PairingStatus.PENDING)
                .pairingCodeExpiresAt(LocalDateTime.now().plusHours(24)).build();

        when(parentChildRepository.findByPairingCode("DUPE1234")).thenReturn(Optional.of(pairing));
        when(parentChildRepository.existsByParentIdAndChildIdAndStatus(
                parentId, studentId, PairingStatus.ACTIVE)).thenReturn(true);

        PairWithCodeRequest request = PairWithCodeRequest.builder()
                .pairingCode("DUPE1234").build();

        assertThatThrownBy(() -> parentService.pairWithCode(parentId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("parent.already.paired");
    }

    @Test
    void pairWithCode_notParent_throws() {
        PairWithCodeRequest request = PairWithCodeRequest.builder()
                .pairingCode("ABCD1234").build();

        // studentId has role STUDENT, not PARENT
        assertThatThrownBy(() -> parentService.pairWithCode(studentId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("parent.only.parents.use.code");
    }

    // ── Revoke ──

    @Test
    void revokePairing_byParent_success() {
        ParentChild pairing = ParentChild.builder()
                .id(pairingId).parentId(parentId).childId(studentId)
                .status(PairingStatus.ACTIVE).build();

        when(parentChildRepository.findById(pairingId)).thenReturn(Optional.of(pairing));
        when(parentChildRepository.save(any(ParentChild.class))).thenReturn(pairing);

        parentService.revokePairing(pairingId, parentId);

        assertThat(pairing.getStatus()).isEqualTo(PairingStatus.REVOKED);
        assertThat(pairing.getRevokedAt()).isNotNull();
    }

    @Test
    void revokePairing_byChild_success() {
        ParentChild pairing = ParentChild.builder()
                .id(pairingId).parentId(parentId).childId(studentId)
                .status(PairingStatus.ACTIVE).build();

        when(parentChildRepository.findById(pairingId)).thenReturn(Optional.of(pairing));
        when(parentChildRepository.save(any(ParentChild.class))).thenReturn(pairing);

        parentService.revokePairing(pairingId, studentId);

        assertThat(pairing.getStatus()).isEqualTo(PairingStatus.REVOKED);
    }

    @Test
    void revokePairing_unrelatedUser_throws() {
        UUID otherUserId = UUID.randomUUID();
        ParentChild pairing = ParentChild.builder()
                .id(pairingId).parentId(parentId).childId(studentId)
                .status(PairingStatus.ACTIVE).build();

        when(parentChildRepository.findById(pairingId)).thenReturn(Optional.of(pairing));

        assertThatThrownBy(() -> parentService.revokePairing(pairingId, otherUserId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("error.access.denied");
    }

    @Test
    void revokePairing_alreadyRevoked_throws() {
        ParentChild pairing = ParentChild.builder()
                .id(pairingId).parentId(parentId).childId(studentId)
                .status(PairingStatus.REVOKED).build();

        when(parentChildRepository.findById(pairingId)).thenReturn(Optional.of(pairing));

        assertThatThrownBy(() -> parentService.revokePairing(pairingId, parentId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("parent.pairing.already.revoked");
    }

    // ── Get Children / Parents ──

    @Test
    void getMyChildren_returnsActiveChildren() {
        ParentChild pairing = ParentChild.builder()
                .id(pairingId).parentId(parentId).childId(studentId)
                .status(PairingStatus.ACTIVE).build();

        when(parentChildRepository.findByParentIdAndStatus(parentId, PairingStatus.ACTIVE))
                .thenReturn(List.of(pairing));

        List<ParentChildDto> result = parentService.getMyChildren(parentId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChildName()).isEqualTo("Student User");
    }

    @Test
    void getMyParents_returnsActiveParents() {
        ParentChild pairing = ParentChild.builder()
                .id(pairingId).parentId(parentId).childId(studentId)
                .status(PairingStatus.ACTIVE).build();

        when(parentChildRepository.findByChildIdAndStatus(studentId, PairingStatus.ACTIVE))
                .thenReturn(List.of(pairing));

        List<ParentChildDto> result = parentService.getMyParents(studentId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getParentName()).isEqualTo("Parent User");
    }

    // ── Child Dashboard ──

    @Test
    void getChildDashboard_success() {
        when(parentChildRepository.existsByParentIdAndChildIdAndStatus(
                parentId, studentId, PairingStatus.ACTIVE)).thenReturn(true);

        TestAssignment assignment = TestAssignment.builder()
                .id(UUID.randomUUID()).title("Math Test").build();
        TestAttempt attempt = TestAttempt.builder()
                .id(UUID.randomUUID()).studentId(studentId)
                .assignment(assignment).status(AttemptStatus.GRADED)
                .percentage(new BigDecimal("85.00"))
                .submittedAt(LocalDateTime.now()).build();

        when(attemptRepository.findByStudentIdOrderByCreatedAtDesc(studentId))
                .thenReturn(List.of(attempt));

        Page<TestAssignment> assignmentPage = new PageImpl<>(List.of(assignment));
        when(assignmentRepository.findAssignmentsForStudent(anyString(), any(Pageable.class)))
                .thenReturn(assignmentPage);

        ChildDashboardDto result = parentService.getChildDashboard(parentId, studentId);

        assertThat(result).isNotNull();
        assertThat(result.getChildName()).isEqualTo("Student User");
        assertThat(result.getAverageScore()).isEqualByComparingTo("85.00");
        assertThat(result.getRecentAttempts()).hasSize(1);
    }

    @Test
    void getChildDashboard_notPaired_throws() {
        when(parentChildRepository.existsByParentIdAndChildIdAndStatus(
                parentId, studentId, PairingStatus.ACTIVE)).thenReturn(false);

        assertThatThrownBy(() -> parentService.getChildDashboard(parentId, studentId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("parent.no.active.pairing");
    }
}
