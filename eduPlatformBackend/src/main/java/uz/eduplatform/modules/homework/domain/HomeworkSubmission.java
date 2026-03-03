package uz.eduplatform.modules.homework.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "homework_submissions",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_hw_sub_latest",
        columnNames = {"homework_id", "student_id", "attempt_number"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class HomeworkSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "homework_id", nullable = false)
    private UUID homeworkId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "text_answer", columnDefinition = "TEXT")
    private String textAnswer;

    @Column(name = "file_url", length = 1000)
    private String fileUrl;

    @Column(name = "file_name", length = 500)
    private String fileName;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(precision = 5, scale = 2)
    private BigDecimal grade;

    @Column(name = "teacher_comment", columnDefinition = "TEXT")
    private String teacherComment;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

    @Column(name = "graded_by")
    private UUID gradedBy;

    @Builder.Default
    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber = 1;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "SUBMITTED";
}
