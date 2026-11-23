package org.ctrlprivacy.core.autoconfigure;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.ctrlprivacy.core.audit.AuditStamp;
import org.ctrlprivacy.core.audit.Auditors;
import org.ctrlprivacy.core.audit.CallerAuditor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Turns on Spring Data MongoDB auditing, so every {@code save} of a {@code BaseEntity} sets its
 * created and updated timestamps and auditors, and provides {@link AuditStamp} for field-level
 * updates. Times come from the application's {@link Clock} bean when it has one, so tests can fix
 * them. A service must not add {@code @EnableMongoAuditing} of its own.
 */
@AutoConfiguration(afterName = "org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration")
@ConditionalOnClass(MongoTemplate.class)
@EnableMongoAuditing(
        auditorAwareRef = AuditingAutoConfiguration.AUDITOR,
        dateTimeProviderRef = AuditingAutoConfiguration.DATE_TIME_PROVIDER)
public class AuditingAutoConfiguration {

    /** The auditor bean's name. */
    public static final String AUDITOR = "ctrlprivacyAuditor";

    /** The time source bean's name. */
    public static final String DATE_TIME_PROVIDER = "ctrlprivacyDateTimeProvider";

    /** ctrlprivacy-security's view of the current caller, present when the service verifies tokens. */
    static final String CALLER = "org.ctrlprivacy.security.caller.Caller";

    /**
     * Timestamps for auditing, from the application's clock or the system's in UTC, to the
     * millisecond MongoDB stores, so a saved entity holds the same time a later read does.
     *
     * @param clock the application's clock, if it defines one
     * @return the time source
     */
    @Bean(DATE_TIME_PROVIDER)
    DateTimeProvider ctrlprivacyDateTimeProvider(ObjectProvider<Clock> clock) {
        Clock source = clock.getIfAvailable(Clock::systemUTC);
        return () -> Optional.of(source.instant().truncatedTo(ChronoUnit.MILLIS));
    }

    /**
     * The system UUID as auditor, when the service doesn't have ctrlprivacy-security. Declared on the
     * outer class so it's considered after {@link SecurityAuditor}.
     *
     * @return the auditor
     */
    @Bean(AUDITOR)
    @ConditionalOnMissingBean(name = AUDITOR)
    AuditorAware<UUID> ctrlprivacySystemAuditor() {
        return Auditors.system();
    }

    /**
     * Stamps field-level updates.
     *
     * @param auditor who the audit trail records
     * @param clock the application's clock, if it defines one
     * @return the stamp
     */
    @Bean
    @ConditionalOnMissingBean
    AuditStamp auditStamp(@Qualifier(AUDITOR) AuditorAware<UUID> auditor, ObjectProvider<Clock> clock) {
        return new AuditStamp(auditor, clock.getIfAvailable(Clock::systemUTC));
    }

    /** The caller's user key as auditor, when the service has ctrlprivacy-security. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = CALLER)
    static class SecurityAuditor {

        /**
         * Records the caller's user key, or the system UUID.
         *
         * @return the auditor
         */
        @Bean(AUDITOR)
        AuditorAware<UUID> ctrlprivacyAuditor() {
            return new CallerAuditor();
        }
    }
}
