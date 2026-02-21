package uz.eduplatform.modules.subscription.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uz.eduplatform.modules.subscription.config.PaymentProperties;
import uz.eduplatform.modules.subscription.domain.*;
import uz.eduplatform.modules.subscription.dto.PaymentInitiationResponse;
import uz.eduplatform.modules.subscription.dto.payme.PaymeError;
import uz.eduplatform.modules.subscription.dto.payme.PaymeRequest;
import uz.eduplatform.modules.subscription.dto.payme.PaymeResponse;
import uz.eduplatform.modules.subscription.repository.PaymentRepository;
import uz.eduplatform.modules.subscription.service.PaymentOrchestrationService;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymeProviderStrategy implements PaymentProviderStrategy {

    private final PaymentProperties paymentProperties;
    private final PaymentRepository paymentRepository;

    private PaymentOrchestrationService orchestrationService;

    public void setOrchestrationService(PaymentOrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.PAYME;
    }

    @Override
    public boolean isEnabled() {
        return paymentProperties.getPayme().isEnabled();
    }

    @Override
    public PaymentInitiationResponse initiateCheckout(Payment payment, SubscriptionPlan plan) {
        // Payme uses tiyin (1 UZS = 100 tiyin)
        long amountInTiyin = payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue();

        String params = "m=" + paymentProperties.getPayme().getMerchantId()
                + ";ac.order_id=" + payment.getProviderOrderId()
                + ";a=" + amountInTiyin;

        String base64Params = Base64.getEncoder().encodeToString(
                params.getBytes(StandardCharsets.UTF_8));

        String redirectUrl = paymentProperties.getPayme().getCheckoutUrl() + "/" + base64Params;

        return PaymentInitiationResponse.builder()
                .paymentId(payment.getId())
                .providerOrderId(payment.getProviderOrderId())
                .provider(PaymentProvider.PAYME)
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .redirectUrl(redirectUrl)
                .providerParams(Map.of(
                        "merchantId", paymentProperties.getPayme().getMerchantId(),
                        "amountInTiyin", amountInTiyin
                ))
                .build();
    }

    /**
     * Main JSON-RPC handler for Payme callbacks.
     */
    public PaymeResponse handleJsonRpc(PaymeRequest request) {
        String method = request.getMethod();
        Long rpcId = request.getId();

        return switch (method) {
            case "CheckPerformTransaction" -> checkPerformTransaction(request, rpcId);
            case "CreateTransaction" -> createTransaction(request, rpcId);
            case "PerformTransaction" -> performTransaction(request, rpcId);
            case "CancelTransaction" -> cancelTransaction(request, rpcId);
            case "CheckTransaction" -> checkTransaction(request, rpcId);
            case "GetStatement" -> getStatement(request, rpcId);
            default -> PaymeResponse.error(rpcId, PaymeError.methodNotFound());
        };
    }

    private PaymeResponse checkPerformTransaction(PaymeRequest request, Long rpcId) {
        String orderId = request.getAccountOrderId();
        Long amount = request.getAmount();

        Optional<Payment> paymentOpt = paymentRepository.findByProviderAndProviderOrderId(
                PaymentProvider.PAYME, orderId);

        if (paymentOpt.isEmpty()) {
            return PaymeResponse.error(rpcId, PaymeError.orderNotFound());
        }

        Payment payment = paymentOpt.get();

        if (payment.getStatus() != PaymentStatus.PENDING) {
            return PaymeResponse.error(rpcId, PaymeError.cantPerform());
        }

        // Payme sends amount in tiyin, DB stores in UZS
        long expectedTiyin = payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue();
        if (amount == null || amount != expectedTiyin) {
            return PaymeResponse.error(rpcId, PaymeError.invalidAmount());
        }

        return PaymeResponse.success(rpcId, Map.of("allow", true));
    }

    private PaymeResponse createTransaction(PaymeRequest request, Long rpcId) {
        String paymeTransId = request.getTransactionId();
        String orderId = request.getAccountOrderId();
        Long amount = request.getAmount();
        Object timeObj = request.getParams().get("time");
        Long time = timeObj instanceof Number ? ((Number) timeObj).longValue() : System.currentTimeMillis();

        // Check if transaction already exists (idempotency)
        Optional<Payment> existingByExtId = paymentRepository.findByProviderAndExternalTransactionId(
                PaymentProvider.PAYME, paymeTransId);

        if (existingByExtId.isPresent()) {
            Payment existing = existingByExtId.get();
            if (existing.getPaymeTransactionState() != null && existing.getPaymeTransactionState() == 1) {
                // Check timeout
                if (isTransactionTimedOut(existing)) {
                    existing.setPaymeTransactionState(-1);
                    existing.setPaymeReason(4); // timeout
                    existing.setPaymeCancelTime(System.currentTimeMillis());
                    existing.setStatus(PaymentStatus.CANCELLED);
                    paymentRepository.save(existing);
                    return PaymeResponse.error(rpcId, PaymeError.cantPerform());
                }
                return PaymeResponse.success(rpcId, buildTransactionResult(existing));
            }
            return PaymeResponse.error(rpcId, PaymeError.cantPerform());
        }

        Optional<Payment> paymentOpt = paymentRepository.findByProviderAndProviderOrderId(
                PaymentProvider.PAYME, orderId);

        if (paymentOpt.isEmpty()) {
            return PaymeResponse.error(rpcId, PaymeError.orderNotFound());
        }

        Payment payment = paymentOpt.get();

        if (payment.getStatus() != PaymentStatus.PENDING) {
            return PaymeResponse.error(rpcId, PaymeError.cantPerform());
        }

        long expectedTiyin = payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue();
        if (amount == null || amount != expectedTiyin) {
            return PaymeResponse.error(rpcId, PaymeError.invalidAmount());
        }

        // Create the Payme transaction
        payment.setExternalTransactionId(paymeTransId);
        payment.setPaymeTransactionState(1);
        payment.setPaymeCreateTime(time);
        paymentRepository.save(payment);

        return PaymeResponse.success(rpcId, buildTransactionResult(payment));
    }

    private PaymeResponse performTransaction(PaymeRequest request, Long rpcId) {
        String paymeTransId = request.getTransactionId();

        Optional<Payment> paymentOpt = paymentRepository.findByProviderAndExternalTransactionId(
                PaymentProvider.PAYME, paymeTransId);

        if (paymentOpt.isEmpty()) {
            return PaymeResponse.error(rpcId, PaymeError.transactionNotFound());
        }

        Payment payment = paymentOpt.get();

        if (payment.getPaymeTransactionState() != null && payment.getPaymeTransactionState() == 2) {
            // Already performed — idempotent
            return PaymeResponse.success(rpcId, buildPerformResult(payment));
        }

        if (payment.getPaymeTransactionState() == null || payment.getPaymeTransactionState() != 1) {
            return PaymeResponse.error(rpcId, PaymeError.cantPerform());
        }

        // Check timeout
        if (isTransactionTimedOut(payment)) {
            payment.setPaymeTransactionState(-1);
            payment.setPaymeReason(4);
            payment.setPaymeCancelTime(System.currentTimeMillis());
            payment.setStatus(PaymentStatus.CANCELLED);
            paymentRepository.save(payment);
            return PaymeResponse.error(rpcId, PaymeError.cantPerform());
        }

        // Perform the transaction
        long performTime = System.currentTimeMillis();
        payment.setPaymeTransactionState(2);
        payment.setPaymePerformTime(performTime);
        payment.setPaidAt(LocalDateTime.now());
        payment.setStatus(PaymentStatus.COMPLETED);
        paymentRepository.save(payment);

        // Activate subscription
        if (orchestrationService != null) {
            orchestrationService.onPaymentConfirmed(payment.getId(), paymeTransId);
        }

        return PaymeResponse.success(rpcId, buildPerformResult(payment));
    }

    private PaymeResponse cancelTransaction(PaymeRequest request, Long rpcId) {
        String paymeTransId = request.getTransactionId();
        Integer reason = request.getReason();

        Optional<Payment> paymentOpt = paymentRepository.findByProviderAndExternalTransactionId(
                PaymentProvider.PAYME, paymeTransId);

        if (paymentOpt.isEmpty()) {
            return PaymeResponse.error(rpcId, PaymeError.transactionNotFound());
        }

        Payment payment = paymentOpt.get();
        Integer state = payment.getPaymeTransactionState();

        if (state != null && (state == -1 || state == -2)) {
            // Already cancelled — idempotent
            return PaymeResponse.success(rpcId, buildCancelResult(payment));
        }

        if (state != null && state == 2) {
            // Completed transaction — cancel with state -2
            payment.setPaymeTransactionState(-2);
            payment.setPaymeReason(reason);
            payment.setPaymeCancelTime(System.currentTimeMillis());
            payment.setStatus(PaymentStatus.CANCELLED);
            paymentRepository.save(payment);

            if (orchestrationService != null) {
                orchestrationService.onPaymentCancelled(payment.getId(),
                        "Payme cancel after perform, reason: " + reason);
            }

            return PaymeResponse.success(rpcId, buildCancelResult(payment));
        }

        if (state != null && state == 1) {
            // Created but not performed — cancel with state -1
            payment.setPaymeTransactionState(-1);
            payment.setPaymeReason(reason);
            payment.setPaymeCancelTime(System.currentTimeMillis());
            payment.setStatus(PaymentStatus.CANCELLED);
            paymentRepository.save(payment);

            return PaymeResponse.success(rpcId, buildCancelResult(payment));
        }

        return PaymeResponse.error(rpcId, PaymeError.cantPerform());
    }

    private PaymeResponse checkTransaction(PaymeRequest request, Long rpcId) {
        String paymeTransId = request.getTransactionId();

        Optional<Payment> paymentOpt = paymentRepository.findByProviderAndExternalTransactionId(
                PaymentProvider.PAYME, paymeTransId);

        if (paymentOpt.isEmpty()) {
            return PaymeResponse.error(rpcId, PaymeError.transactionNotFound());
        }

        Payment payment = paymentOpt.get();
        return PaymeResponse.success(rpcId, buildCheckResult(payment));
    }

    private PaymeResponse getStatement(PaymeRequest request, Long rpcId) {
        Long from = request.getFrom();
        Long to = request.getTo();

        if (from == null || to == null) {
            return PaymeResponse.error(rpcId, PaymeError.of(
                    PaymeError.INVALID_AMOUNT, "Неверные параметры", "Invalid parameters"));
        }

        LocalDateTime fromTime = LocalDateTime.ofEpochSecond(from / 1000, 0, ZoneOffset.UTC);
        LocalDateTime toTime = LocalDateTime.ofEpochSecond(to / 1000, 0, ZoneOffset.UTC);

        List<Payment> payments = paymentRepository.findByProviderAndStatusAndCreatedAtBetween(
                PaymentProvider.PAYME, PaymentStatus.COMPLETED, fromTime, toTime);

        // Also include cancelled payments in the statement
        List<Payment> cancelledPayments = paymentRepository.findByProviderAndStatusAndCreatedAtBetween(
                PaymentProvider.PAYME, PaymentStatus.CANCELLED, fromTime, toTime);

        List<Map<String, Object>> transactions = new ArrayList<>();

        for (Payment p : payments) {
            if (p.getExternalTransactionId() != null) {
                transactions.add(buildStatementEntry(p));
            }
        }
        for (Payment p : cancelledPayments) {
            if (p.getExternalTransactionId() != null) {
                transactions.add(buildStatementEntry(p));
            }
        }

        return PaymeResponse.success(rpcId, Map.of("transactions", transactions));
    }

    private boolean isTransactionTimedOut(Payment payment) {
        if (payment.getPaymeCreateTime() == null) return false;
        long timeout = paymentProperties.getPayme().getTransactionTimeout();
        return System.currentTimeMillis() - payment.getPaymeCreateTime() > timeout;
    }

    private Map<String, Object> buildTransactionResult(Payment payment) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("create_time", payment.getPaymeCreateTime());
        result.put("transaction", payment.getId().toString());
        result.put("state", payment.getPaymeTransactionState());
        return result;
    }

    private Map<String, Object> buildPerformResult(Payment payment) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("transaction", payment.getId().toString());
        result.put("perform_time", payment.getPaymePerformTime());
        result.put("state", payment.getPaymeTransactionState());
        return result;
    }

    private Map<String, Object> buildCancelResult(Payment payment) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("transaction", payment.getId().toString());
        result.put("cancel_time", payment.getPaymeCancelTime());
        result.put("state", payment.getPaymeTransactionState());
        return result;
    }

    private Map<String, Object> buildCheckResult(Payment payment) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("create_time", payment.getPaymeCreateTime());
        result.put("perform_time", payment.getPaymePerformTime() != null ? payment.getPaymePerformTime() : 0);
        result.put("cancel_time", payment.getPaymeCancelTime() != null ? payment.getPaymeCancelTime() : 0);
        result.put("transaction", payment.getId().toString());
        result.put("state", payment.getPaymeTransactionState());
        result.put("reason", payment.getPaymeReason());
        return result;
    }

    private Map<String, Object> buildStatementEntry(Payment payment) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("id", payment.getExternalTransactionId());
        entry.put("time", payment.getPaymeCreateTime());
        entry.put("amount", payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue());
        entry.put("account", Map.of("order_id", payment.getProviderOrderId()));
        entry.put("create_time", payment.getPaymeCreateTime());
        entry.put("perform_time", payment.getPaymePerformTime() != null ? payment.getPaymePerformTime() : 0);
        entry.put("cancel_time", payment.getPaymeCancelTime() != null ? payment.getPaymeCancelTime() : 0);
        entry.put("transaction", payment.getId().toString());
        entry.put("state", payment.getPaymeTransactionState());
        entry.put("reason", payment.getPaymeReason());
        return entry;
    }
}
