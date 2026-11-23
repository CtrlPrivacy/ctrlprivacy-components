package org.ctrlprivacy.core.audit;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.AuditorAware;

/** Who the audit trail records: a user's key, or {@link #SYSTEM}. */
public final class Auditors {

    /**
     * The auditor for work no caller asked for, such as a Kafka consumer's cascade or a startup
     * bootstrap: the nil UUID, {@code 00000000-0000-0000-0000-000000000000}, which no user key is.
     */
    public static final UUID SYSTEM = new UUID(0L, 0L);

    /** Not instantiable: a holder of constants and factories. */
    private Auditors() {}

    /**
     * An auditor that always records {@link #SYSTEM}, for a service without Spring Security.
     *
     * @return the auditor
     */
    public static AuditorAware<UUID> system() {
        return () -> Optional.of(SYSTEM);
    }
}
