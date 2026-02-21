package uz.eduplatform.modules.subscription.service;

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
import org.springframework.data.domain.PageRequest;
import uz.eduplatform.core.common.dto.PagedResponse;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.core.common.exception.ResourceNotFoundException;
import uz.eduplatform.modules.auth.domain.User;
import uz.eduplatform.modules.auth.repository.UserRepository;
import uz.eduplatform.modules.subscription.domain.*;
import uz.eduplatform.modules.subscription.dto.AssignSubscriptionRequest;
import uz.eduplatform.modules.subscription.dto.UserSubscriptionDto;
import uz.eduplatform.modules.subscription.repository.SubscriptionPlanRepository;
import uz.eduplatform.modules.subscription.repository.UserSubscriptionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
class SubscriptionServiceTest {

    @Mock private UserSubscriptionRepository subscriptionRepository;
    @Mock private SubscriptionPlanRepository planRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private SubscriptionService subscriptionService;

    private UUID adminId;
    private UUID userId;
    private User user;
    private SubscriptionPlan premiumPlan;
    private UserSubscription activeSub;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        userId = UUID.randomUUID();

        user = User.builder()
                .id(userId).firstName("Test").lastName("User").build();

        premiumPlan = SubscriptionPlan.builder()
                .id(UUID.randomUUID())
                .name(Map.of("uz_latn", "Premium", "en", "Premium"))
                .planType(PlanType.PREMIUM)
                .priceMonthly(new BigDecimal("15000"))
                .currency("UZS")
                .build();

        activeSub = UserSubscription.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .plan(premiumPlan)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(LocalDateTime.now().minusDays(10))
                .endDate(LocalDateTime.now().plusDays(20))
                .autoRenew(true)
                .amountPaid(new BigDecimal("15000"))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    }

    @Test
    void getActiveSubscription_exists_returnsDto() {
        when(subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(activeSub));

        UserSubscriptionDto result = subscriptionService.getActiveSubscription(userId);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void getActiveSubscription_noSub_returnsNull() {
        when(subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        UserSubscriptionDto result = subscriptionService.getActiveSubscription(userId);

        assertThat(result).isNull();
    }

    @Test
    void getUserSubscriptions_returnsPaged() {
        Page<UserSubscription> page = new PageImpl<>(List.of(activeSub));
        when(subscriptionRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any()))
                .thenReturn(page);

        PagedResponse<UserSubscriptionDto> result = subscriptionService.getUserSubscriptions(
                userId, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getAllSubscriptions_noFilter_returnsAll() {
        Page<UserSubscription> page = new PageImpl<>(List.of(activeSub));
        when(subscriptionRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(page);

        PagedResponse<UserSubscriptionDto> result = subscriptionService.getAllSubscriptions(
                null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getAllSubscriptions_withFilter_filtersCorrectly() {
        Page<UserSubscription> page = new PageImpl<>(List.of(activeSub));
        when(subscriptionRepository.findByStatusOrderByCreatedAtDesc(eq(SubscriptionStatus.ACTIVE), any()))
                .thenReturn(page);

        PagedResponse<UserSubscriptionDto> result = subscriptionService.getAllSubscriptions(
                SubscriptionStatus.ACTIVE, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void assignSubscription_success() {
        when(planRepository.findById(premiumPlan.getId())).thenReturn(Optional.of(premiumPlan));
        when(subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(UserSubscription.class))).thenAnswer(inv -> {
            UserSubscription s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        AssignSubscriptionRequest request = AssignSubscriptionRequest.builder()
                .userId(userId)
                .planId(premiumPlan.getId())
                .durationMonths(3)
                .build();

        UserSubscriptionDto result = subscriptionService.assignSubscription(adminId, request);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(result.getAmountPaid()).isEqualByComparingTo("45000"); // 15000 * 3
    }

    @Test
    void assignSubscription_cancelsExisting() {
        when(planRepository.findById(premiumPlan.getId())).thenReturn(Optional.of(premiumPlan));
        when(subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(activeSub));
        when(subscriptionRepository.save(any(UserSubscription.class))).thenAnswer(inv -> {
            UserSubscription s = inv.getArgument(0);
            if (s.getId() == null) s.setId(UUID.randomUUID());
            return s;
        });

        AssignSubscriptionRequest request = AssignSubscriptionRequest.builder()
                .userId(userId)
                .planId(premiumPlan.getId())
                .durationMonths(1)
                .build();

        subscriptionService.assignSubscription(adminId, request);

        assertThat(activeSub.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(activeSub.getCancelledAt()).isNotNull();
        verify(subscriptionRepository, times(2)).save(any(UserSubscription.class));
    }

    @Test
    void assignSubscription_userNotFound_throwsException() {
        UUID fakeUserId = UUID.randomUUID();
        when(userRepository.findById(fakeUserId)).thenReturn(Optional.empty());

        AssignSubscriptionRequest request = AssignSubscriptionRequest.builder()
                .userId(fakeUserId)
                .planId(premiumPlan.getId())
                .build();

        assertThatThrownBy(() -> subscriptionService.assignSubscription(adminId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void assignSubscription_planNotFound_throwsException() {
        UUID fakePlanId = UUID.randomUUID();
        when(planRepository.findById(fakePlanId)).thenReturn(Optional.empty());

        AssignSubscriptionRequest request = AssignSubscriptionRequest.builder()
                .userId(userId)
                .planId(fakePlanId)
                .build();

        assertThatThrownBy(() -> subscriptionService.assignSubscription(adminId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cancelSubscription_success() {
        when(subscriptionRepository.findById(activeSub.getId()))
                .thenReturn(Optional.of(activeSub));
        when(subscriptionRepository.save(any(UserSubscription.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UserSubscriptionDto result = subscriptionService.cancelSubscription(activeSub.getId());

        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
    }

    @Test
    void cancelSubscription_notActive_throwsException() {
        activeSub.setStatus(SubscriptionStatus.EXPIRED);
        when(subscriptionRepository.findById(activeSub.getId()))
                .thenReturn(Optional.of(activeSub));

        assertThatThrownBy(() -> subscriptionService.cancelSubscription(activeSub.getId()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void suspendSubscription_success() {
        when(subscriptionRepository.findById(activeSub.getId()))
                .thenReturn(Optional.of(activeSub));
        when(subscriptionRepository.save(any(UserSubscription.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UserSubscriptionDto result = subscriptionService.suspendSubscription(activeSub.getId());

        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.SUSPENDED);
    }

    @Test
    void suspendSubscription_notActive_throwsException() {
        activeSub.setStatus(SubscriptionStatus.CANCELLED);
        when(subscriptionRepository.findById(activeSub.getId()))
                .thenReturn(Optional.of(activeSub));

        assertThatThrownBy(() -> subscriptionService.suspendSubscription(activeSub.getId()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void reactivateSubscription_success() {
        activeSub.setStatus(SubscriptionStatus.SUSPENDED);
        when(subscriptionRepository.findById(activeSub.getId()))
                .thenReturn(Optional.of(activeSub));
        when(subscriptionRepository.save(any(UserSubscription.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UserSubscriptionDto result = subscriptionService.reactivateSubscription(activeSub.getId());

        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void reactivateSubscription_notSuspended_throwsException() {
        activeSub.setStatus(SubscriptionStatus.ACTIVE);
        when(subscriptionRepository.findById(activeSub.getId()))
                .thenReturn(Optional.of(activeSub));

        assertThatThrownBy(() -> subscriptionService.reactivateSubscription(activeSub.getId()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getActivePlan_exists_returnsPlan() {
        when(subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(activeSub));

        SubscriptionPlan result = subscriptionService.getActivePlan(userId);

        assertThat(result).isNotNull();
        assertThat(result.getPlanType()).isEqualTo(PlanType.PREMIUM);
    }

    @Test
    void getActivePlan_noSub_returnsNull() {
        when(subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        SubscriptionPlan result = subscriptionService.getActivePlan(userId);

        assertThat(result).isNull();
    }
}
