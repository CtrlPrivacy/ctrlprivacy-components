package org.ctrlprivacy.web.api;

import java.time.Instant;
import lombok.Value;
import org.jspecify.annotations.Nullable;

/**
 * The envelope a successful JSON answer is wrapped in: the data, an optional message, and when the
 * answer was made. Errors are never wrapped; they're answered as problem details
 * ({@code application/problem+json}) with a {@code code}, so the HTTP status alone says whether a
 * call succeeded.
 *
 * @param <T> the data's type
 */
@Value
public class ApiResponse<T> {

    /** What the call returned; null only for an answer that carries just a message. */
    @Nullable
    private final T data;

    /** A short human-readable note, such as {@code Tag retracted}, or null. */
    @Nullable
    private final String message;

    /** When the answer was made. */
    private final Instant timestamp;

    /**
     * Wraps the data of a successful call.
     *
     * @param data what the call returned
     * @param <T> the data's type
     * @return the envelope, stamped now
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(data, null, Instant.now());
    }

    /**
     * Wraps the data of a successful call with a message, or just a message when there's no data.
     *
     * @param data what the call returned, or null
     * @param message a short human-readable note
     * @param <T> the data's type
     * @return the envelope, stamped now
     */
    public static <T> ApiResponse<T> ok(@Nullable T data, String message) {
        return new ApiResponse<>(data, message, Instant.now());
    }
}
