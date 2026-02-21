package uz.eduplatform.modules.subscription.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.modules.auth.domain.Role;
import uz.eduplatform.modules.auth.domain.User;
import uz.eduplatform.modules.auth.repository.UserRepository;
import uz.eduplatform.modules.subscription.domain.*;
import uz.eduplatform.modules.subscription.dto.UsageDto;
import uz.eduplatform.modules.subscription.repository.UsageRecordRepository;
import uz.eduplatform.modules.subscription.repository.UserSubscriptionRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsageTrackingService {

    private final UsageRecordRepository usageRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    /**
     * Check if a user can perform an action, then increment usage.
     * ADMIN/SUPER_ADMIN bypass all limits.
     */
    @Transactional
    public void trackAndCheckLimit(UUID userId, UsageType usageType) {
        // ADMIN/SUPER_ADMIN bypass all limits
        if (isAdminOrSuperAdmin(userId)) {
            incrementUsage(userId, usageType);
            return;
        }

        int limit = getLimit(userId, usageType);
        // -1 means unlimited
        if (limit == -1) {
            incrementUsage(userId, usageType);
            return;
        }

        int currentUsage = getCurrentUsage(userId, usageType);
        if (currentUsage >= limit) {
            throw BusinessException.ofKey("usage.limit.reached", usageType.name(), currentUsage, limit);
        }

        incrementUsage(userId, usageType);
    }

    /**
     * Check limit without incrementing. Returns true if action is allowed.
     */
    @Transactional(readOnly = true)
    public boolean canPerform(UUID userId, UsageType usageType) {
        if (isAdminOrSuperAdmin(userId)) return true;

        int limit = getLimit(userId, usageType);
        if (limit == -1) return true;

        int currentUsage = getCurrentUsage(userId, usageType);
        return currentUsage < limit;
    }

    /**
     * Get current daily usage for a type.
     */
    @Transactional(readOnly = true)
    public int getCurrentUsage(UUID userId, UsageType usageType) {
        if (usageType == UsageType.TEST_GENERATION || usageType == UsageType.TEST_ATTEMPT) {
            // Daily limit types
            return usageRepository.findByUserIdAndUsageTypeAndUsageDate(userId, usageType, LocalDate.now())
                    .map(UsageRecord::getCount)
                    .orElse(0);
        } else {
            // Monthly limit types
            LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
            return usageRepository.sumCountByUserIdAndTypeAndDateRange(userId, usageType, monthStart, monthEnd);
        }
    }

    /**
     * Get usage summary for a user.
     */
    @Transactional(readOnly = true)
    public List<UsageDto> getUserUsageSummary(UUID userId) {
        return java.util.Arrays.stream(UsageType.values())
                .map(type -> {
                    int limit = getLimit(userId, type);
                    return UsageDto.builder()
                            .userId(userId)
                            .usageType(type)
                            .date(LocalDate.now())
                            .count(getCurrentUsage(userId, type))
                            .limit(limit)
                            .unlimited(limit == -1 || isAdminOrSuperAdmin(userId))
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Get the limit for a specific usage type based on user's active plan.
     */
    private int getLimit(UUID userId, UsageType usageType) {
        Optional<UserSubscription> activeSub = subscriptionRepository
                .findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE);

        if (activeSub.isEmpty()) {
            return getFreePlanLimit(userId, usageType);
        }

        SubscriptionPlan plan = activeSub.get().getPlan();
        return getPlanLimit(plan, usageType);
    }

    private int getPlanLimit(SubscriptionPlan plan, UsageType usageType) {
        return switch (usageType) {
            case TEST_GENERATION -> plan.getMaxTestsPerDay() != null ? plan.getMaxTestsPerDay() : 5;
            case TEST_ATTEMPT -> plan.getMaxTestsPerDay() != null ? plan.getMaxTestsPerDay() : 5;
            case EXPORT_PDF -> plan.getExportPdfEnabled() ?
                    (plan.getMaxExportsPerMonth() != null ? plan.getMaxExportsPerMonth() : 10) : 0;
            case EXPORT_DOCX -> plan.getExportDocxEnabled() ?
                    (plan.getMaxExportsPerMonth() != null ? plan.getMaxExportsPerMonth() : 10) : 0;
            case GROUP_CREATE -> plan.getMaxGroups() != null ? plan.getMaxGroups() : 3;
            case QUESTION_IMPORT -> plan.getMaxQuestionsPerImport() != null ? plan.getMaxQuestionsPerImport() : 50;
        };
    }

    /**
     * Default FREE plan limits (no active subscription).
     */
    private int getFreePlanLimit(UUID userId, UsageType usageType) {
        boolean isTeacher = userRepository.findById(userId)
                .map(u -> u.getRole() == Role.TEACHER)
                .orElse(false);

        return switch (usageType) {
            case TEST_GENERATION -> isTeacher ? -1 : 5;        // Teacher: unlimited daily, Student: 5/day
            case TEST_ATTEMPT -> isTeacher ? -1 : 5;           // Teacher: unlimited, Student: 5/day
            case EXPORT_PDF -> 0;                               // Disabled on free
            case EXPORT_DOCX -> 0;                              // Disabled on free
            case GROUP_CREATE -> isTeacher ? 3 : 0;             // Teacher: 3 groups
            case QUESTION_IMPORT -> isTeacher ? 50 : 0;        // Teacher: 50 questions
        };
    }

    private void incrementUsage(UUID userId, UsageType usageType) {
        LocalDate today = LocalDate.now();
        Optional<UsageRecord> existing = usageRepository
                .findByUserIdAndUsageTypeAndUsageDate(userId, usageType, today);

        if (existing.isPresent()) {
            UsageRecord record = existing.get();
            record.setCount(record.getCount() + 1);
            usageRepository.save(record);
        } else {
            UsageRecord record = UsageRecord.builder()
                    .userId(userId)
                    .usageType(usageType)
                    .usageDate(today)
                    .count(1)
                    .build();
            usageRepository.save(record);
        }
    }

    private boolean isAdminOrSuperAdmin(UUID userId) {
        return userRepository.findById(userId)
                .map(u -> u.getRole() == Role.ADMIN || u.getRole() == Role.SUPER_ADMIN)
                .orElse(false);
    }
}
