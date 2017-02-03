package org.endeavourhealth.hl7test.transforms.framework;

public class RuntimeParseException extends RuntimeException {
    public RuntimeParseException() {
        super();
    }
    public RuntimeParseException(String message) {
        super(message);
    }
    public RuntimeParseException(String message, Throwable cause) {
        super(message, cause);
    }
    public RuntimeParseException(Throwable cause) {
        super(cause);
    }
}
