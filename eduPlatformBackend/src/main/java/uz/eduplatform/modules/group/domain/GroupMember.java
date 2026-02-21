package uz.eduplatform.modules.group.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "group_members", indexes = {
        @Index(name = "idx_gm_group", columnList = "group_id"),
        @Index(name = "idx_gm_student", columnList = "student_id")
}, uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_group_student",
                columnNames = {"group_id", "student_id"}
        )
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private StudentGroup group;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @CreatedDate
    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;
}
