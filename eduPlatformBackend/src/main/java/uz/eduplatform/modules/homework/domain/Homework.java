package uz.eduplatform.modules.homework.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "homeworks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
public class Homework {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "teacher_id", nullable = false)
    private UUID teacherId;

    @Column(name = "group_id")
    private UUID groupId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDateTime deadline;

    @Builder.Default
    @Column(name = "allow_resubmission", nullable = false)
    private Boolean allowResubmission = true;

    @Builder.Default
    @Column(name = "file_required", nullable = false)
    private Boolean fileRequired = false;

    @Builder.Default
    @Column(name = "max_file_size_mb", nullable = false)
    private Integer maxFileSizeMb = 10;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
