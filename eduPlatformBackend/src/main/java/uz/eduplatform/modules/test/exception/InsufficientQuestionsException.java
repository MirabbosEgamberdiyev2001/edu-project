package uz.eduplatform.modules.test.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import uz.eduplatform.core.common.exception.BusinessException;

import java.util.Map;

@Getter
public class InsufficientQuestionsException extends BusinessException {

    private final Map<String, Integer> available;
    private final Map<String, Integer> required;

    public InsufficientQuestionsException(Map<String, Integer> available, Map<String, Integer> required) {
        super("test.insufficient.questions",
                new Object[]{
                        required.getOrDefault("easy", 0),
                        required.getOrDefault("medium", 0),
                        required.getOrDefault("hard", 0),
                        available.getOrDefault("easy", 0),
                        available.getOrDefault("medium", 0),
                        available.getOrDefault("hard", 0)
                },
                HttpStatus.BAD_REQUEST);
        this.available = available;
        this.required = required;
    }
}
