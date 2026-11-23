package org.ctrlprivacy.security.web;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Hands a {@code @PreAuthorize} denial back to Spring Security instead of letting Spring MVC answer
 * it, so a caller with no token gets 401 and one without the permission gets 403, as
 * {@link ProblemSecurityHandler} writes them. It runs before any other exception handler
 * ({@link Ordered#HIGHEST_PRECEDENCE}), so a catch-all, such as ctrlprivacy-web's, never turns a
 * denial into a 500.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityExceptionPassthrough {

    /**
     * Rethrows the exception, so it leaves the dispatcher for Spring Security's exception
     * translation filter.
     *
     * @param exception the denial or failed authentication
     * @throws RuntimeException always, the same exception
     */
    @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
    void rethrow(RuntimeException exception) {
        throw exception;
    }
}
