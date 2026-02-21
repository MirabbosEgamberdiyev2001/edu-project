package uz.eduplatform.modules.group.dto;

import lombok.*;
import uz.eduplatform.modules.group.domain.GroupStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupDto {

    private UUID id;
    private String name;
    private String description;

    private UUID teacherId;
    private String teacherName;

    private UUID subjectId;
    private String subjectName;

    private GroupStatus status;
    private int memberCount;

    private List<GroupMemberDto> members;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
