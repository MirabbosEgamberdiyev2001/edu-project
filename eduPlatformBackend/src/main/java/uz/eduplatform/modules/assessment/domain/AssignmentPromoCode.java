package uz.eduplatform.modules.assessment.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "assignment_promo_codes", indexes = {
        @Index(name = "idx_promo_code_assignment", columnList = "assignment_id"),
        @Index(name = "idx_promo_code_code", columnList = "code")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AssignmentPromoCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "assignment_id", nullable = false)
    private UUID assignmentId;

    @Column(nullable = false, unique = true, length = 8)
    private String code;

    @Column(name = "max_uses")
    private Integer maxUses;

    @Builder.Default
    @Column(name = "current_uses", nullable = false)
    private Integer currentUses = 0;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
