package uz.eduplatform.modules.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentResultDto {

    private UUID studentId;
    private String firstName;
    private String lastName;
    private BigDecimal score;
    private BigDecimal maxScore;
    private BigDecimal percentage;
    private LocalDateTime submittedAt;
    private Integer attemptCount;
    private Integer tabSwitches;
    private String status;
}
