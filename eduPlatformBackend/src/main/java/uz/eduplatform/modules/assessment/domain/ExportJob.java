package uz.eduplatform.modules.assessment.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import uz.eduplatform.modules.assessment.service.export.ResultExportFormat;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "export_jobs", indexes = {
        @Index(name = "idx_export_job_teacher", columnList = "teacher_id"),
        @Index(name = "idx_export_job_status", columnList = "status"),
        @Index(name = "idx_export_job_expires_at", columnList = "expires_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ExportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "assignment_id", nullable = false)
    private UUID assignmentId;

    @Column(name = "teacher_id", nullable = false)
    private UUID teacherId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExportJobStatus status = ExportJobStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ResultExportFormat format;

    @Column(length = 10)
    private String locale;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "content_type")
    private String contentType;

    @Lob
    @Column(name = "file_data")
    private byte[] fileData;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
