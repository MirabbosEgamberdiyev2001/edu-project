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
import uz.eduplatform.modules.subscription.domain.*;
import uz.eduplatform.modules.subscription.dto.CreatePaymentRequest;
import uz.eduplatform.modules.subscription.dto.PaymentDto;
import uz.eduplatform.modules.subscription.repository.PaymentRepository;
import uz.eduplatform.modules.subscription.repository.SubscriptionPlanRepository;
import uz.eduplatform.modules.subscription.repository.UserSubscriptionRepository;

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
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private SubscriptionPlanRepository planRepository;
    @Mock private UserSubscriptionRepository subscriptionRepository;

    @InjectMocks private PaymentService paymentService;

    private UUID userId;
    private SubscriptionPlan premiumPlan;
    private SubscriptionPlan freePlan;
    private Payment pendingPayment;
    private Payment completedPayment;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        premiumPlan = SubscriptionPlan.builder()
                .id(UUID.randomUUID())
                .name(Map.of("uz_latn", "Premium"))
                .planType(PlanType.PREMIUM)
                .priceMonthly(new BigDecimal("15000"))
                .currency("UZS")
                .build();

        freePlan = SubscriptionPlan.builder()
                .id(UUID.randomUUID())
                .name(Map.of("uz_latn", "Free"))
                .planType(PlanType.FREE)
                .priceMonthly(BigDecimal.ZERO)
                .currency("UZS")
                .build();

        pendingPayment = Payment.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .amount(new BigDecimal("15000"))
                .currency("UZS")
                .provider(PaymentProvider.PAYME)
                .status(PaymentStatus.PENDING)
                .build();

        completedPayment = Payment.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .amount(new BigDecimal("15000"))
                .currency("UZS")
                .provider(PaymentProvider.CLICK)
                .status(PaymentStatus.COMPLETED)
                .build();
    }

    @Test
    void initiatePayment_success() {
        when(planRepository.findById(premiumPlan.getId())).thenReturn(Optional.of(premiumPlan));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .planId(premiumPlan.getId())
                .provider(PaymentProvider.PAYME)
                .durationMonths(1)
                .build();

        PaymentDto result = paymentService.initiatePayment(userId, request);

        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualByComparingTo("15000");
        assertThat(result.getProvider()).isEqualTo(PaymentProvider.PAYME);
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void initiatePayment_multiMonth() {
        when(planRepository.findById(premiumPlan.getId())).thenReturn(Optional.of(premiumPlan));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .planId(premiumPlan.getId())
                .provider(PaymentProvider.UZUM)
                .durationMonths(6)
                .build();

        PaymentDto result = paymentService.initiatePayment(userId, request);

        assertThat(result.getAmount()).isEqualByComparingTo("90000"); // 15000 * 6
    }

    @Test
    void initiatePayment_freePlan_throwsException() {
        when(planRepository.findById(freePlan.getId())).thenReturn(Optional.of(freePlan));

        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .planId(freePlan.getId())
                .provider(PaymentProvider.PAYME)
                .build();

        assertThatThrownBy(() -> paymentService.initiatePayment(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("payment.free.plan");
    }

    @Test
    void initiatePayment_planNotFound_throwsException() {
        UUID fakePlanId = UUID.randomUUID();
        when(planRepository.findById(fakePlanId)).thenReturn(Optional.empty());

        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .planId(fakePlanId)
                .provider(PaymentProvider.PAYME)
                .build();

        assertThatThrownBy(() -> paymentService.initiatePayment(userId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void confirmPayment_success() {
        when(paymentRepository.findById(pendingPayment.getId()))
                .thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentDto result = paymentService.confirmPayment(pendingPayment.getId(), "TXN-12345");

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(result.getExternalTransactionId()).isEqualTo("TXN-12345");
        assertThat(result.getPaidAt()).isNotNull();
    }

    @Test
    void confirmPayment_notPending_throwsException() {
        completedPayment.setStatus(PaymentStatus.COMPLETED);
        when(paymentRepository.findById(completedPayment.getId()))
                .thenReturn(Optional.of(completedPayment));

        assertThatThrownBy(() -> paymentService.confirmPayment(completedPayment.getId(), "TXN"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void failPayment_success() {
        when(paymentRepository.findById(pendingPayment.getId()))
                .thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentDto result = paymentService.failPayment(pendingPayment.getId(), "Insufficient funds");

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void failPayment_notPending_throwsException() {
        completedPayment.setStatus(PaymentStatus.COMPLETED);
        when(paymentRepository.findById(completedPayment.getId()))
                .thenReturn(Optional.of(completedPayment));

        assertThatThrownBy(() -> paymentService.failPayment(completedPayment.getId(), "reason"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void refundPayment_success() {
        completedPayment.setStatus(PaymentStatus.COMPLETED);
        when(paymentRepository.findById(completedPayment.getId()))
                .thenReturn(Optional.of(completedPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentDto result = paymentService.refundPayment(completedPayment.getId());

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void refundPayment_notCompleted_throwsException() {
        pendingPayment.setStatus(PaymentStatus.PENDING);
        when(paymentRepository.findById(pendingPayment.getId()))
                .thenReturn(Optional.of(pendingPayment));

        assertThatThrownBy(() -> paymentService.refundPayment(pendingPayment.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("payment.only.completed.refundable");
    }

    @Test
    void getUserPayments_returnsPaged() {
        Page<Payment> page = new PageImpl<>(List.of(pendingPayment, completedPayment));
        when(paymentRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any()))
                .thenReturn(page);

        PagedResponse<PaymentDto> result = paymentService.getUserPayments(
                userId, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void getAllPayments_noFilter() {
        Page<Payment> page = new PageImpl<>(List.of(pendingPayment));
        when(paymentRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(page);

        PagedResponse<PaymentDto> result = paymentService.getAllPayments(
                null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getAllPayments_withFilter() {
        Page<Payment> page = new PageImpl<>(List.of(pendingPayment));
        when(paymentRepository.findByStatusOrderByCreatedAtDesc(eq(PaymentStatus.PENDING), any()))
                .thenReturn(page);

        PagedResponse<PaymentDto> result = paymentService.getAllPayments(
                PaymentStatus.PENDING, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void getPayment_exists() {
        when(paymentRepository.findById(pendingPayment.getId()))
                .thenReturn(Optional.of(pendingPayment));

        PaymentDto result = paymentService.getPayment(pendingPayment.getId());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(pendingPayment.getId());
    }

    @Test
    void getPayment_notFound_throwsException() {
        UUID fakeId = UUID.randomUUID();
        when(paymentRepository.findById(fakeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPayment(fakeId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
