package org.ctrlprivacy.core.entity;

import java.time.Instant;
import java.util.UUID;
import org.bson.types.ObjectId;
import org.ctrlprivacy.core.entity.TestEntities.CreateRequest;
import org.ctrlprivacy.core.entity.TestEntities.MutableEntity;
import org.ctrlprivacy.core.entity.TestEntities.MutableResponse;
import org.ctrlprivacy.core.mapper.EntityMapperConfig;
import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/** A MapStruct mapper using {@link EntityMapperConfig}, and helpers for the mapper's test. */
public final class TestMappers {

    private static final ThingMapper MAPPER = Mappers.getMapper(ThingMapper.class);

    /** Not instantiable: a holder of test helpers. */
    private TestMappers() {}

    /**
     * Maps a create request to an entity.
     *
     * @param title the request's title
     * @return what the entity holds
     */
    public static Created create(String title) {
        CreateRequest request = new CreateRequest();
        request.setTitle(title);
        MutableEntity entity = MAPPER.toEntity(request);
        return new Created(entity.getTitle(), entity.getKey(), entity.getId(), entity.getCreatedBy());
    }

    /**
     * Maps an audited entity to its response.
     *
     * @param title the entity's title
     * @param now its timestamps
     * @return a description of the response
     */
    public static String respond(String title, Instant now) {
        MutableEntity entity = MutableEntity.builder()
                .title(title)
                .id(new ObjectId())
                .createdAt(now)
                .createdBy(UUID.randomUUID())
                .updatedAt(now)
                .updatedBy(UUID.randomUUID())
                .build();
        MutableResponse response = MAPPER.toResponse(entity);
        return "key " + (entity.getKey().equals(response.getKey()) ? "set" : "missing") + ", createdAt "
                + response.getCreatedAt() + ", updatedAt " + response.getUpdatedAt() + ", title "
                + response.getTitle();
    }

    /**
     * What a mapped entity holds.
     *
     * @param title its title
     * @param key its key
     * @param id its id
     * @param createdBy its creator
     */
    public record Created(
            String title,
            UUID key,
            @Nullable ObjectId id,
            @Nullable UUID createdBy) {}

    @Mapper(config = EntityMapperConfig.class)
    interface ThingMapper {

        /**
         * A request to an entity; the base fields come from the config's prototype.
         *
         * @param request the request
         * @return the entity
         */
        MutableEntity toEntity(CreateRequest request);

        /**
         * An entity to its response; createdBy, updatedBy and id have no target.
         *
         * @param entity the entity
         * @return the response
         */
        MutableResponse toResponse(MutableEntity entity);
    }
}
