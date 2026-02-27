package uz.eduplatform.modules.assessment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromoCodeService {

    private final PromoCodeRepository promoCodeRepository;
    private final TestAssignmentRepository assignmentRepository;
    private final AuditService auditService;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    @Transactional
    public PromoCodeDto generateCode(UUID assignmentId, UUID teacherId, GeneratePromoCodeRequest request) {
        TestAssignment assignment = assignmentRepository.findByIdAndTeacherId(assignmentId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("TestAssignment", "id", assignmentId));

        // Deactivate any existing active promo code for this assignment
        promoCodeRepository.findByAssignmentIdAndIsActiveTrue(assignmentId)
                .ifPresent(existing -> {
                    existing.setIsActive(false);
                    promoCodeRepository.save(existing);
                });

        String code = generateUniqueCode();

        AssignmentPromoCode promoCode = AssignmentPromoCode.builder()
                .assignmentId(assignmentId)
                .code(code)
                .maxUses(request != null ? request.getMaxUses() : null)
                .expiresAt(request != null ? request.getExpiresAt() : null)
                .createdBy(teacherId)
                .build();

        promoCode = promoCodeRepository.save(promoCode);

        auditService.log(teacherId, "TEACHER", "PROMO_CODE_GENERATED", "ASSESSMENT",
                "AssignmentPromoCode", promoCode.getId(),
                null, Map.of("assignmentId", assignmentId.toString(), "code", code));

        log.info("Teacher {} generated promo code for assignment {}", teacherId, assignmentId);
        return mapToDto(promoCode);
    }

    @Transactional
    public void redeemCode(String code, UUID studentId) {
        AssignmentPromoCode promoCode = promoCodeRepository.findByCodeAndIsActiveTrue(code.toUpperCase().trim())
                .orElseThrow(() -> BusinessException.ofKey("promo.code.invalid"));

        // Validate not expired
        if (promoCode.getExpiresAt() != null && LocalDateTime.now().isAfter(promoCode.getExpiresAt())) {
            throw BusinessException.ofKey("promo.code.expired");
        }

        // Validate not exhausted
        if (promoCode.getMaxUses() != null && promoCode.getCurrentUses() >= promoCode.getMaxUses()) {
            throw BusinessException.ofKey("promo.code.exhausted");
        }

        // Validate assignment exists and is ACTIVE
        TestAssignment assignment = assignmentRepository.findById(promoCode.getAssignmentId())
                .orElseThrow(() -> BusinessException.ofKey("promo.code.assignment.not.found"));

        if (assignment.getStatus() != AssignmentStatus.ACTIVE) {
            throw BusinessException.ofKey("promo.code.assignment.not.active");
        }

        // Check if student is already assigned
        List<UUID> assignedIds = assignment.getAssignedStudentIds();
        if (assignedIds != null && assignedIds.contains(studentId)) {
            throw BusinessException.ofKey("promo.code.already.enrolled");
        }

        // Atomic increment (returns 0 if code was exhausted between validation and increment)
        int updated = promoCodeRepository.incrementCurrentUses(promoCode.getId());
        if (updated == 0) {
            throw BusinessException.ofKey("promo.code.exhausted");
        }

        // Add student to assignedStudentIds
        if (assignedIds == null) {
            assignedIds = new ArrayList<>();
        } else {
            assignedIds = new ArrayList<>(assignedIds);
        }
        assignedIds.add(studentId);
        assignment.setAssignedStudentIds(assignedIds);
        assignmentRepository.save(assignment);

        auditService.log(studentId, "STUDENT", "PROMO_CODE_REDEEMED", "ASSESSMENT",
                "TestAssignment", assignment.getId(),
                null, Map.of("promoCodeId", promoCode.getId().toString(), "code", code));

        log.info("Student {} redeemed promo code '{}' for assignment {}", studentId, code, assignment.getId());
    }

    @Transactional
    public void revokeCode(UUID assignmentId, UUID teacherId) {
        assignmentRepository.findByIdAndTeacherId(assignmentId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("TestAssignment", "id", assignmentId));

        promoCodeRepository.findByAssignmentIdAndIsActiveTrue(assignmentId)
                .ifPresent(promoCode -> {
                    promoCode.setIsActive(false);
                    promoCodeRepository.save(promoCode);

                    auditService.log(teacherId, "TEACHER", "PROMO_CODE_REVOKED", "ASSESSMENT",
                            "AssignmentPromoCode", promoCode.getId());

                    log.info("Teacher {} revoked promo code for assignment {}", teacherId, assignmentId);
                });
    }

    @Transactional(readOnly = true)
    public PromoCodeDto getActivePromoCode(UUID assignmentId, UUID teacherId) {
        assignmentRepository.findByIdAndTeacherId(assignmentId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("TestAssignment", "id", assignmentId));

        return promoCodeRepository.findByAssignmentIdAndIsActiveTrue(assignmentId)
                .map(this::mapToDto)
                .orElse(null);
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < 8; i++) {
                sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
            }
            String code = sb.toString();
            if (promoCodeRepository.findByCodeAndIsActiveTrue(code).isEmpty()) {
                return code;
            }
        }
        throw BusinessException.ofKey("promo.code.generation.failed");
    }

    private PromoCodeDto mapToDto(AssignmentPromoCode pc) {
        return PromoCodeDto.builder()
                .id(pc.getId())
                .assignmentId(pc.getAssignmentId())
                .code(pc.getCode())
                .maxUses(pc.getMaxUses())
                .currentUses(pc.getCurrentUses())
                .expiresAt(pc.getExpiresAt())
                .isActive(pc.getIsActive())
                .createdAt(pc.getCreatedAt())
                .build();
    }
}
