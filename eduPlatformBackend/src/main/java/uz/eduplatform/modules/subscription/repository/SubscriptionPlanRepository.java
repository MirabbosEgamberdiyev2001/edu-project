package uz.eduplatform.modules.subscription.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.eduplatform.modules.subscription.domain.PlanType;
import uz.eduplatform.modules.subscription.domain.SubscriptionPlan;

import java.util.List;
import java.util.UUID;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, UUID> {

    List<SubscriptionPlan> findByActiveTrueOrderBySortOrderAsc();

    List<SubscriptionPlan> findByPlanTypeAndActiveTrue(PlanType planType);

    List<SubscriptionPlan> findAllByOrderBySortOrderAsc();
}
