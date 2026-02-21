package uz.eduplatform.modules.subscription.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uz.eduplatform.core.common.exception.ResourceNotFoundException;
import uz.eduplatform.modules.subscription.domain.PlanType;
import uz.eduplatform.modules.subscription.domain.SubscriptionPlan;
import uz.eduplatform.modules.subscription.dto.CreatePlanRequest;
import uz.eduplatform.modules.subscription.dto.SubscriptionPlanDto;
import uz.eduplatform.modules.subscription.dto.UpdatePlanRequest;
import uz.eduplatform.modules.subscription.repository.SubscriptionPlanRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubscriptionPlanServiceTest {

    @Mock private SubscriptionPlanRepository planRepository;

    @InjectMocks private SubscriptionPlanService planService;

    private SubscriptionPlan freePlan;
    private SubscriptionPlan premiumPlan;

    @BeforeEach
    void setUp() {
        freePlan = SubscriptionPlan.builder()
                .id(UUID.randomUUID())
                .name(Map.of("uz_latn", "Bepul", "en", "Free"))
                .description(Map.of("uz_latn", "Bepul reja", "en", "Free plan"))
                .planType(PlanType.FREE)
                .priceMonthly(BigDecimal.ZERO)
                .currency("UZS")
                .maxTestsPerDay(5)
                .maxTestsPerMonth(150)
                .maxGroups(3)
                .maxStudentsPerGroup(30)
                .maxExportsPerMonth(0)
                .maxQuestionsPerImport(50)
                .analyticsEnabled(false)
                .exportPdfEnabled(false)
                .exportDocxEnabled(false)
                .proofVisible(false)
                .apiAccess(false)
                .customBranding(false)
                .active(true)
                .sortOrder(1)
                .build();

        premiumPlan = SubscriptionPlan.builder()
                .id(UUID.randomUUID())
                .name(Map.of("uz_latn", "Premium", "en", "Premium"))
                .description(Map.of("uz_latn", "Premium reja", "en", "Premium plan"))
                .planType(PlanType.PREMIUM)
                .priceMonthly(new BigDecimal("15000"))
                .priceYearly(new BigDecimal("150000"))
                .currency("UZS")
                .maxTestsPerDay(-1)
                .maxTestsPerMonth(-1)
                .maxGroups(0)
                .maxStudentsPerGroup(0)
                .maxExportsPerMonth(-1)
                .analyticsEnabled(true)
                .exportPdfEnabled(true)
                .exportDocxEnabled(false)
                .proofVisible(true)
                .apiAccess(false)
                .customBranding(false)
                .active(true)
                .sortOrder(2)
                .build();
    }

    @Test
    void getActivePlans_returnsOnlyActive() {
        when(planRepository.findByActiveTrueOrderBySortOrderAsc())
                .thenReturn(List.of(freePlan, premiumPlan));

        List<SubscriptionPlanDto> result = planService.getActivePlans();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPlanType()).isEqualTo(PlanType.FREE);
        assertThat(result.get(1).getPlanType()).isEqualTo(PlanType.PREMIUM);
    }

    @Test
    void getAllPlans_returnsAllIncludingInactive() {
        SubscriptionPlan inactivePlan = SubscriptionPlan.builder()
                .id(UUID.randomUUID())
                .name(Map.of("uz_latn", "Test"))
                .planType(PlanType.PRO)
                .priceMonthly(BigDecimal.ZERO)
                .currency("UZS")
                .active(false)
                .build();

        when(planRepository.findAllByOrderBySortOrderAsc())
                .thenReturn(List.of(freePlan, premiumPlan, inactivePlan));

        List<SubscriptionPlanDto> result = planService.getAllPlans();

        assertThat(result).hasSize(3);
    }

    @Test
    void getPlan_exists_returnsDto() {
        when(planRepository.findById(freePlan.getId())).thenReturn(Optional.of(freePlan));

        SubscriptionPlanDto result = planService.getPlan(freePlan.getId());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(freePlan.getId());
        assertThat(result.getPlanType()).isEqualTo(PlanType.FREE);
        assertThat(result.getPriceMonthly()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getPlan_notExists_throwsException() {
        UUID randomId = UUID.randomUUID();
        when(planRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planService.getPlan(randomId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createPlan_success() {
        CreatePlanRequest request = CreatePlanRequest.builder()
                .name("New Plan")
                .description("A new subscription plan")
                .planType(PlanType.PRO)
                .priceMonthly(new BigDecimal("25000"))
                .priceYearly(new BigDecimal("250000"))
                .maxTestsPerDay(-1)
                .maxTestsPerMonth(-1)
                .analyticsEnabled(true)
                .exportPdfEnabled(true)
                .exportDocxEnabled(true)
                .sortOrder(5)
                .build();

        when(planRepository.save(any(SubscriptionPlan.class))).thenAnswer(inv -> {
            SubscriptionPlan p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        SubscriptionPlanDto result = planService.createPlan(request);

        assertThat(result).isNotNull();
        assertThat(result.getPlanType()).isEqualTo(PlanType.PRO);
        assertThat(result.getPriceMonthly()).isEqualByComparingTo("25000");
        assertThat(result.getAnalyticsEnabled()).isTrue();
        assertThat(result.getExportPdfEnabled()).isTrue();
        verify(planRepository).save(any(SubscriptionPlan.class));
    }

    @Test
    void createPlan_withDefaults() {
        CreatePlanRequest request = CreatePlanRequest.builder()
                .name("Basic Plan")
                .planType(PlanType.FREE)
                .priceMonthly(BigDecimal.ZERO)
                .build();

        when(planRepository.save(any(SubscriptionPlan.class))).thenAnswer(inv -> {
            SubscriptionPlan p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        SubscriptionPlanDto result = planService.createPlan(request);

        assertThat(result.getCurrency()).isEqualTo("UZS");
        assertThat(result.getMaxTestsPerDay()).isEqualTo(5);
        assertThat(result.getMaxGroups()).isEqualTo(3);
        assertThat(result.getAnalyticsEnabled()).isFalse();
    }

    @Test
    void updatePlan_partialUpdate() {
        when(planRepository.findById(freePlan.getId())).thenReturn(Optional.of(freePlan));
        when(planRepository.save(any(SubscriptionPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdatePlanRequest request = UpdatePlanRequest.builder()
                .maxTestsPerDay(10)
                .analyticsEnabled(true)
                .build();

        SubscriptionPlanDto result = planService.updatePlan(freePlan.getId(), request);

        assertThat(result.getMaxTestsPerDay()).isEqualTo(10);
        assertThat(result.getAnalyticsEnabled()).isTrue();
        // Unchanged fields remain the same
        assertThat(result.getPlanType()).isEqualTo(PlanType.FREE);
    }

    @Test
    void updatePlan_fullUpdate() {
        when(planRepository.findById(premiumPlan.getId())).thenReturn(Optional.of(premiumPlan));
        when(planRepository.save(any(SubscriptionPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdatePlanRequest request = UpdatePlanRequest.builder()
                .name("Updated Premium")
                .priceMonthly(new BigDecimal("20000"))
                .active(false)
                .build();

        SubscriptionPlanDto result = planService.updatePlan(premiumPlan.getId(), request);

        assertThat(result.getPriceMonthly()).isEqualByComparingTo("20000");
        assertThat(result.getActive()).isFalse();
    }

    @Test
    void updatePlan_notExists_throwsException() {
        UUID randomId = UUID.randomUUID();
        when(planRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planService.updatePlan(randomId, new UpdatePlanRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deletePlan_setsDeletedAt() {
        when(planRepository.findById(freePlan.getId())).thenReturn(Optional.of(freePlan));
        when(planRepository.save(any(SubscriptionPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        planService.deletePlan(freePlan.getId());

        assertThat(freePlan.getDeletedAt()).isNotNull();
        verify(planRepository).save(freePlan);
    }

    @Test
    void deletePlan_notExists_throwsException() {
        UUID randomId = UUID.randomUUID();
        when(planRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planService.deletePlan(randomId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void toDto_mapsAllFields() {
        when(planRepository.findById(premiumPlan.getId())).thenReturn(Optional.of(premiumPlan));

        SubscriptionPlanDto dto = planService.getPlan(premiumPlan.getId());

        assertThat(dto.getId()).isEqualTo(premiumPlan.getId());
        assertThat(dto.getName()).isNotNull();
        assertThat(dto.getPlanType()).isEqualTo(PlanType.PREMIUM);
        assertThat(dto.getPriceMonthly()).isEqualByComparingTo("15000");
        assertThat(dto.getPriceYearly()).isEqualByComparingTo("150000");
        assertThat(dto.getMaxTestsPerDay()).isEqualTo(-1);
        assertThat(dto.getAnalyticsEnabled()).isTrue();
        assertThat(dto.getExportPdfEnabled()).isTrue();
        assertThat(dto.getProofVisible()).isTrue();
        assertThat(dto.getApiAccess()).isFalse();
        assertThat(dto.getSortOrder()).isEqualTo(2);
    }
}
