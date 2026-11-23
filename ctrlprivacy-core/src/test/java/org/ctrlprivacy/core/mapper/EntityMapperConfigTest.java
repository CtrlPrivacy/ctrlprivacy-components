package org.ctrlprivacy.core.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.ctrlprivacy.core.entity.TestMappers;
import org.ctrlprivacy.core.entity.TestMappers.Created;
import org.junit.jupiter.api.Test;

class EntityMapperConfigTest {

    @Test
    void neverMapsTheBaseFieldsFromARequest() {
        Created created = TestMappers.create("Title");

        assertThat(created.title()).isEqualTo("Title");
        assertThat(created.key()).isNotNull();
        assertThat(created.id()).isNull();
        assertThat(created.createdBy()).isNull();
    }

    @Test
    void answersTheKeyAndTimestampsOnly() {
        Instant now = Instant.parse("2026-11-23T12:00:00Z");

        assertThat(TestMappers.respond("Title", now))
                .isEqualTo("key set, createdAt " + now + ", updatedAt " + now + ", title Title");
    }
}
