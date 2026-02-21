package uz.eduplatform.modules.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentResultDto {

    private UUID assignmentId;
    private String assignmentTitle;
    private UUID teacherId;

    // --- Overall stats ---
    private Integer totalAssigned;
    private Integer totalStarted;
    private Integer totalSubmitted;
    private Integer totalGraded;

    // --- Score stats ---
    private BigDecimal averageScore;
    private BigDecimal highestScore;
    private BigDecimal lowestScore;
    private BigDecimal averagePercentage;

    // --- Individual results ---
    private List<AttemptDto> attempts;
}
