package uz.eduplatform.modules.test.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "test_history", indexes = {
        @Index(name = "idx_test_history_user", columnList = "user_id"),
        @Index(name = "idx_test_history_subject", columnList = "subject_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class TestHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String title;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "title_translations", columnDefinition = "jsonb")
    private Map<String, String> titleTranslations;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private TestCategory category;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "topic_ids", nullable = false, columnDefinition = "jsonb")
    private List<UUID> topicIds;

    @Column(name = "question_count", nullable = false)
    private Integer questionCount;

    @Builder.Default
    @Column(name = "variant_count", nullable = false)
    private Integer variantCount = 1;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "difficulty_distribution", columnDefinition = "jsonb")
    private Map<String, Integer> difficultyDistribution;

    @Builder.Default
    @Column(name = "shuffle_questions")
    private Boolean shuffleQuestions = true;

    @Builder.Default
    @Column(name = "shuffle_options")
    private Boolean shuffleOptions = true;

    @Column(name = "random_seed")
    private Long randomSeed;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "header_config", columnDefinition = "jsonb")
    private Map<String, Object> headerConfig;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<Map<String, Object>> variants;

    @Column(name = "test_pdf_url", length = 500)
    private String testPdfUrl;

    @Column(name = "answer_key_pdf_url", length = 500)
    private String answerKeyPdfUrl;

    @Column(name = "combined_pdf_url", length = 500)
    private String combinedPdfUrl;

    @Column(name = "proofs_pdf_url", length = 500)
    private String proofsPdfUrl;

    @Builder.Default
    @Column(name = "download_count")
    private Integer downloadCount = 0;

    @Column(name = "last_downloaded_at")
    private LocalDateTime lastDownloadedAt;

    @Builder.Default
    @Column(name = "is_public")
    private Boolean isPublic = false;

    @Column(name = "public_slug", unique = true, length = 32)
    private String publicSlug;

    @Column(name = "public_duration_minutes")
    private Integer publicDurationMinutes;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TestStatus status = TestStatus.CREATED;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
