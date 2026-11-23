package org.ctrlprivacy.web.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

/**
 * Builds problem details in the shape every Ctrl Privacy API answers errors with: the status, its
 * reason phrase as the {@code title}, a {@code detail}, and a machine-readable {@code code}. It's
 * the same shape ctrlprivacy-security answers 401 and 403 with.
 */
public final class Problems {

    /** The problem property holding the machine-readable code. */
    public static final String CODE = "code";

    /** The code of a request that failed bean validation. */
    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";

    /** The code of an unexpected failure. */
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    /** Not instantiable: a factory of problems. */
    private Problems() {}

    /**
     * A problem detail with a code.
     *
     * @param status the HTTP status, also the problem's {@code status}
     * @param code the machine-readable code
     * @param detail the human-readable explanation the caller sees
     * @return the problem
     */
    public static ProblemDetail of(HttpStatusCode status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        HttpStatus known = HttpStatus.resolve(status.value());
        if (known != null) {
            problem.setTitle(known.getReasonPhrase());
        }
        problem.setProperty(CODE, code);
        return problem;
    }

    /**
     * The code for an error that has no business code of its own, such as Spring MVC's answer to an
     * unknown route: the status's name.
     *
     * @param status the HTTP status
     * @return its name, such as {@code NOT_FOUND}, or {@code HTTP_<status>} for a status Spring
     *     doesn't name
     */
    public static String codeFor(HttpStatusCode status) {
        HttpStatus known = HttpStatus.resolve(status.value());
        return known != null ? known.name() : "HTTP_" + status.value();
    }
}
