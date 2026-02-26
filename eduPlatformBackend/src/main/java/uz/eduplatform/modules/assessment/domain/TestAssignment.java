package uz.eduplatform.modules.assessment.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "test_assignments", indexes = {
        @Index(name = "idx_assignment_teacher", columnList = "teacher_id"),
        @Index(name = "idx_assignment_test_history", columnList = "test_history_id"),
        @Index(name = "idx_assignment_status", columnList = "status"),
        @Index(name = "idx_assignment_start_time", columnList = "start_time"),
        @Index(name = "idx_assignment_end_time", columnList = "end_time")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@SQLRestriction("deleted_at IS NULL")
public class TestAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "test_history_id", nullable = false)
    private UUID testHistoryId;

    @Column(name = "teacher_id", nullable = false)
    private UUID teacherId;

    @Column(name = "group_id")
    private UUID groupId;

    @Column(nullable = false)
    private String title;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "title_translations", columnDefinition = "jsonb")
    private Map<String, String> titleTranslations;

    @Column(columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "description_translations", columnDefinition = "jsonb")
    private Map<String, String> descriptionTranslations;

    // --- Scheduling ---
    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Builder.Default
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes = 45;

    // --- Attempt settings ---
    @Builder.Default
    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts = 1;

    // --- Result visibility ---
    @Builder.Default
    @Column(name = "show_results", nullable = false)
    private Boolean showResults = true;

    @Builder.Default
    @Column(name = "show_correct_answers", nullable = false)
    private Boolean showCorrectAnswers = false;

    @Builder.Default
    @Column(name = "show_proofs", nullable = false)
    private Boolean showProofs = false;

    // --- Shuffling ---
    @Builder.Default
    @Column(name = "shuffle_per_student", nullable = false)
    private Boolean shufflePerStudent = true;

    // --- Anti-cheat ---
    @Builder.Default
    @Column(name = "prevent_copy_paste", nullable = false)
    private Boolean preventCopyPaste = true;

    @Builder.Default
    @Column(name = "prevent_tab_switch", nullable = false)
    private Boolean preventTabSwitch = false;

    @Builder.Default
    @Column(name = "tab_switch_threshold")
    private Integer tabSwitchThreshold = 0;

    @Builder.Default
    @Column(name = "tab_switch_action", length = 20)
    private String tabSwitchAction = "WARN";

    // --- Access control ---
    @Column(name = "access_code", length = 20)
    private String accessCode;

    // --- Assigned students (JSONB list of UUIDs) ---
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "assigned_student_ids", columnDefinition = "jsonb")
    private List<UUID> assignedStudentIds;

    // --- Status ---
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssignmentStatus status = AssignmentStatus.DRAFT;

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
