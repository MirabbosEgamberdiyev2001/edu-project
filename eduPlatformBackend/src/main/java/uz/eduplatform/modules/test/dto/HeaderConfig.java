package uz.eduplatform.modules.test.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeaderConfig {

    private String schoolName;
    private Map<String, String> schoolNameTranslations;
    private String className;
    private String teacherName;
    private Map<String, String> teacherNameTranslations;
    private String logoUrl;
    private String date;
}
