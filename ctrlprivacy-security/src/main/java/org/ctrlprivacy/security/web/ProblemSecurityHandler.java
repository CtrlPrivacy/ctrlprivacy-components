package org.ctrlprivacy.security.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.json.JsonMapper;

/**
 * Answers the filter chain's 401 and 403 as {@code application/problem+json}, the same shape the
 * services use for every other error, with a {@code code} property of {@code UNAUTHORIZED} or
 * {@code FORBIDDEN}.
 */
public final class ProblemSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final JsonMapper jsonMapper;

    /**
     * Creates the handler.
     *
     * @param jsonMapper writes the problem details
     */
    public ProblemSecurityHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /**
     * Answers a request with no valid token with 401 and a {@code WWW-Authenticate: Bearer} header.
     *
     * @param request the rejected request
     * @param response where the problem is written
     * @param authException why authentication failed; not exposed to the caller
     * @throws IOException if the response can't be written
     */
    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        write(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "A valid access token is required.");
    }

    /**
     * Answers a request whose token lacks the permission the endpoint needs with 403.
     *
     * @param request the rejected request
     * @param response where the problem is written
     * @param accessDeniedException why access was denied; not exposed to the caller
     * @throws IOException if the response can't be written
     */
    @Override
    public void handle(
            HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        write(response, HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission to perform this action.");
    }

    /**
     * Writes one problem detail.
     *
     * @param response where it's written
     * @param status the HTTP status, also the problem's status and title
     * @param code the machine-readable error code
     * @param detail the human-readable explanation
     * @throws IOException if the response can't be written
     */
    private void write(HttpServletResponse response, HttpStatus status, String code, String detail) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("code", code);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), problem);
    }
}
