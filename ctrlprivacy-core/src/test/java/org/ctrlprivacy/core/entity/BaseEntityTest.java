package org.ctrlprivacy.core.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.ctrlprivacy.core.entity.TestEntities.ImmutableEntity;
import org.ctrlprivacy.core.entity.TestEntities.MutableEntity;
import org.junit.jupiter.api.Test;
import org.springframework.data.auditing.IsNewAwareAuditingHandler;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

/**
 * Both entity styles through Spring Data's real mapping and auditing, without a database: the
 * stored field names, reading a document back, and the audit trail on a new and a saved entity.
 */
class BaseEntityTest {

    private static final Instant NOW = Instant.parse("2026-11-23T12:00:00Z");
    private static final UUID USER_KEY = UUID.fromString("5c2f7a1e-9b3d-4e8a-a1f0-0d6c2b7e4f91");
    private static final UUID CREATOR = UUID.fromString("9e1d4c3b-2a10-4f5e-8d7c-6b5a49382716");

    private final MongoCustomConversions conversions = new MongoCustomConversions(List.of());
    private final MongoMappingContext context = mappingContext(conversions);
    private final MappingMongoConverter converter = converter(context, conversions);
    private final IsNewAwareAuditingHandler auditing = auditing(USER_KEY);

    @Test
    void generatesAKeyWhenBuilt() {
        UUID first = MutableEntity.builder().title("a").build().getKey();
        UUID second = ImmutableEntity.builder().name("b").build().getKey();

        assertThat(first).isNotNull().isNotEqualTo(second);
    }

    @Test
    void storesTheKeyAsUnderscoreKey() {
        Document stored = new Document();
        converter.write(MutableEntity.builder().title("a").build(), stored);

        assertThat(stored).containsKey("_key").doesNotContainKey("key").containsEntry("title", "a");
    }

    @Test
    void readsAnImmutableEntityWithItsBaseFields() {
        ObjectId id = new ObjectId();
        UUID key = UUID.randomUUID();
        Document stored = new Document("_id", id)
                .append("_key", key)
                .append("name", "Strong passwords")
                .append("createdAt", java.util.Date.from(NOW))
                .append("createdBy", USER_KEY);

        ImmutableEntity entity = converter.read(ImmutableEntity.class, stored);

        assertThat(entity.getName()).isEqualTo("Strong passwords");
        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getKey()).isEqualTo(key);
        assertThat(entity.getCreatedAt()).isEqualTo(NOW);
        assertThat(entity.getCreatedBy()).isEqualTo(USER_KEY);
    }

    @Test
    void auditsANewEntityOfEitherStyle() {
        MutableEntity mutable = (MutableEntity)
                auditing.markAudited(MutableEntity.builder().title("a").build());
        ImmutableEntity immutable = (ImmutableEntity)
                auditing.markAudited(ImmutableEntity.builder().name("b").build());

        for (BaseEntity entity : new BaseEntity[] {mutable, immutable}) {
            assertThat(entity.getCreatedAt()).isEqualTo(NOW);
            assertThat(entity.getCreatedBy()).isEqualTo(USER_KEY);
            assertThat(entity.getUpdatedAt()).isEqualTo(NOW);
            assertThat(entity.getUpdatedBy()).isEqualTo(USER_KEY);
        }
    }

    @Test
    void keepsTheCreatorWhenAnotherCallerSaves() {
        ImmutableEntity saved = (ImmutableEntity) auditing.markAudited(
                ImmutableEntity.builder().name("b").id(new ObjectId()).build());
        ImmutableEntity created = ImmutableEntity.builder()
                .name("b")
                .id(new ObjectId())
                .createdAt(NOW.minusSeconds(60))
                .createdBy(CREATOR)
                .build();

        ImmutableEntity updated = (ImmutableEntity) auditing.markAudited(created);

        assertThat(saved.getCreatedBy()).isNull();
        assertThat(updated.getCreatedBy()).isEqualTo(CREATOR);
        assertThat(updated.getUpdatedBy()).isEqualTo(USER_KEY);
        assertThat(updated.getUpdatedAt()).isEqualTo(NOW);
    }

    /**
     * A mapping context that knows the test entities, set up as Spring Boot sets it up.
     *
     * @param conversions the store's conversions, which say which types are simple values
     * @return the context
     */
    private static MongoMappingContext mappingContext(MongoCustomConversions conversions) {
        MongoMappingContext context = new MongoMappingContext();
        context.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
        context.setInitialEntitySet(java.util.Set.of(MutableEntity.class, ImmutableEntity.class));
        context.initialize();
        return context;
    }

    /**
     * The converter that writes and reads documents.
     *
     * @param context the mapping context
     * @param conversions the store's conversions
     * @return the converter
     */
    private static MappingMongoConverter converter(MongoMappingContext context, MongoCustomConversions conversions) {
        MappingMongoConverter converter = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, context);
        converter.setCustomConversions(conversions);
        converter.afterPropertiesSet();
        return converter;
    }

    /**
     * Spring Data's auditing, as the auto-configuration sets it up, at a fixed time.
     *
     * @param auditor who it records
     * @return the handler
     */
    private IsNewAwareAuditingHandler auditing(UUID auditor) {
        IsNewAwareAuditingHandler handler = new IsNewAwareAuditingHandler(PersistentEntities.of(context));
        handler.setAuditorAware(() -> Optional.of(auditor));
        handler.setDateTimeProvider(() -> Optional.of(NOW));
        return handler;
    }
}
