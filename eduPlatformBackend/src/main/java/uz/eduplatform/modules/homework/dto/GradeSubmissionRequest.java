package uz.eduplatform.modules.homework.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class GradeSubmissionRequest {

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    private BigDecimal grade;

    private String teacherComment;
}
