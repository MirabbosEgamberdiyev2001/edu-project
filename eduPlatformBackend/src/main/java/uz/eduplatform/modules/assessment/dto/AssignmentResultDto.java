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
    private UUID groupId;
    private String groupName;

    // --- Overall stats ---
    private Integer totalStudents;
    private Integer completedStudents;

    // --- Score stats ---
    private BigDecimal averageScore;
    private BigDecimal highestScore;
    private BigDecimal lowestScore;

    // --- Individual results ---
    private List<StudentResultDto> students;
}
