package uz.eduplatform.modules.group.dto;

import lombok.*;
import uz.eduplatform.modules.group.domain.GroupStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupDto {

    private UUID id;

    /** Resolved name for the current locale */
    private String name;
    /** Full translations map for edit forms */
    private Map<String, String> nameTranslations;

    /** Resolved description for the current locale */
    private String description;
    /** Full translations map for edit forms */
    private Map<String, String> descriptionTranslations;

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
