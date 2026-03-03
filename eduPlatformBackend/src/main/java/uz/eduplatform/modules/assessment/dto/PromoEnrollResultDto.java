package uz.eduplatform.modules.assessment.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PromoEnrollResultDto {

    private String accessToken;
    private UUID assignmentId;
    private UUID studentId;
}
