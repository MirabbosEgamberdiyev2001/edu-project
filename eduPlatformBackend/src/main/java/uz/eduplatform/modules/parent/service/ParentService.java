package uz.eduplatform.modules.parent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.core.common.exception.ResourceNotFoundException;
import uz.eduplatform.core.i18n.TranslatedField;
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
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParentService {

    private final ParentChildRepository parentChildRepository;
    private final UserRepository userRepository;
    private final TestAttemptRepository attemptRepository;
    private final TestAssignmentRepository assignmentRepository;
    private final SubjectRepository subjectRepository;
    private final TestHistoryRepository testHistoryRepository;
    private final QRCodeService qrCodeService;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;
    private static final int CODE_EXPIRY_HOURS = 24;

    /**
     * Student generates a pairing code for their parent to use.
     */
    @Transactional
    public GeneratePairingCodeResponse generatePairingCode(UUID studentId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", studentId));

        if (student.getRole() != Role.STUDENT) {
            throw BusinessException.ofKey("parent.only.students.generate.code");
        }

        String code = generateUniqueCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(CODE_EXPIRY_HOURS);

        // Create a pending pairing record (parent will be set when they use the code)
        ParentChild pairing = ParentChild.builder()
                .parentId(studentId) // temporary placeholder — will be replaced on pairing
                .childId(studentId)
                .pairingCode(code)
                .pairingCodeExpiresAt(expiresAt)
                .status(PairingStatus.PENDING)
                .build();

        parentChildRepository.save(pairing);
        log.info("Generated pairing code for student {}", studentId);

        String qrDataUri = qrCodeService.generatePairingQrCode(code);

        return GeneratePairingCodeResponse.builder()
                .pairingCode(code)
                .expiresAt(expiresAt)
                .qrCodeDataUri(qrDataUri)
                .build();
    }

    /**
     * Parent uses a pairing code to link with their child.
     */
    @Transactional
    public ParentChildDto pairWithCode(UUID parentId, PairWithCodeRequest request) {
        User parent = userRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", parentId));

        if (parent.getRole() != Role.PARENT) {
            throw BusinessException.ofKey("parent.only.parents.use.code");
        }

        ParentChild pairing = parentChildRepository.findByPairingCode(request.getPairingCode())
                .orElseThrow(() -> BusinessException.ofKey("parent.invalid.pairing.code"));

        if (pairing.getStatus() != PairingStatus.PENDING) {
            throw BusinessException.ofKey("parent.code.already.used");
        }

        if (pairing.getPairingCodeExpiresAt() != null
                && pairing.getPairingCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw BusinessException.ofKey("parent.code.expired");
        }

        UUID childId = pairing.getChildId();

        if (parentId.equals(childId)) {
            throw BusinessException.ofKey("parent.cannot.pair.self");
        }

        // Check if already paired
        if (parentChildRepository.existsByParentIdAndChildIdAndStatus(parentId, childId, PairingStatus.ACTIVE)) {
            throw BusinessException.ofKey("parent.already.paired");
        }

        // Update the pairing record
        pairing.setParentId(parentId);
        pairing.setStatus(PairingStatus.ACTIVE);
        pairing.setPairingCode(null); // consumed
        pairing.setPairingCodeExpiresAt(null);

        pairing = parentChildRepository.save(pairing);
        log.info("Parent {} paired with child {}", parentId, childId);

        return mapToDto(pairing);
    }

    /**
     * Revoke a parent-child pairing. Either parent or child can revoke.
     */
    @Transactional
    public void revokePairing(UUID pairingId, UUID userId) {
        ParentChild pairing = parentChildRepository.findById(pairingId)
                .orElseThrow(() -> new ResourceNotFoundException("ParentChild", "id", pairingId));

        if (!pairing.getParentId().equals(userId) && !pairing.getChildId().equals(userId)) {
            throw new BusinessException("error.access.denied", null, HttpStatus.FORBIDDEN);
        }

        if (pairing.getStatus() == PairingStatus.REVOKED) {
            throw BusinessException.ofKey("parent.pairing.already.revoked");
        }

        pairing.setStatus(PairingStatus.REVOKED);
        pairing.setRevokedAt(LocalDateTime.now());
        parentChildRepository.save(pairing);
        log.info("Pairing {} revoked by user {}", pairingId, userId);
    }

    /**
     * Get all children for a parent.
     */
    @Transactional(readOnly = true)
    public List<ParentChildDto> getMyChildren(UUID parentId) {
        return parentChildRepository.findByParentIdAndStatus(parentId, PairingStatus.ACTIVE)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    /**
     * Get all parents for a student.
     */
    @Transactional(readOnly = true)
    public List<ParentChildDto> getMyParents(UUID studentId) {
        return parentChildRepository.findByChildIdAndStatus(studentId, PairingStatus.ACTIVE)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    /**
     * Parent views child's academic dashboard.
     */
    @Transactional(readOnly = true)
    public ChildDashboardDto getChildDashboard(UUID parentId, UUID childId) {
        // Verify active pairing
        if (!parentChildRepository.existsByParentIdAndChildIdAndStatus(parentId, childId, PairingStatus.ACTIVE)) {
            throw new BusinessException("parent.no.active.pairing", null, HttpStatus.FORBIDDEN);
        }

        User child = userRepository.findById(childId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", childId));

        // Get attempt stats
        List<TestAttempt> attempts = attemptRepository.findByStudentIdOrderByCreatedAtDesc(childId);

        long completedCount = attempts.stream()
                .filter(a -> "SUBMITTED".equals(a.getStatus().name()) || "GRADED".equals(a.getStatus().name()))
                .count();

        BigDecimal averageScore = BigDecimal.ZERO;
        if (!attempts.isEmpty()) {
            BigDecimal totalPercentage = attempts.stream()
                    .filter(a -> a.getPercentage() != null)
                    .map(TestAttempt::getPercentage)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long scoredCount = attempts.stream().filter(a -> a.getPercentage() != null).count();
            if (scoredCount > 0) {
                averageScore = totalPercentage.divide(BigDecimal.valueOf(scoredCount), 2, RoundingMode.HALF_UP);
            }
        }

        // Recent attempts (last 10)
        List<ChildDashboardDto.RecentAttemptDto> recentAttempts = attempts.stream()
                .limit(10)
                .map(attempt -> {
                    String assignmentTitle = attempt.getAssignment() != null
                            ? attempt.getAssignment().getTitle()
                            : "Unknown";

                    return ChildDashboardDto.RecentAttemptDto.builder()
                            .attemptId(attempt.getId())
                            .assignmentTitle(assignmentTitle)
                            .percentage(attempt.getPercentage())
                            .status(attempt.getStatus().name())
                            .submittedAt(attempt.getSubmittedAt())
                            .build();
                })
                .toList();

        // Count total assignments (how many the student has been assigned to)
        String studentIdJson = "[\"" + childId + "\"]";
        long totalAssignments = assignmentRepository.findAssignmentsForStudent(
                studentIdJson, org.springframework.data.domain.Pageable.unpaged()).getTotalElements();

        // Subject breakdown
        List<ChildDashboardDto.SubjectScoreDto> subjectBreakdown = buildSubjectBreakdown(attempts);

        // Score trend
        String scoreTrend = calculateScoreTrend(attempts);

        // Weekly activity
        ChildDashboardDto.WeeklyActivityDto weeklyActivity = buildWeeklyActivity(attempts);

        return ChildDashboardDto.builder()
                .childId(childId)
                .childName(child.getFirstName() + " " + child.getLastName())
                .totalAssignments((int) totalAssignments)
                .completedAssignments((int) completedCount)
                .pendingAssignments((int) (totalAssignments - completedCount))
                .averageScore(averageScore)
                .scoreTrend(scoreTrend)
                .recentAttempts(recentAttempts)
                .subjectBreakdown(subjectBreakdown)
                .weeklyActivity(weeklyActivity)
                .build();
    }

    // ── Dashboard Helpers ──

    private List<ChildDashboardDto.SubjectScoreDto> buildSubjectBreakdown(List<TestAttempt> attempts) {
        Map<UUID, List<TestAttempt>> bySubject = new HashMap<>();

        for (TestAttempt attempt : attempts) {
            if (attempt.getAssignment() == null) continue;
            UUID testHistoryId = attempt.getAssignment().getTestHistoryId();
            if (testHistoryId == null) continue;

            testHistoryRepository.findById(testHistoryId).ifPresent(th -> {
                UUID subjectId = th.getSubjectId();
                if (subjectId != null) {
                    bySubject.computeIfAbsent(subjectId, k -> new ArrayList<>()).add(attempt);
                }
            });
        }

        return bySubject.entrySet().stream()
                .map(entry -> {
                    String subjectName = subjectRepository.findById(entry.getKey())
                            .map(s -> TranslatedField.resolve(s.getName()))
                            .orElse("Unknown");

                    BigDecimal avg = calculateAvg(entry.getValue());
                    String level = getPerformanceLevel(avg);

                    return ChildDashboardDto.SubjectScoreDto.builder()
                            .subjectName(subjectName)
                            .averageScore(avg)
                            .attemptCount(entry.getValue().size())
                            .level(level)
                            .build();
                })
                .sorted(Comparator.comparing(ChildDashboardDto.SubjectScoreDto::getSubjectName))
                .toList();
    }

    private String getPerformanceLevel(BigDecimal avg) {
        if (avg.compareTo(new BigDecimal("85")) >= 0) return "EXCELLENT";
        if (avg.compareTo(new BigDecimal("65")) >= 0) return "GOOD";
        if (avg.compareTo(new BigDecimal("40")) >= 0) return "ATTENTION";
        return "CRITICAL";
    }

    private String calculateScoreTrend(List<TestAttempt> attempts) {
        List<TestAttempt> scored = attempts.stream()
                .filter(a -> a.getPercentage() != null && a.getSubmittedAt() != null)
                .sorted(Comparator.comparing(TestAttempt::getSubmittedAt).reversed())
                .toList();
        if (scored.size() < 2) return "STABLE";

        BigDecimal recentAvg = calculateAvg(scored.stream().limit(3).toList());
        BigDecimal previousAvg = calculateAvg(scored.stream().skip(3).limit(3).toList());

        if (previousAvg.compareTo(BigDecimal.ZERO) == 0) return "STABLE";
        int cmp = recentAvg.compareTo(previousAvg);
        if (cmp > 0) return "UP";
        if (cmp < 0) return "DOWN";
        return "STABLE";
    }

    private ChildDashboardDto.WeeklyActivityDto buildWeeklyActivity(List<TestAttempt> attempts) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = now.toLocalDate().atStartOfDay();
        LocalDateTime startOfThisWeek = now.with(ChronoField.DAY_OF_WEEK, 1).toLocalDate().atStartOfDay();
        LocalDateTime startOfThisMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay();

        List<TestAttempt> today = attempts.stream()
                .filter(a -> a.getSubmittedAt() != null && a.getSubmittedAt().isAfter(startOfToday))
                .toList();
        List<TestAttempt> thisWeek = attempts.stream()
                .filter(a -> a.getSubmittedAt() != null && a.getSubmittedAt().isAfter(startOfThisWeek))
                .toList();
        List<TestAttempt> thisMonth = attempts.stream()
                .filter(a -> a.getSubmittedAt() != null && a.getSubmittedAt().isAfter(startOfThisMonth))
                .toList();

        BigDecimal avgThisWeek = calculateAvg(thisWeek);
        long timeToday = today.stream()
                .filter(a -> a.getStartedAt() != null && a.getSubmittedAt() != null)
                .mapToLong(a -> Duration.between(a.getStartedAt(), a.getSubmittedAt()).toMinutes())
                .sum();

        return ChildDashboardDto.WeeklyActivityDto.builder()
                .testsCompletedToday(today.size())
                .testsCompletedThisWeek(thisWeek.size())
                .testsCompletedThisMonth(thisMonth.size())
                .averageScoreThisWeek(avgThisWeek)
                .totalTimeSpentMinutesToday(timeToday)
                .build();
    }

    private BigDecimal calculateAvg(List<TestAttempt> attempts) {
        List<BigDecimal> scores = attempts.stream()
                .filter(a -> a.getPercentage() != null)
                .map(TestAttempt::getPercentage)
                .toList();
        if (scores.isEmpty()) return BigDecimal.ZERO;
        BigDecimal total = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
    }

    // ── Code Helpers ──

    private String generateUniqueCode() {
        for (int i = 0; i < 10; i++) {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int j = 0; j < CODE_LENGTH; j++) {
                sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            String code = sb.toString();
            if (parentChildRepository.findByPairingCode(code).isEmpty()) {
                return code;
            }
        }
        throw BusinessException.ofKey("parent.code.generation.failed");
    }

    private ParentChildDto mapToDto(ParentChild pc) {
        String parentName = userRepository.findById(pc.getParentId())
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse(null);

        User child = userRepository.findById(pc.getChildId()).orElse(null);

        return ParentChildDto.builder()
                .id(pc.getId())
                .parentId(pc.getParentId())
                .parentName(parentName)
                .childId(pc.getChildId())
                .childName(child != null ? child.getFirstName() + " " + child.getLastName() : null)
                .childEmail(child != null ? child.getEmail() : null)
                .status(pc.getStatus())
                .pairedAt(pc.getPairedAt())
                .revokedAt(pc.getRevokedAt())
                .build();
    }
}
