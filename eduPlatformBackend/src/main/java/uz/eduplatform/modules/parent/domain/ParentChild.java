package uz.eduplatform.modules.parent.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "parent_children", indexes = {
        @Index(name = "idx_pc_parent", columnList = "parent_id"),
        @Index(name = "idx_pc_child", columnList = "child_id")
}, uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_parent_child",
                columnNames = {"parent_id", "child_id"}
        )
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ParentChild {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "parent_id", nullable = false)
    private UUID parentId;

    @Column(name = "child_id", nullable = false)
    private UUID childId;

    @Column(name = "pairing_code", unique = true, length = 8)
    private String pairingCode;

    @Column(name = "pairing_code_expires_at")
    private LocalDateTime pairingCodeExpiresAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PairingStatus status = PairingStatus.PENDING;

    @CreatedDate
    @Column(name = "paired_at", updatable = false)
    private LocalDateTime pairedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
}
