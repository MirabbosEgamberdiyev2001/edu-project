package uz.eduplatform.core.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String messageKey;
    private final Object[] messageArgs;

    public BusinessException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
        this.messageKey = null;
        this.messageArgs = null;
    }

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.messageKey = null;
        this.messageArgs = null;
    }

    public BusinessException(String messageKey, Object[] args, HttpStatus status) {
        super(messageKey);
        this.status = status;
        this.messageKey = messageKey;
        this.messageArgs = args;
    }

    public static BusinessException ofKey(String messageKey) {
        return new BusinessException(messageKey, null, HttpStatus.BAD_REQUEST);
    }

    public static BusinessException ofKey(String messageKey, Object... args) {
        return new BusinessException(messageKey, args, HttpStatus.BAD_REQUEST);
    }
}
