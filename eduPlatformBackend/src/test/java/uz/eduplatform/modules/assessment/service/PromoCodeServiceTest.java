package uz.eduplatform.modules.assessment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uz.eduplatform.core.audit.AuditService;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.core.common.exception.ResourceNotFoundException;
import uz.eduplatform.modules.assessment.domain.AssignmentPromoCode;
import uz.eduplatform.modules.assessment.domain.AssignmentStatus;
import uz.eduplatform.modules.assessment.domain.TestAssignment;
import uz.eduplatform.modules.assessment.dto.GeneratePromoCodeRequest;
import uz.eduplatform.modules.assessment.dto.PromoCodeDto;
import uz.eduplatform.modules.assessment.repository.PromoCodeRepository;
import uz.eduplatform.modules.assessment.repository.TestAssignmentRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PromoCodeServiceTest {

    @Mock private PromoCodeRepository promoCodeRepository;
    @Mock private TestAssignmentRepository assignmentRepository;
    @Mock private AuditService auditService;

    @InjectMocks
    private PromoCodeService promoCodeService;

    private UUID teacherId;
    private UUID studentId;
    private UUID assignmentId;
    private UUID promoCodeId;
    private TestAssignment activeAssignment;

    @BeforeEach
    void setUp() {
        teacherId   = UUID.randomUUID();
        studentId   = UUID.randomUUID();
        assignmentId = UUID.randomUUID();
        promoCodeId  = UUID.randomUUID();

        activeAssignment = TestAssignment.builder()
                .id(assignmentId)
                .teacherId(teacherId)
                .title("Test Assignment")
                .status(AssignmentStatus.ACTIVE)
                .assignedStudentIds(null)
                .build();

        // Default: code not yet taken
        when(promoCodeRepository.findByCodeAndIsActiveTrue(anyString())).thenReturn(Optional.empty());
        when(promoCodeRepository.save(any(AssignmentPromoCode.class)))
                .thenAnswer(inv -> {
                    AssignmentPromoCode pc = inv.getArgument(0);
                    if (pc.getId() == null) pc = AssignmentPromoCode.builder()
                            .id(promoCodeId)
                            .assignmentId(pc.getAssignmentId())
                            .code(pc.getCode())
                            .maxUses(pc.getMaxUses())
                            .currentUses(pc.getCurrentUses() != null ? pc.getCurrentUses() : 0)
                            .expiresAt(pc.getExpiresAt())
                            .isActive(pc.getIsActive() != null ? pc.getIsActive() : true)
                            .createdBy(pc.getCreatedBy())
                            .build();
                    return pc;
                });
    }

    // ─────────────────────────── generateCode ───────────────────────────

    @Test
    void generateCode_success_createsNewCode() {
        when(assignmentRepository.findByIdAndTeacherId(assignmentId, teacherId))
                .thenReturn(Optional.of(activeAssignment));
        when(promoCodeRepository.findByAssignmentIdAndIsActiveTrue(assignmentId))
                .thenReturn(Optional.empty());

        PromoCodeDto result = promoCodeService.generateCode(assignmentId, teacherId, null);

        assertNotNull(result);
        assertNotNull(result.getCode());
        assertEquals(8, result.getCode().length());
        assertTrue(result.getIsActive());
        verify(promoCodeRepository).save(any(AssignmentPromoCode.class));
    }

    @Test
    void generateCode_deactivatesExistingActiveCode() {
        AssignmentPromoCode existingCode = AssignmentPromoCode.builder()
                .id(UUID.randomUUID())
                .assignmentId(assignmentId)
                .code("OLDCODE1")
                .isActive(true)
                .currentUses(0)
                .build();

        when(assignmentRepository.findByIdAndTeacherId(assignmentId, teacherId))
                .thenReturn(Optional.of(activeAssignment));
        when(promoCodeRepository.findByAssignmentIdAndIsActiveTrue(assignmentId))
                .thenReturn(Optional.of(existingCode));

        promoCodeService.generateCode(assignmentId, teacherId, null);

        // Existing code must be deactivated
        assertFalse(existingCode.getIsActive());
        verify(promoCodeRepository, atLeastOnce()).save(existingCode);
    }

    @Test
    void generateCode_withMaxUsesAndExpiry_setsFieldsCorrectly() {
        LocalDateTime expiry = LocalDateTime.now().plusDays(7);
        GeneratePromoCodeRequest request = GeneratePromoCodeRequest.builder()
                .maxUses(50)
                .expiresAt(expiry)
                .build();

        when(assignmentRepository.findByIdAndTeacherId(assignmentId, teacherId))
                .thenReturn(Optional.of(activeAssignment));
        when(promoCodeRepository.findByAssignmentIdAndIsActiveTrue(assignmentId))
                .thenReturn(Optional.empty());

        PromoCodeDto result = promoCodeService.generateCode(assignmentId, teacherId, request);

        assertNotNull(result);
        assertEquals(50, result.getMaxUses());
        assertEquals(expiry, result.getExpiresAt());
    }

    @Test
    void generateCode_assignmentNotFound_throwsResourceNotFoundException() {
        when(assignmentRepository.findByIdAndTeacherId(assignmentId, teacherId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> promoCodeService.generateCode(assignmentId, teacherId, null));
    }

    // ─────────────────────────── redeemCode ───────────────────────────

    @Test
    void redeemCode_success_enrollsStudentInAssignment() {
        AssignmentPromoCode promoCode = AssignmentPromoCode.builder()
                .id(promoCodeId)
                .assignmentId(assignmentId)
                .code("VALIDCOD")
                .currentUses(0)
                .maxUses(null)
                .expiresAt(null)
                .isActive(true)
                .createdBy(teacherId)
                .build();

        when(promoCodeRepository.findByCodeAndIsActiveTrue("VALIDCOD"))
                .thenReturn(Optional.of(promoCode));
        when(assignmentRepository.findById(assignmentId))
                .thenReturn(Optional.of(activeAssignment));
        when(promoCodeRepository.incrementCurrentUses(promoCodeId)).thenReturn(1);

        promoCodeService.redeemCode("VALIDCOD", studentId);

        assertNotNull(activeAssignment.getAssignedStudentIds());
        assertTrue(activeAssignment.getAssignedStudentIds().contains(studentId));
        verify(assignmentRepository).save(activeAssignment);
    }

    @Test
    void redeemCode_caseInsensitive_uppercasesCode() {
        AssignmentPromoCode promoCode = AssignmentPromoCode.builder()
                .id(promoCodeId).assignmentId(assignmentId).code("VALIDCOD")
                .currentUses(0).maxUses(null).expiresAt(null).isActive(true).createdBy(teacherId)
                .build();

        when(promoCodeRepository.findByCodeAndIsActiveTrue("VALIDCOD"))
                .thenReturn(Optional.of(promoCode));
        when(assignmentRepository.findById(assignmentId))
                .thenReturn(Optional.of(activeAssignment));
        when(promoCodeRepository.incrementCurrentUses(promoCodeId)).thenReturn(1);

        // lowercase input
        promoCodeService.redeemCode("validcod", studentId);

        verify(promoCodeRepository).findByCodeAndIsActiveTrue("VALIDCOD");
    }

    @Test
    void redeemCode_studentAlreadyHasOtherStudents_appendsToExistingList() {
        UUID otherStudent = UUID.randomUUID();
        List<UUID> existing = new ArrayList<>();
        existing.add(otherStudent);
        activeAssignment.setAssignedStudentIds(existing);

        AssignmentPromoCode promoCode = AssignmentPromoCode.builder()
                .id(promoCodeId).assignmentId(assignmentId).code("VALIDCOD")
                .currentUses(0).maxUses(null).expiresAt(null).isActive(true).createdBy(teacherId)
                .build();

        when(promoCodeRepository.findByCodeAndIsActiveTrue("VALIDCOD"))
                .thenReturn(Optional.of(promoCode));
        when(assignmentRepository.findById(assignmentId))
                .thenReturn(Optional.of(activeAssignment));
        when(promoCodeRepository.incrementCurrentUses(promoCodeId)).thenReturn(1);

        promoCodeService.redeemCode("VALIDCOD", studentId);

        assertTrue(activeAssignment.getAssignedStudentIds().contains(otherStudent));
        assertTrue(activeAssignment.getAssignedStudentIds().contains(studentId));
    }

    @Test
    void redeemCode_invalidCode_throwsBusinessException() {
        when(promoCodeRepository.findByCodeAndIsActiveTrue("BADCODE1"))
                .thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> promoCodeService.redeemCode("BADCODE1", studentId));
    }

    @Test
    void redeemCode_expiredCode_throwsBusinessException() {
        AssignmentPromoCode promoCode = AssignmentPromoCode.builder()
                .id(promoCodeId).assignmentId(assignmentId).code("EXPCODE1")
                .currentUses(0).maxUses(null)
                .expiresAt(LocalDateTime.now().minusHours(1)) // expired
                .isActive(true).createdBy(teacherId)
                .build();

        when(promoCodeRepository.findByCodeAndIsActiveTrue("EXPCODE1"))
                .thenReturn(Optional.of(promoCode));

        assertThrows(BusinessException.class,
                () -> promoCodeService.redeemCode("EXPCODE1", studentId));
    }

    @Test
    void redeemCode_exhaustedCode_throwsBusinessException() {
        AssignmentPromoCode promoCode = AssignmentPromoCode.builder()
                .id(promoCodeId).assignmentId(assignmentId).code("FULLCOD1")
                .currentUses(10).maxUses(10).expiresAt(null)
                .isActive(true).createdBy(teacherId)
                .build();

        when(promoCodeRepository.findByCodeAndIsActiveTrue("FULLCOD1"))
                .thenReturn(Optional.of(promoCode));

        assertThrows(BusinessException.class,
                () -> promoCodeService.redeemCode("FULLCOD1", studentId));
    }

    @Test
    void redeemCode_assignmentNotActive_throwsBusinessException() {
        TestAssignment draftAssignment = TestAssignment.builder()
                .id(assignmentId).teacherId(teacherId)
                .status(AssignmentStatus.DRAFT)
                .build();

        AssignmentPromoCode promoCode = AssignmentPromoCode.builder()
                .id(promoCodeId).assignmentId(assignmentId).code("DRAFTCO1")
                .currentUses(0).maxUses(null).expiresAt(null)
                .isActive(true).createdBy(teacherId)
                .build();

        when(promoCodeRepository.findByCodeAndIsActiveTrue("DRAFTCO1"))
                .thenReturn(Optional.of(promoCode));
        when(assignmentRepository.findById(assignmentId))
                .thenReturn(Optional.of(draftAssignment));

        assertThrows(BusinessException.class,
                () -> promoCodeService.redeemCode("DRAFTCO1", studentId));
    }

    @Test
    void redeemCode_assignmentNotFound_throwsBusinessException() {
        AssignmentPromoCode promoCode = AssignmentPromoCode.builder()
                .id(promoCodeId).assignmentId(assignmentId).code("NOASGN1X")
                .currentUses(0).maxUses(null).expiresAt(null)
                .isActive(true).createdBy(teacherId)
                .build();

        when(promoCodeRepository.findByCodeAndIsActiveTrue("NOASGN1X"))
                .thenReturn(Optional.of(promoCode));
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> promoCodeService.redeemCode("NOASGN1X", studentId));
    }

    @Test
    void redeemCode_studentAlreadyEnrolled_throwsBusinessException() {
        List<UUID> enrolled = new ArrayList<>();
        enrolled.add(studentId);
        activeAssignment.setAssignedStudentIds(enrolled);

        AssignmentPromoCode promoCode = AssignmentPromoCode.builder()
                .id(promoCodeId).assignmentId(assignmentId).code("DUPCOD11")
                .currentUses(1).maxUses(null).expiresAt(null)
                .isActive(true).createdBy(teacherId)
                .build();

        when(promoCodeRepository.findByCodeAndIsActiveTrue("DUPCOD11"))
                .thenReturn(Optional.of(promoCode));
        when(assignmentRepository.findById(assignmentId))
                .thenReturn(Optional.of(activeAssignment));

        assertThrows(BusinessException.class,
                () -> promoCodeService.redeemCode("DUPCOD11", studentId));
        verify(promoCodeRepository, never()).incrementCurrentUses(any());
    }

    @Test
    void redeemCode_raceConditionExhaustion_throwsBusinessException() {
        AssignmentPromoCode promoCode = AssignmentPromoCode.builder()
                .id(promoCodeId).assignmentId(assignmentId).code("RACECOD1")
                .currentUses(9).maxUses(10).expiresAt(null)
                .isActive(true).createdBy(teacherId)
                .build();

        when(promoCodeRepository.findByCodeAndIsActiveTrue("RACECOD1"))
                .thenReturn(Optional.of(promoCode));
        when(assignmentRepository.findById(assignmentId))
                .thenReturn(Optional.of(activeAssignment));
        // Atomic increment fails — another thread used the last slot
        when(promoCodeRepository.incrementCurrentUses(promoCodeId)).thenReturn(0);

        assertThrows(BusinessException.class,
                () -> promoCodeService.redeemCode("RACECOD1", studentId));
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void redeemCode_notYetExpiredCode_succeeds() {
        AssignmentPromoCode promoCode = AssignmentPromoCode.builder()
                .id(promoCodeId).assignmentId(assignmentId).code("FUTEXP11")
                .currentUses(0).maxUses(null)
                .expiresAt(LocalDateTime.now().plusHours(1)) // not expired yet
                .isActive(true).createdBy(teacherId)
                .build();

        when(promoCodeRepository.findByCodeAndIsActiveTrue("FUTEXP11"))
                .thenReturn(Optional.of(promoCode));
        when(assignmentRepository.findById(assignmentId))
                .thenReturn(Optional.of(activeAssignment));
        when(promoCodeRepository.incrementCurrentUses(promoCodeId)).thenReturn(1);

        assertDoesNotThrow(() -> promoCodeService.redeemCode("FUTEXP11", studentId));
    }

    // ─────────────────────────── revokeCode ───────────────────────────

    @Test
    void revokeCode_success_deactivatesActiveCode() {
        AssignmentPromoCode activeCode = AssignmentPromoCode.builder()
                .id(promoCodeId).assignmentId(assignmentId).code("REVOKCOD")
                .isActive(true).currentUses(3).createdBy(teacherId)
                .build();

        when(assignmentRepository.findByIdAndTeacherId(assignmentId, teacherId))
                .thenReturn(Optional.of(activeAssignment));
        when(promoCodeRepository.findByAssignmentIdAndIsActiveTrue(assignmentId))
                .thenReturn(Optional.of(activeCode));

        promoCodeService.revokeCode(assignmentId, teacherId);

        assertFalse(activeCode.getIsActive());
        verify(promoCodeRepository).save(activeCode);
    }

    @Test
    void revokeCode_noActiveCode_doesNothing() {
        when(assignmentRepository.findByIdAndTeacherId(assignmentId, teacherId))
                .thenReturn(Optional.of(activeAssignment));
        when(promoCodeRepository.findByAssignmentIdAndIsActiveTrue(assignmentId))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> promoCodeService.revokeCode(assignmentId, teacherId));
        verify(promoCodeRepository, never()).save(any());
    }

    @Test
    void revokeCode_assignmentNotFound_throwsResourceNotFoundException() {
        when(assignmentRepository.findByIdAndTeacherId(assignmentId, teacherId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> promoCodeService.revokeCode(assignmentId, teacherId));
    }

    // ─────────────────────────── getActivePromoCode ───────────────────────────

    @Test
    void getActivePromoCode_returnsActiveCodeDto() {
        AssignmentPromoCode activeCode = AssignmentPromoCode.builder()
                .id(promoCodeId).assignmentId(assignmentId).code("GETTEST1")
                .maxUses(100).currentUses(7).expiresAt(null)
                .isActive(true).createdBy(teacherId)
                .build();

        when(assignmentRepository.findByIdAndTeacherId(assignmentId, teacherId))
                .thenReturn(Optional.of(activeAssignment));
        when(promoCodeRepository.findByAssignmentIdAndIsActiveTrue(assignmentId))
                .thenReturn(Optional.of(activeCode));

        PromoCodeDto result = promoCodeService.getActivePromoCode(assignmentId, teacherId);

        assertNotNull(result);
        assertEquals("GETTEST1", result.getCode());
        assertEquals(100, result.getMaxUses());
        assertEquals(7, result.getCurrentUses());
        assertTrue(result.getIsActive());
    }

    @Test
    void getActivePromoCode_noActiveCode_returnsNull() {
        when(assignmentRepository.findByIdAndTeacherId(assignmentId, teacherId))
                .thenReturn(Optional.of(activeAssignment));
        when(promoCodeRepository.findByAssignmentIdAndIsActiveTrue(assignmentId))
                .thenReturn(Optional.empty());

        PromoCodeDto result = promoCodeService.getActivePromoCode(assignmentId, teacherId);

        assertNull(result);
    }

    @Test
    void getActivePromoCode_assignmentNotFound_throwsResourceNotFoundException() {
        when(assignmentRepository.findByIdAndTeacherId(assignmentId, teacherId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> promoCodeService.getActivePromoCode(assignmentId, teacherId));
    }
}
