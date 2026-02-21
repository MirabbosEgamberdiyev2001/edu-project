package uz.eduplatform.modules.subscription.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "subscription_plans", indexes = {
        @Index(name = "idx_plan_type", columnList = "plan_type"),
        @Index(name = "idx_plan_active", columnList = "active")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@SQLRestriction("deleted_at IS NULL")
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "name", columnDefinition = "jsonb", nullable = false)
    private Map<String, String> name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "description", columnDefinition = "jsonb")
    private Map<String, String> description;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false, length = 20)
    private PlanType planType;

    @Column(name = "price_monthly", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceMonthly;

    @Column(name = "price_yearly", precision = 12, scale = 2)
    private BigDecimal priceYearly;

    @Column(nullable = false, length = 10)
    private String currency;

    // --- Usage limits ---
    @Builder.Default
    @Column(name = "max_tests_per_day")
    private Integer maxTestsPerDay = 5;

    @Builder.Default
    @Column(name = "max_tests_per_month")
    private Integer maxTestsPerMonth = 150;

    @Builder.Default
    @Column(name = "max_groups")
    private Integer maxGroups = 3;

    @Builder.Default
    @Column(name = "max_students_per_group")
    private Integer maxStudentsPerGroup = 30;

    @Builder.Default
    @Column(name = "max_exports_per_month")
    private Integer maxExportsPerMonth = 10;

    @Builder.Default
    @Column(name = "max_questions_per_import")
    private Integer maxQuestionsPerImport = 50;

    // --- Feature flags ---
    @Builder.Default
    @Column(name = "analytics_enabled", nullable = false)
    private Boolean analyticsEnabled = false;

    @Builder.Default
    @Column(name = "export_pdf_enabled", nullable = false)
    private Boolean exportPdfEnabled = false;

    @Builder.Default
    @Column(name = "export_docx_enabled", nullable = false)
    private Boolean exportDocxEnabled = false;

    @Builder.Default
    @Column(name = "proof_visible", nullable = false)
    private Boolean proofVisible = false;

    @Builder.Default
    @Column(name = "api_access", nullable = false)
    private Boolean apiAccess = false;

    @Builder.Default
    @Column(name = "custom_branding", nullable = false)
    private Boolean customBranding = false;

    // --- Plan state ---
    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "sort_order")
    private Integer sortOrder;

    // --- Audit ---
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
