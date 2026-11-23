package org.ctrlprivacy.core.mapper;

import org.ctrlprivacy.core.entity.BaseEntity;
import org.ctrlprivacy.core.entity.EntityFields;
import org.mapstruct.MapperConfig;
import org.mapstruct.Mapping;
import org.mapstruct.MappingInheritanceStrategy;

/**
 * The MapStruct configuration every entity mapper uses, so a request never sets an entity's
 * identity or audit trail: those come from the builder's default ({@code key}), MongoDB
 * ({@code id}) and auditing (the rest).
 *
 * <pre>
 * &#64;Mapper(config = EntityMapperConfig.class)
 * public interface GroupMapper {
 *     GroupEntity toEntity(CreateGroupRequest request);   // base fields ignored automatically
 *     GroupResponse toResponse(GroupEntity entity);        // key and timestamps mapped; _id and the
 * }                                                        // "by" fields aren't in the response
 * </pre>
 */
@MapperConfig(mappingInheritanceStrategy = MappingInheritanceStrategy.AUTO_INHERIT_FROM_CONFIG)
public interface EntityMapperConfig {

    /**
     * The prototype every method mapping to an entity inherits: the base fields are never mapped
     * from the source.
     *
     * @param source any request
     * @return the entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = EntityFields.KEY, ignore = true)
    @Mapping(target = EntityFields.CREATED_AT, ignore = true)
    @Mapping(target = EntityFields.CREATED_BY, ignore = true)
    @Mapping(target = EntityFields.UPDATED_AT, ignore = true)
    @Mapping(target = EntityFields.UPDATED_BY, ignore = true)
    BaseEntity toEntity(Object source);
}
