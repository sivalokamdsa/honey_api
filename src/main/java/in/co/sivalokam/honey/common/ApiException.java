package in.co.sivalokam.honey.common;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * An expected, client-facing error. The handler renders it as
 * { "detail": "...", "errors": { "field": "..." } }.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final Map<String, String> errors;

    public ApiException(HttpStatus status, String detail) {
        this(status, detail, Map.of());
    }

    public ApiException(HttpStatus status, String detail, Map<String, String> errors) {
        super(detail);
        this.status = status;
        this.errors = errors == null ? Map.of() : Map.copyOf(errors);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
