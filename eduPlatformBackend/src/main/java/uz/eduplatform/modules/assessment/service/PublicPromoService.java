package uz.eduplatform.modules.assessment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.eduplatform.core.audit.AuditService;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.core.security.JwtTokenProvider;
import uz.eduplatform.modules.assessment.domain.AssignmentPromoCode;
import uz.eduplatform.modules.assessment.domain.AssignmentStatus;
import uz.eduplatform.modules.assessment.domain.TestAssignment;
import uz.eduplatform.modules.assessment.dto.PromoEnrollResultDto;
import uz.eduplatform.modules.assessment.dto.PublicPromoEnrollRequest;
import uz.eduplatform.modules.assessment.dto.PublicPromoInfoDto;
import uz.eduplatform.modules.assessment.repository.PromoCodeRepository;
import uz.eduplatform.modules.assessment.repository.TestAssignmentRepository;
import uz.eduplatform.modules.auth.domain.Role;
import uz.eduplatform.modules.auth.domain.User;
import uz.eduplatform.modules.auth.domain.UserStatus;
import uz.eduplatform.modules.auth.repository.UserRepository;
import uz.eduplatform.modules.test.domain.TestHistory;
import uz.eduplatform.modules.test.repository.TestHistoryRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicPromoService {

    private final PromoCodeRepository promoCodeRepository;
    private final TestAssignmentRepository assignmentRepository;
    private final TestHistoryRepository testHistoryRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuditService auditService;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PASSWORD_CHARS = "ABCDEFGHIJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#";

    @Transactional(readOnly = true)
    public PublicPromoInfoDto getPublicPromoInfo(String code) {
        AssignmentPromoCode promoCode = validatePromoCode(code);

        TestAssignment assignment = assignmentRepository.findById(promoCode.getAssignmentId())
                .orElseThrow(() -> BusinessException.ofKey("promo.code.assignment.not.found"));

        if (assignment.getStatus() != AssignmentStatus.ACTIVE) {
            throw BusinessException.ofKey("promo.code.assignment.not.active");
        }

        // Get test info
        String testTitle = assignment.getTitle();
        Integer questionCount = null;
        TestHistory testHistory = testHistoryRepository
                .findByIdAndDeletedAtIsNull(assignment.getTestHistoryId())
                .orElse(null);
        if (testHistory != null) {
            testTitle = testHistory.getTitle();
            questionCount = testHistory.getQuestionCount();
        }

        // Get teacher info
        String teacherFullName = null;
        userRepository.findById(assignment.getTeacherId()).ifPresent(teacher -> {
            // captured below
        });
        User teacher = userRepository.findById(assignment.getTeacherId()).orElse(null);
        if (teacher != null) {
            teacherFullName = teacher.getFirstName() + " " + teacher.getLastName();
        }

        return PublicPromoInfoDto.builder()
                .assignmentId(assignment.getId())
                .assignmentTitle(assignment.getTitle())
                .testTitle(testTitle)
                .questionCount(questionCount)
                .durationMinutes(assignment.getDurationMinutes())
                .teacherFullName(teacherFullName)
                .startsAt(assignment.getStartTime())
                .endsAt(assignment.getEndTime())
                .maxAttempts(assignment.getMaxAttempts())
                .showResults(assignment.getShowResults())
                .build();
    }

    @Transactional
    public PromoEnrollResultDto autoEnrollStudent(String code, PublicPromoEnrollRequest request) {
        AssignmentPromoCode promoCode = validatePromoCode(code);

        TestAssignment assignment = assignmentRepository.findById(promoCode.getAssignmentId())
                .orElseThrow(() -> BusinessException.ofKey("promo.code.assignment.not.found"));

        if (assignment.getStatus() != AssignmentStatus.ACTIVE) {
            throw BusinessException.ofKey("promo.code.assignment.not.active");
        }

        // Find or create student user by phone
        String normalizedPhone = request.getPhone().trim();
        User student = userRepository.findByPhone(normalizedPhone).orElseGet(() ->
                createPromoStudent(request.getFirstName().trim(), request.getLastName().trim(), normalizedPhone));

        // Validate role: only STUDENTs can use promo codes
        if (student.getRole() != Role.STUDENT) {
            throw BusinessException.ofKey("promo.phone.not.student");
        }

        // Check if already enrolled — idempotent: return success if already in list
        List<UUID> assignedIds = assignment.getAssignedStudentIds();
        boolean alreadyEnrolled = assignedIds != null && assignedIds.contains(student.getId());

        if (!alreadyEnrolled) {
            // Atomic increment — race condition protection
            int updated = promoCodeRepository.incrementCurrentUses(promoCode.getId());
            if (updated == 0) {
                throw BusinessException.ofKey("promo.code.exhausted");
            }

            // Add student to assignedStudentIds
            List<UUID> mutableIds = assignedIds != null ? new ArrayList<>(assignedIds) : new ArrayList<>();
            mutableIds.add(student.getId());
            assignment.setAssignedStudentIds(mutableIds);
            assignmentRepository.save(assignment);

            auditService.log(student.getId(), "STUDENT", "PROMO_CODE_REDEEMED", "ASSESSMENT",
                    "TestAssignment", assignment.getId(),
                    null, Map.of("promoCodeId", promoCode.getId().toString(), "code", code));

            log.info("Student {} enrolled via promo code '{}' for assignment {}", student.getId(), code, assignment.getId());
        } else {
            log.info("Student {} already enrolled in assignment {} via promo code '{}'", student.getId(), assignment.getId(), code);
        }

        // Generate JWT for the student
        String accessToken = tokenProvider.generateAccessToken(
                student.getId(), student.getEmail(), student.getPhone(), student.getRole());

        return PromoEnrollResultDto.builder()
                .accessToken(accessToken)
                .assignmentId(assignment.getId())
                .studentId(student.getId())
                .build();
    }

    private AssignmentPromoCode validatePromoCode(String code) {
        AssignmentPromoCode promoCode = promoCodeRepository
                .findByCodeAndIsActiveTrue(code.toUpperCase().trim())
                .orElseThrow(() -> BusinessException.ofKey("promo.code.invalid"));

        if (promoCode.getExpiresAt() != null && LocalDateTime.now().isAfter(promoCode.getExpiresAt())) {
            throw BusinessException.ofKey("promo.code.expired");
        }

        if (promoCode.getMaxUses() != null && promoCode.getCurrentUses() >= promoCode.getMaxUses()) {
            throw BusinessException.ofKey("promo.code.exhausted");
        }

        return promoCode;
    }

    private User createPromoStudent(String firstName, String lastName, String phone) {
        // Generate a random secure password — student can reset it later if needed
        String randomPassword = generateRandomPassword();

        User user = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .phone(phone)
                .passwordHash(passwordEncoder.encode(randomPassword))
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .phoneVerified(true)
                .passwordChangedAt(LocalDateTime.now())
                .build();

        user = userRepository.save(user);

        auditService.log(user.getId(), "STUDENT", "PROMO_AUTO_REGISTERED", "AUTH",
                "User", user.getId());

        log.info("Auto-registered student {} via promo code", user.getId());
        return user;
    }

    private String generateRandomPassword() {
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}
