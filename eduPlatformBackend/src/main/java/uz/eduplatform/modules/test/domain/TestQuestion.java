package uz.eduplatform.modules.test.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "test_questions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"test_id", "variant_code", "question_order"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "test_id", nullable = false)
    private UUID testId;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "question_version", nullable = false)
    private Integer questionVersion;

    @Column(name = "variant_code", nullable = false, length = 1)
    private String variantCode;

    @Column(name = "question_order", nullable = false)
    private Integer questionOrder;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options_order", columnDefinition = "jsonb")
    private List<String> optionsOrder;
}
