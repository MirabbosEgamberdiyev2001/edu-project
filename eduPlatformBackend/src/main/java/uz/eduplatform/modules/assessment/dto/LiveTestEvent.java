package uz.eduplatform.modules.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveTestEvent {

    public enum EventType {
        STUDENT_STARTED,
        ANSWER_SAVED,
        TAB_SWITCH,
        SUBMITTED
    }

    private EventType eventType;
    private UUID assignmentId;
    private UUID studentId;
    private String studentName;
    private Integer answeredQuestions;
    private Integer totalQuestions;
    private Integer tabSwitchCount;
    private BigDecimal percentage;
}
