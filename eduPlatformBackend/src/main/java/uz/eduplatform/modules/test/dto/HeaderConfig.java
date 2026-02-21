package uz.eduplatform.modules.test.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeaderConfig {

    private String schoolName;
    private String className;
    private String teacherName;
    private String logoUrl;
    private String date;
}
