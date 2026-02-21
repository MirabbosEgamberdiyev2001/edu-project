package uz.eduplatform.modules.group.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberDto {

    private UUID id;
    private UUID studentId;
    private String studentName;
    private String studentEmail;
    private LocalDateTime joinedAt;
}
