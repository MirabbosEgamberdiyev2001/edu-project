package uz.eduplatform.modules.inappnotification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InAppNotificationDto {
    private UUID id;
    private String type;
    private String title;
    private String body;
    private UUID referenceId;
    private String referenceType;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
