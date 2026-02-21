package uz.eduplatform.modules.subscription.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.modules.auth.domain.Role;
import uz.eduplatform.modules.auth.domain.User;
import uz.eduplatform.modules.auth.repository.UserRepository;
import uz.eduplatform.modules.subscription.domain.*;
import uz.eduplatform.modules.subscription.dto.UsageDto;
import uz.eduplatform.modules.subscription.repository.UsageRecordRepository;
import uz.eduplatform.modules.subscription.repository.UserSubscriptionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UsageTrackingServiceTest {

    @Mock private UsageRecordRepository usageRepository;
    @Mock private UserSubscriptionRepository subscriptionRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private UsageTrackingService usageTrackingService;

    private UUID studentId;
    private UUID teacherId;
    private UUID adminId;
    private User student;
    private User teacher;
    private User admin;
    private SubscriptionPlan premiumPlan;
    private UserSubscription premiumSub;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        teacherId = UUID.randomUUID();
        adminId = UUID.randomUUID();

        student = User.builder()
                .id(studentId).firstName("Student").lastName("Test")
                .role(Role.STUDENT).build();

        teacher = User.builder()
                .id(teacherId).firstName("Teacher").lastName("Test")
                .role(Role.TEACHER).build();

        admin = User.builder()
                .id(adminId).firstName("Admin").lastName("Test")
                .role(Role.ADMIN).build();

        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        premiumPlan = SubscriptionPlan.builder()
                .id(UUID.randomUUID())
                .name(Map.of("uz_latn", "Premium"))
                .planType(PlanType.PREMIUM)
                .priceMonthly(new BigDecimal("15000"))
                .maxTestsPerDay(-1)
                .maxTestsPerMonth(-1)
                .maxExportsPerMonth(-1)
                .exportPdfEnabled(true)
                .exportDocxEnabled(false)
                .analyticsEnabled(true)
                .build();

        premiumSub = UserSubscription.builder()
                .id(UUID.randomUUID())
                .userId(studentId)
                .plan(premiumPlan)
                .status(SubscriptionStatus.ACTIVE)
                .build();
    }

    // ── Admin Bypass ──

    @Test
    void trackAndCheckLimit_admin_bypassesAllLimits() {
        when(usageRepository.findByUserIdAndUsageTypeAndUsageDate(eq(adminId), any(), any()))
                .thenReturn(Optional.empty());
        when(usageRepository.save(any(UsageRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        // Should NOT throw even with no subscription
        usageTrackingService.trackAndCheckLimit(adminId, UsageType.TEST_GENERATION);
        usageTrackingService.trackAndCheckLimit(adminId, UsageType.EXPORT_PDF);

        verify(usageRepository, times(2)).save(any(UsageRecord.class));
    }

    @Test
    void canPerform_admin_alwaysTrue() {
        assertThat(usageTrackingService.canPerform(adminId, UsageType.TEST_GENERATION)).isTrue();
        assertThat(usageTrackingService.canPerform(adminId, UsageType.EXPORT_PDF)).isTrue();
        assertThat(usageTrackingService.canPerform(adminId, UsageType.EXPORT_DOCX)).isTrue();
    }

    // ── Free Plan Limits ──

    @Test
    void trackAndCheckLimit_freeStudent_withinLimit() {
        when(subscriptionRepository.findByUserIdAndStatus(studentId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(usageRepository.findByUserIdAndUsageTypeAndUsageDate(eq(studentId), eq(UsageType.TEST_ATTEMPT), any()))
                .thenReturn(Optional.empty());
        when(usageRepository.save(any(UsageRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        usageTrackingService.trackAndCheckLimit(studentId, UsageType.TEST_ATTEMPT);

        verify(usageRepository).save(any(UsageRecord.class));
    }

    @Test
    void trackAndCheckLimit_freeStudent_limitReached_throwsException() {
        when(subscriptionRepository.findByUserIdAndStatus(studentId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        UsageRecord record = UsageRecord.builder()
                .userId(studentId).usageType(UsageType.TEST_ATTEMPT)
                .usageDate(LocalDate.now()).count(5).build();
        when(usageRepository.findByUserIdAndUsageTypeAndUsageDate(eq(studentId), eq(UsageType.TEST_ATTEMPT), any()))
                .thenReturn(Optional.of(record));

        assertThatThrownBy(() -> usageTrackingService.trackAndCheckLimit(studentId, UsageType.TEST_ATTEMPT))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("usage.limit.reached");
    }

    @Test
    void trackAndCheckLimit_freeStudent_exportDisabled() {
        when(subscriptionRepository.findByUserIdAndStatus(studentId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(usageRepository.findByUserIdAndUsageTypeAndUsageDate(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(usageRepository.sumCountByUserIdAndTypeAndDateRange(any(), any(), any(), any()))
                .thenReturn(0);

        assertThatThrownBy(() -> usageTrackingService.trackAndCheckLimit(studentId, UsageType.EXPORT_PDF))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("usage.limit.reached");
    }

    // ── Premium Plan ──

    @Test
    void trackAndCheckLimit_premiumStudent_unlimitedTests() {
        when(subscriptionRepository.findByUserIdAndStatus(studentId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(premiumSub));
        when(usageRepository.findByUserIdAndUsageTypeAndUsageDate(eq(studentId), eq(UsageType.TEST_ATTEMPT), any()))
                .thenReturn(Optional.empty());
        when(usageRepository.save(any(UsageRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        usageTrackingService.trackAndCheckLimit(studentId, UsageType.TEST_ATTEMPT);

        verify(usageRepository).save(any(UsageRecord.class));
    }

    @Test
    void canPerform_premiumStudent_exportPdf_true() {
        when(subscriptionRepository.findByUserIdAndStatus(studentId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(premiumSub));
        when(usageRepository.sumCountByUserIdAndTypeAndDateRange(any(), any(), any(), any()))
                .thenReturn(0);

        assertThat(usageTrackingService.canPerform(studentId, UsageType.EXPORT_PDF)).isTrue();
    }

    @Test
    void canPerform_premiumStudent_exportDocx_disabled() {
        when(subscriptionRepository.findByUserIdAndStatus(studentId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(premiumSub));
        when(usageRepository.sumCountByUserIdAndTypeAndDateRange(any(), any(), any(), any()))
                .thenReturn(0);

        // DOCX is disabled in premium plan
        assertThat(usageTrackingService.canPerform(studentId, UsageType.EXPORT_DOCX)).isFalse();
    }

    // ── Teacher Free Plan ──

    @Test
    void trackAndCheckLimit_freeTeacher_unlimitedDailyTests() {
        when(subscriptionRepository.findByUserIdAndStatus(teacherId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(usageRepository.findByUserIdAndUsageTypeAndUsageDate(eq(teacherId), eq(UsageType.TEST_GENERATION), any()))
                .thenReturn(Optional.empty());
        when(usageRepository.save(any(UsageRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        usageTrackingService.trackAndCheckLimit(teacherId, UsageType.TEST_GENERATION);

        verify(usageRepository).save(any(UsageRecord.class));
    }

    @Test
    void trackAndCheckLimit_freeTeacher_groupLimit() {
        when(subscriptionRepository.findByUserIdAndStatus(teacherId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        UsageRecord record = UsageRecord.builder()
                .userId(teacherId).usageType(UsageType.GROUP_CREATE)
                .usageDate(LocalDate.now()).count(3).build();
        when(usageRepository.findByUserIdAndUsageTypeAndUsageDate(eq(teacherId), eq(UsageType.GROUP_CREATE), any()))
                .thenReturn(Optional.of(record));
        when(usageRepository.sumCountByUserIdAndTypeAndDateRange(eq(teacherId), eq(UsageType.GROUP_CREATE), any(), any()))
                .thenReturn(3);

        assertThatThrownBy(() -> usageTrackingService.trackAndCheckLimit(teacherId, UsageType.GROUP_CREATE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("usage.limit.reached");
    }

    // ── Usage Summary ──

    @Test
    void getUserUsageSummary_returnsAllUsageTypes() {
        when(subscriptionRepository.findByUserIdAndStatus(studentId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(usageRepository.findByUserIdAndUsageTypeAndUsageDate(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(usageRepository.sumCountByUserIdAndTypeAndDateRange(any(), any(), any(), any()))
                .thenReturn(0);

        List<UsageDto> result = usageTrackingService.getUserUsageSummary(studentId);

        assertThat(result).hasSize(UsageType.values().length);
        assertThat(result).allMatch(u -> u.getUserId().equals(studentId));
    }

    @Test
    void getUserUsageSummary_admin_allUnlimited() {
        List<UsageDto> result = usageTrackingService.getUserUsageSummary(adminId);

        assertThat(result).allMatch(UsageDto::getUnlimited);
    }

    // ── Increment Logic ──

    @Test
    void trackAndCheckLimit_incrementsExistingRecord() {
        when(subscriptionRepository.findByUserIdAndStatus(studentId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(premiumSub));

        UsageRecord existing = UsageRecord.builder()
                .userId(studentId).usageType(UsageType.TEST_ATTEMPT)
                .usageDate(LocalDate.now()).count(3).build();
        when(usageRepository.findByUserIdAndUsageTypeAndUsageDate(eq(studentId), eq(UsageType.TEST_ATTEMPT), any()))
                .thenReturn(Optional.of(existing));
        when(usageRepository.save(any(UsageRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        usageTrackingService.trackAndCheckLimit(studentId, UsageType.TEST_ATTEMPT);

        assertThat(existing.getCount()).isEqualTo(4);
        verify(usageRepository).save(existing);
    }

    @Test
    void trackAndCheckLimit_createsNewRecord() {
        when(subscriptionRepository.findByUserIdAndStatus(studentId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(premiumSub));
        when(usageRepository.findByUserIdAndUsageTypeAndUsageDate(eq(studentId), eq(UsageType.TEST_ATTEMPT), any()))
                .thenReturn(Optional.empty());
        when(usageRepository.save(any(UsageRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        usageTrackingService.trackAndCheckLimit(studentId, UsageType.TEST_ATTEMPT);

        verify(usageRepository).save(argThat(record ->
                record.getCount() == 1 &&
                record.getUserId().equals(studentId) &&
                record.getUsageType() == UsageType.TEST_ATTEMPT
        ));
    }
}
