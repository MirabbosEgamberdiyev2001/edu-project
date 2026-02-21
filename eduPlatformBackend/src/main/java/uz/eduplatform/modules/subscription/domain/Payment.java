package uz.eduplatform.modules.subscription.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_user", columnList = "user_id"),
        @Index(name = "idx_payment_subscription", columnList = "subscription_id"),
        @Index(name = "idx_payment_status", columnList = "status"),
        @Index(name = "idx_payment_provider", columnList = "provider"),
        @Index(name = "idx_payment_external_id", columnList = "external_transaction_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_provider_order", columnNames = {"provider", "provider_order_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private UserSubscription subscription;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "external_transaction_id")
    private String externalTransactionId;

    @Column(name = "provider_order_id")
    private String providerOrderId;

    @Column(name = "plan_id")
    private UUID planId;

    @Column(name = "duration_months")
    private Integer durationMonths;

    @Column(name = "callback_data", columnDefinition = "TEXT")
    private String callbackData;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // Payme-specific fields
    @Column(name = "payme_transaction_state")
    private Integer paymeTransactionState;

    @Column(name = "payme_create_time")
    private Long paymeCreateTime;

    @Column(name = "payme_perform_time")
    private Long paymePerformTime;

    @Column(name = "payme_cancel_time")
    private Long paymeCancelTime;

    @Column(name = "payme_reason")
    private Integer paymeReason;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "failure_reason")
    private String failureReason;

    // --- Audit ---
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
