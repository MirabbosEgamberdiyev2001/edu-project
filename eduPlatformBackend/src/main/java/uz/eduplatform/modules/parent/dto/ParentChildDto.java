package uz.eduplatform.modules.parent.dto;

import lombok.*;
import uz.eduplatform.modules.parent.domain.PairingStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParentChildDto {

    private UUID id;

    private UUID parentId;
    private String parentName;

    private UUID childId;
    private String childName;
    private String childEmail;

    private PairingStatus status;
    private LocalDateTime pairedAt;
    private LocalDateTime revokedAt;
}
