package uz.eduplatform.modules.subscription.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.eduplatform.core.common.exception.ResourceNotFoundException;
import uz.eduplatform.core.i18n.TranslatedField;
import uz.eduplatform.modules.subscription.domain.PlanType;
import uz.eduplatform.modules.subscription.domain.SubscriptionPlan;
import uz.eduplatform.modules.subscription.dto.CreatePlanRequest;
import uz.eduplatform.modules.subscription.dto.SubscriptionPlanDto;
import uz.eduplatform.modules.subscription.dto.UpdatePlanRequest;
import uz.eduplatform.modules.subscription.repository.SubscriptionPlanRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionPlanService {

    private final SubscriptionPlanRepository planRepository;

    @Cacheable(value = "activePlans")
    @Transactional(readOnly = true)
    public List<SubscriptionPlanDto> getActivePlans() {
        return planRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SubscriptionPlanDto> getAllPlans() {
        return planRepository.findAllByOrderBySortOrderAsc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "plans", key = "#planId")
    @Transactional(readOnly = true)
    public SubscriptionPlanDto getPlan(UUID planId) {
        return toDto(findPlan(planId));
    }

    @CacheEvict(value = {"activePlans", "plans"}, allEntries = true)
    @Transactional
    public SubscriptionPlanDto createPlan(CreatePlanRequest request) {
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .name(TranslatedField.wrap(request.getName()))
                .description(request.getDescription() != null ? TranslatedField.wrap(request.getDescription()) : null)
                .planType(request.getPlanType())
                .priceMonthly(request.getPriceMonthly())
                .priceYearly(request.getPriceYearly())
                .currency(request.getCurrency() != null ? request.getCurrency() : "UZS")
                .maxTestsPerDay(request.getMaxTestsPerDay() != null ? request.getMaxTestsPerDay() : 5)
                .maxTestsPerMonth(request.getMaxTestsPerMonth() != null ? request.getMaxTestsPerMonth() : 150)
                .maxGroups(request.getMaxGroups() != null ? request.getMaxGroups() : 3)
                .maxStudentsPerGroup(request.getMaxStudentsPerGroup() != null ? request.getMaxStudentsPerGroup() : 30)
                .maxExportsPerMonth(request.getMaxExportsPerMonth() != null ? request.getMaxExportsPerMonth() : 10)
                .maxQuestionsPerImport(request.getMaxQuestionsPerImport() != null ? request.getMaxQuestionsPerImport() : 50)
                .analyticsEnabled(request.getAnalyticsEnabled() != null && request.getAnalyticsEnabled())
                .exportPdfEnabled(request.getExportPdfEnabled() != null && request.getExportPdfEnabled())
                .exportDocxEnabled(request.getExportDocxEnabled() != null && request.getExportDocxEnabled())
                .proofVisible(request.getProofVisible() != null && request.getProofVisible())
                .apiAccess(request.getApiAccess() != null && request.getApiAccess())
                .customBranding(request.getCustomBranding() != null && request.getCustomBranding())
                .sortOrder(request.getSortOrder())
                .build();

        return toDto(planRepository.save(plan));
    }

    @CacheEvict(value = {"activePlans", "plans"}, allEntries = true)
    @Transactional
    public SubscriptionPlanDto updatePlan(UUID planId, UpdatePlanRequest request) {
        SubscriptionPlan plan = findPlan(planId);

        if (request.getName() != null) plan.setName(TranslatedField.wrap(request.getName()));
        if (request.getDescription() != null) plan.setDescription(TranslatedField.wrap(request.getDescription()));
        if (request.getPlanType() != null) plan.setPlanType(request.getPlanType());
        if (request.getPriceMonthly() != null) plan.setPriceMonthly(request.getPriceMonthly());
        if (request.getPriceYearly() != null) plan.setPriceYearly(request.getPriceYearly());
        if (request.getCurrency() != null) plan.setCurrency(request.getCurrency());
        if (request.getMaxTestsPerDay() != null) plan.setMaxTestsPerDay(request.getMaxTestsPerDay());
        if (request.getMaxTestsPerMonth() != null) plan.setMaxTestsPerMonth(request.getMaxTestsPerMonth());
        if (request.getMaxGroups() != null) plan.setMaxGroups(request.getMaxGroups());
        if (request.getMaxStudentsPerGroup() != null) plan.setMaxStudentsPerGroup(request.getMaxStudentsPerGroup());
        if (request.getMaxExportsPerMonth() != null) plan.setMaxExportsPerMonth(request.getMaxExportsPerMonth());
        if (request.getMaxQuestionsPerImport() != null) plan.setMaxQuestionsPerImport(request.getMaxQuestionsPerImport());
        if (request.getAnalyticsEnabled() != null) plan.setAnalyticsEnabled(request.getAnalyticsEnabled());
        if (request.getExportPdfEnabled() != null) plan.setExportPdfEnabled(request.getExportPdfEnabled());
        if (request.getExportDocxEnabled() != null) plan.setExportDocxEnabled(request.getExportDocxEnabled());
        if (request.getProofVisible() != null) plan.setProofVisible(request.getProofVisible());
        if (request.getApiAccess() != null) plan.setApiAccess(request.getApiAccess());
        if (request.getCustomBranding() != null) plan.setCustomBranding(request.getCustomBranding());
        if (request.getActive() != null) plan.setActive(request.getActive());
        if (request.getSortOrder() != null) plan.setSortOrder(request.getSortOrder());

        return toDto(planRepository.save(plan));
    }

    @CacheEvict(value = {"activePlans", "plans"}, allEntries = true)
    @Transactional
    public void deletePlan(UUID planId) {
        SubscriptionPlan plan = findPlan(planId);
        plan.setDeletedAt(LocalDateTime.now());
        planRepository.save(plan);
    }

    public SubscriptionPlan findPlan(UUID planId) {
        return planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionPlan", "id", planId));
    }

    private SubscriptionPlanDto toDto(SubscriptionPlan plan) {
        return SubscriptionPlanDto.builder()
                .id(plan.getId())
                .name(TranslatedField.resolve(plan.getName()))
                .description(TranslatedField.resolve(plan.getDescription()))
                .planType(plan.getPlanType())
                .priceMonthly(plan.getPriceMonthly())
                .priceYearly(plan.getPriceYearly())
                .currency(plan.getCurrency())
                .maxTestsPerDay(plan.getMaxTestsPerDay())
                .maxTestsPerMonth(plan.getMaxTestsPerMonth())
                .maxGroups(plan.getMaxGroups())
                .maxStudentsPerGroup(plan.getMaxStudentsPerGroup())
                .maxExportsPerMonth(plan.getMaxExportsPerMonth())
                .maxQuestionsPerImport(plan.getMaxQuestionsPerImport())
                .analyticsEnabled(plan.getAnalyticsEnabled())
                .exportPdfEnabled(plan.getExportPdfEnabled())
                .exportDocxEnabled(plan.getExportDocxEnabled())
                .proofVisible(plan.getProofVisible())
                .apiAccess(plan.getApiAccess())
                .customBranding(plan.getCustomBranding())
                .active(plan.getActive())
                .sortOrder(plan.getSortOrder())
                .build();
    }
}
