package org.ctrlprivacy.core.audit;

import java.util.Optional;
import java.util.UUID;
import org.ctrlprivacy.security.caller.Caller;
import org.springframework.data.domain.AuditorAware;

/**
 * Records the caller's user key as the auditor, as ctrlprivacy-security's {@link Caller} reads it
 * from the access token's {@code sub}. Without a caller (a Kafka consumer, a scheduled job, an
 * anonymous request) it records {@link Auditors#SYSTEM}.
 */
public final class CallerAuditor implements AuditorAware<UUID> {

    /**
     * The current caller's user key, or {@link Auditors#SYSTEM}.
     *
     * @return the auditor, never empty, so every save records who made it
     */
    @Override
    public Optional<UUID> getCurrentAuditor() {
        return Optional.of(Caller.currentUserKey().orElse(Auditors.SYSTEM));
    }
}
