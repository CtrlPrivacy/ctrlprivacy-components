package org.ctrlprivacy.core.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.jspecify.annotations.Nullable;

/**
 * The base every response DTO for an entity extends: the entity's key and when it was created and
 * last changed. MongoDB's {@code _id} is internal, and who made a change ({@code createdBy},
 * {@code updatedBy}) is kept for auditing, not answered, so neither is here.
 */
@Getter
@ToString
@EqualsAndHashCode
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseResponse {

    /** The entity's application identity, the only id the API uses. */
    private @Nullable UUID key;

    /** When the entity was first saved. */
    private @Nullable Instant createdAt;

    /** When it was last changed. */
    private @Nullable Instant updatedAt;
}
