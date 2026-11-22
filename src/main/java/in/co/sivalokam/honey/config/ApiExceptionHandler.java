package in.co.sivalokam.honey.config;

import in.co.sivalokam.honey.common.ApiException;
import in.co.sivalokam.honey.registrations.DuplicateRegistrationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    // Expected business errors (unknown product, out of stock, bad signature, ...)
    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApi(ApiException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        pd.setTitle(ex.getStatus().getReasonPhrase());
        pd.setProperty("errors", ex.getErrors());
        return pd;
    }

    // 400 – a field broke a rule (@NotBlank, @Pattern, ...)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> errors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Some fields need fixing");
        pd.setTitle("Validation failed");
        pd.setProperty("errors", errors);
        return pd;
    }

    // 400 – body is not valid JSON, or a wrong type (e.g. "consent": "yes")
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleBadJson(HttpMessageNotReadableException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Request body is missing or is not valid JSON");
        pd.setTitle("Malformed request");
        pd.setProperty("errors", Map.of());
        return pd;
    }

    // 409 – phone already registered
    @ExceptionHandler(DuplicateRegistrationException.class)
    public ProblemDetail handleDuplicate(DuplicateRegistrationException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Already registered");
        pd.setProperty("errors", Map.of("phone", ex.getMessage()));
        return pd;
    }

    // 405 / 415 / 404 / ResponseStatusException – keep Spring's status, same JSON shape
    @ExceptionHandler({HttpRequestMethodNotSupportedException.class,
            HttpMediaTypeNotSupportedException.class,
            ErrorResponseException.class})
    public ResponseEntity<ProblemDetail> handleSpringErrors(Exception ex) {
        var er = (org.springframework.web.ErrorResponse) ex;
        return ResponseEntity.status(er.getStatusCode()).body(er.getBody());
    }

    // 500 – anything unexpected. Log full details on the server, show a safe message to the client.
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong. Please try again.");
        pd.setTitle("Internal error");
        pd.setProperty("errors", Map.of());
        return pd;
    }
}
