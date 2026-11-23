package org.ctrlprivacy.security.autoconfigure;

import org.ctrlprivacy.security.web.SecurityExceptionPassthrough;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Method security, so each endpoint declares the permission it needs with
 * {@code @PreAuthorize("hasAuthority('SCENARIO:WRITE')")}. {@code @PermitAll} and the other JSR-250
 * annotations are honored too. A denial thrown inside a controller is handed back to Spring
 * Security by {@link SecurityExceptionPassthrough}, so it's answered 401 or 403 like the filter
 * chain's.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableMethodSecurity(jsr250Enabled = true)
public class MethodSecurityAutoConfiguration {

    /**
     * Keeps Spring MVC's exception handlers from answering method security denials.
     *
     * @return the advice
     */
    @Bean
    @ConditionalOnMissingBean
    SecurityExceptionPassthrough securityExceptionPassthrough() {
        return new SecurityExceptionPassthrough();
    }
}
