package org.ctrlprivacy.web.error;

import java.io.Serializable;
import org.springframework.http.HttpStatus;

/**
 * A business error a service can answer with. Each service lists its own as an enum, so the
 * constant's name is the stable, machine-readable {@code code} clients see:
 *
 * <pre>
 * &#64;Getter
 * &#64;RequiredArgsConstructor
 * public enum ResourceError implements ErrorCode {
 *     NOT_FOUND(HttpStatus.NOT_FOUND, "Not found.");
 *
 *     private final HttpStatus status;
 *     private final String message;
 * }
 * </pre>
 *
 * <p>Serializable because {@link CtrlPrivacyException} holds one, and enums already are.
 */
public interface ErrorCode extends Serializable {

    /**
     * The machine-readable code, answered as the problem's {@code code}. An enum's constant name.
     *
     * @return the code, such as {@code NOT_FOUND}
     */
    String name();

    /**
     * The HTTP status the error is answered with.
     *
     * @return the status
     */
    HttpStatus getStatus();

    /**
     * The default human-readable explanation, answered as the problem's {@code detail} when the
     * exception doesn't carry a more specific one.
     *
     * @return the explanation
     */
    String getMessage();
}
