package org.ctrlprivacy.core.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import org.ctrlprivacy.core.dto.BaseResponse;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mongodb.core.mapping.Document;

/** The two entity styles the services use, extending {@link BaseEntity}. */
final class TestEntities {

    /** Not instantiable: a holder of test types. */
    private TestEntities() {}

    /** A mutable entity, as resource-service writes them. */
    @Data
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    @SuperBuilder(toBuilder = true)
    @NoArgsConstructor
    @Document("Mutable")
    static class MutableEntity extends BaseEntity {
        private String title;
    }

    /** An immutable entity, as activity-service and auth-service write them. */
    @Value
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    @SuperBuilder(toBuilder = true)
    @AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor_ = @PersistenceCreator)
    @Document("Immutable")
    static class ImmutableEntity extends BaseEntity {
        private final String name;
    }

    /** A request to create a mutable entity. */
    @Data
    static class CreateRequest {
        private String title;
    }

    /** The mutable entity's response. */
    @Getter
    @SuperBuilder
    static class MutableResponse extends BaseResponse {
        private final String title;
    }
}
