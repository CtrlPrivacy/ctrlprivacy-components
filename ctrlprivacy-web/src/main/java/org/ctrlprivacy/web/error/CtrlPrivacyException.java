package org.ctrlprivacy.web.error;

import java.io.Serial;
import lombok.Getter;

/**
 * A business error, answered by {@link ProblemExceptionHandler} as a problem detail with the
 * error's status, its {@code code}, and the exception's message as the {@code detail}. The message
 * reaches the caller, so it must never hold anything the caller shouldn't see.
 */
@Getter
public class CtrlPrivacyException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Which error this is, deciding the status and {@code code}. */
    private final ErrorCode error;

    /**
     * An error with its default explanation.
     *
     * @param error which error this is
     */
    public CtrlPrivacyException(ErrorCode error) {
        super(error.getMessage());
        this.error = error;
    }

    /**
     * An error with a more specific explanation, such as which key wasn't found.
     *
     * @param error which error this is
     * @param detail the explanation the caller sees
     */
    public CtrlPrivacyException(ErrorCode error, String detail) {
        super(detail);
        this.error = error;
    }

    /**
     * An error with a specific explanation and the failure that caused it. The cause is logged for a
     * 5xx error but never answered.
     *
     * @param error which error this is
     * @param detail the explanation the caller sees
     * @param cause the underlying failure
     */
    public CtrlPrivacyException(ErrorCode error, String detail, Throwable cause) {
        super(detail, cause);
        this.error = error;
    }
}
