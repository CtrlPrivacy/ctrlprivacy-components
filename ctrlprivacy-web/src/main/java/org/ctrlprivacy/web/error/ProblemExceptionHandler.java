package org.ctrlprivacy.web.error;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Answers every error as a problem detail with a {@code code}: a {@link CtrlPrivacyException} with
 * its error's status and code, a failed {@code @Valid} as {@code VALIDATION_FAILED} with the invalid
 * fields, Spring MVC's own errors (an unknown route, a malformed body) with their status's name, and
 * anything else as a bare 500 {@code INTERNAL_ERROR} that's logged but never explained.
 *
 * <p>It runs last ({@link Ordered#LOWEST_PRECEDENCE}), so a service's own advice for an exception it
 * answers differently goes first when it carries an earlier {@code @Order}. Authentication and
 * authorization failures are ctrlprivacy-security's: its advice hands them back to Spring Security
 * before this runs, so this never answers them.
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class ProblemExceptionHandler extends ResponseEntityExceptionHandler {

    /** The problem property listing a {@code VALIDATION_FAILED} problem's invalid fields. */
    public static final String ERRORS = "errors";

    /**
     * A business error, with its status and code and the exception's message as the detail. A 5xx
     * error is logged with its cause; a 4xx is the caller's mistake and isn't.
     *
     * @param exception the error a controller or service threw
     * @return the problem
     */
    @ExceptionHandler(CtrlPrivacyException.class)
    ProblemDetail handleCtrlPrivacyException(CtrlPrivacyException exception) {
        ErrorCode error = exception.getError();
        if (error.getStatus().is5xxServerError()) {
            log.error("Request failed: {}", error.name(), exception);
        }
        String detail = exception.getMessage();
        return Problems.of(error.getStatus(), error.name(), detail != null ? detail : error.getMessage());
    }

    /**
     * Anything unexpected: logged in full, answered without its message so nothing internal leaks.
     *
     * @param exception the failure
     * @return a 500 problem with code {@code INTERNAL_ERROR}
     */
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception) {
        log.error("Unhandled exception", exception);
        return Problems.of(HttpStatus.INTERNAL_SERVER_ERROR, Problems.INTERNAL_ERROR, "An internal error occurred.");
    }

    /**
     * A request body that failed {@code @Valid}: answered 400 {@code VALIDATION_FAILED}, with each
     * invalid field and why in {@code errors}.
     *
     * @param ex the failure, holding the binding result
     * @param headers headers for the answer
     * @param status the status Spring MVC chose, 400
     * @param request the current request
     * @return the problem
     */
    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<InvalidField> errors = ex.getBindingResult().getAllErrors().stream()
                .map(error -> new InvalidField(
                        error instanceof FieldError field ? field.getField() : error.getObjectName(),
                        message(error.getDefaultMessage())))
                .toList();
        return handleExceptionInternal(ex, validationProblem(status, errors), headers, status, request);
    }

    /**
     * A handler parameter, such as a constrained {@code @RequestParam}, that failed method
     * validation: answered like a failed body, with each invalid parameter in {@code errors}.
     *
     * @param ex the failure, holding each parameter's result
     * @param headers headers for the answer
     * @param status the status Spring MVC chose, 400 for a parameter
     * @param request the current request
     * @return the problem
     */
    @Override
    protected @Nullable ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<InvalidField> errors = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new InvalidField(
                                parameterName(result.getMethodParameter().getParameterName()),
                                message(error.getDefaultMessage()))))
                .toList();
        return handleExceptionInternal(ex, validationProblem(status, errors), headers, status, request);
    }

    /**
     * Adds the status's name as the {@code code} of every problem Spring MVC answers itself, such as
     * {@code NOT_FOUND} for an unknown route, so every error a client sees carries a code.
     *
     * @param ex the exception being answered
     * @param body the problem Spring MVC built, or null to have it built
     * @param headers headers for the answer
     * @param statusCode the answer's status
     * @param request the current request
     * @return the answer, its problem carrying a code
     */
    @Override
    protected @Nullable ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof ProblemDetail problem) {
            Map<String, Object> properties = problem.getProperties();
            if (properties == null || !properties.containsKey(Problems.CODE)) {
                problem.setProperty(Problems.CODE, Problems.codeFor(statusCode));
            }
        }
        return response;
    }

    /**
     * The {@code VALIDATION_FAILED} problem.
     *
     * @param status the answer's status
     * @param errors each invalid field and why
     * @return the problem
     */
    private static ProblemDetail validationProblem(HttpStatusCode status, List<InvalidField> errors) {
        ProblemDetail problem = Problems.of(status, Problems.VALIDATION_FAILED, "The request is not valid.");
        problem.setProperty(ERRORS, errors);
        return problem;
    }

    /**
     * A validation message, or a generic one when the constraint has none.
     *
     * @param message the constraint's message
     * @return the message to answer
     */
    private static String message(@Nullable String message) {
        return message != null ? message : "is invalid";
    }

    /**
     * A parameter's name, which is only missing when the code wasn't compiled with
     * {@code -parameters}.
     *
     * @param name the name Spring found
     * @return the name, or {@code parameter}
     */
    private static String parameterName(@Nullable String name) {
        return name != null ? name : "parameter";
    }
}
