package org.ctrlprivacy.core.entity;

import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.bson.types.ObjectId;
import org.ctrlprivacy.core.audit.Auditors;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * The base every Ctrl Privacy MongoDB entity extends: its identity and its audit trail.
 *
 * <ul>
 *   <li>{@code _id}: MongoDB's own id, internal, never answered.
 *   <li>{@code _key}: the application identity, a UUID generated when the entity is built, unique,
 *       and the only id the API uses or other documents reference.
 *   <li>{@code createdAt}, {@code createdBy}, {@code updatedAt}, {@code updatedBy}: set by Spring
 *       Data auditing on every {@code save}; the "by" fields are the caller's user key (the
 *       access token's {@code sub}, through ctrlprivacy-security's {@code Caller}), or
 *       {@link Auditors#SYSTEM} for work no caller asked for. Field-level updates stamp the "updated" pair
 *       with {@link org.ctrlprivacy.core.audit.AuditStamp}.
 * </ul>
 *
 * <p>A subclass uses {@code @SuperBuilder(toBuilder = true)}, so its builder sets these too, which
 * tests rely on; services leave them to the builder's default and to auditing. The fields have no
 * public setters: Spring Data sets them directly, so an immutable ({@code @Value}) subclass stays
 * immutable to its callers. Such a subclass marks its all-arguments constructor
 * {@code @PersistenceCreator} so Spring Data builds it from its own fields, then sets these.
 */
@Getter
@ToString
@EqualsAndHashCode
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// Spring Data sets every field directly after building an immutable subclass, so none can be final.
@SuppressWarnings("PMD.ImmutableField")
public abstract class BaseEntity {

    /** MongoDB's id: internal, never answered or referenced. Null until first saved. */
    @Id
    private @Nullable ObjectId id;

    /** The application identity, stored as {@code _key}: generated when the entity is built. */
    @Indexed(unique = true)
    @Field(EntityFields.STORED_KEY)
    @Builder.Default
    private UUID key = UUID.randomUUID();

    /** When the entity was first saved. */
    @CreatedDate
    private @Nullable Instant createdAt;

    /** Who first saved it: the caller's user key, or {@link Auditors#SYSTEM}. Never answered. */
    @CreatedBy
    private @Nullable UUID createdBy;

    /** When it was last saved or updated. */
    @LastModifiedDate
    private @Nullable Instant updatedAt;

    /** Who last saved or updated it: a user key, or {@link Auditors#SYSTEM}. Never answered. */
    @LastModifiedBy
    private @Nullable UUID updatedBy;
}
