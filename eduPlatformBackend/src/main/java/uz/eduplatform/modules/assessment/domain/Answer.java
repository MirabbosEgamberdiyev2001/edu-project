package uz.eduplatform.modules.assessment.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnTransformer;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "answers", indexes = {
        @Index(name = "idx_answer_attempt", columnList = "attempt_id"),
        @Index(name = "idx_answer_question", columnList = "question_id")
}, uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_answer_attempt_question",
                columnNames = {"attempt_id", "question_id"}
        )
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private TestAttempt attempt;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "question_index", nullable = false)
    private Integer questionIndex;

    // --- Student's answer (JSONB - supports all question types) ---
    @Column(name = "selected_answer", columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String selectedAnswer;

    // --- Grading results ---
    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "is_partial")
    private Boolean isPartial;

    @Column(name = "earned_points", precision = 5, scale = 2)
    private BigDecimal earnedPoints;

    @Column(name = "max_points", precision = 5, scale = 2)
    private BigDecimal maxPoints;

    // --- Manual grading ---
    @Builder.Default
    @Column(name = "needs_manual_grading")
    private Boolean needsManualGrading = false;

    @Column(name = "manual_score", precision = 5, scale = 2)
    private BigDecimal manualScore;

    @Column(name = "manual_feedback", columnDefinition = "TEXT")
    private String manualFeedback;

    @Column(name = "graded_by")
    private UUID gradedBy;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

    // --- Metadata ---
    @Column(name = "time_spent_seconds")
    private Integer timeSpentSeconds;

    @Builder.Default
    @Column(nullable = false)
    private Boolean bookmarked = false;

    // --- Audit ---
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
