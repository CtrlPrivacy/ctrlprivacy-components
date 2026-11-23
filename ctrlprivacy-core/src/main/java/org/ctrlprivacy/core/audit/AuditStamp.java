package org.ctrlprivacy.core.audit;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.ctrlprivacy.core.entity.EntityFields;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.mongodb.core.query.Update;

/**
 * Stamps a field-level update with when and by whom. Spring Data auditing only runs on
 * {@code save}, so an {@code updateFirst} or {@code updateMulti} that changes an entity in place
 * adds {@code updatedAt} and {@code updatedBy} through this instead.
 */
public final class AuditStamp {

    private final AuditorAware<UUID> auditor;
    private final Clock clock;

    /**
     * Creates the stamp.
     *
     * @param auditor who the audit trail records
     * @param clock the clock {@code updatedAt} is read from
     */
    public AuditStamp(AuditorAware<UUID> auditor, Clock clock) {
        this.auditor = auditor;
        this.clock = clock;
    }

    /**
     * Adds {@code updatedAt} (now, to the millisecond MongoDB stores) and {@code updatedBy} (the
     * current auditor) to an update.
     *
     * @param update the update to run with the entity's class, so the property names map
     * @return the same update, for chaining
     */
    public Update touch(Update update) {
        return update.set(EntityFields.UPDATED_AT, Instant.now(clock).truncatedTo(ChronoUnit.MILLIS))
                .set(EntityFields.UPDATED_BY, auditor.getCurrentAuditor().orElse(Auditors.SYSTEM));
    }
}
