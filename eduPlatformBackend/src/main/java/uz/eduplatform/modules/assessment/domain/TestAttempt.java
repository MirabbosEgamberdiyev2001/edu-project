package uz.eduplatform.modules.assessment.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "test_attempts", indexes = {
        @Index(name = "idx_attempt_assignment", columnList = "assignment_id"),
        @Index(name = "idx_attempt_student", columnList = "student_id"),
        @Index(name = "idx_attempt_status", columnList = "status")
}, uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_attempt_assignment_student_number",
                columnNames = {"assignment_id", "student_id", "attempt_number"}
        )
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class TestAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private TestAssignment assignment;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Builder.Default
    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber = 1;

    @Column(name = "variant_index")
    private Integer variantIndex;

    // --- Timing ---
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    // --- Scoring ---
    @Column(name = "raw_score", precision = 7, scale = 2)
    private BigDecimal rawScore;

    @Column(name = "max_score", precision = 7, scale = 2)
    private BigDecimal maxScore;

    @Column(precision = 5, scale = 2)
    private BigDecimal percentage;

    // --- Status ---
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    // --- Anti-cheat tracking ---
    @Builder.Default
    @Column(name = "tab_switch_count")
    private Integer tabSwitchCount = 0;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Builder.Default
    @Column(name = "flagged")
    private Boolean flagged = false;

    @Column(name = "flag_reason")
    private String flagReason;

    // --- Answers ---
    @Builder.Default
    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Answer> answers = new ArrayList<>();

    // --- Audit ---
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
