package uz.eduplatform.modules.subscription.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "usage_records", indexes = {
        @Index(name = "idx_usage_user", columnList = "user_id"),
        @Index(name = "idx_usage_type", columnList = "usage_type"),
        @Index(name = "idx_usage_date", columnList = "usage_date")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_type_date", columnNames = {"user_id", "usage_type", "usage_date"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class UsageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "usage_type", nullable = false, length = 30)
    private UsageType usageType;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Builder.Default
    @Column(name = "count", nullable = false)
    private Integer count = 0;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
