package uz.eduplatform.core.common.exception;

import lombok.Getter;

@Getter
public class ResourceNotFoundException extends RuntimeException {

    private final String messageKey;
    private final Object[] messageArgs;

    public ResourceNotFoundException(String resource, String field, Object value) {
        super(String.format("%s not found with %s: '%s'", resource, field, value));
        this.messageKey = "error.resource.not.found";
        this.messageArgs = new Object[]{resource, field, value};
    }

    public ResourceNotFoundException(String message) {
        super(message);
        this.messageKey = null;
        this.messageArgs = null;
    }
}
