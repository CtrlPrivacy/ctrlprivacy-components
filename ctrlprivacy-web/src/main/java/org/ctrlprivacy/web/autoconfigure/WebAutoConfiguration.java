package org.ctrlprivacy.web.autoconfigure;

import org.ctrlprivacy.web.error.ProblemExceptionHandler;
import org.ctrlprivacy.web.ip.ClientIpResolver;
import org.ctrlprivacy.web.ip.TrustedProxies;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The shared web layer in a servlet service: problem detail error answers and the client IP
 * resolver. It runs before Spring Boot's MVC setup, so its exception handler stands in for Spring
 * Boot's own problem details handler.
 */
@AutoConfiguration(beforeName = WebAutoConfiguration.WEB_MVC_AUTO_CONFIGURATION)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = WebAutoConfiguration.RESPONSE_ENTITY_EXCEPTION_HANDLER)
@EnableConfigurationProperties(CtrlPrivacyWebProperties.class)
public class WebAutoConfiguration {

    /** Spring Boot's MVC setup, which registers its own problem details handler when asked to. */
    static final String WEB_MVC_AUTO_CONFIGURATION =
            "org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration";

    /** Spring MVC's base exception handler, which the error answers extend. */
    static final String RESPONSE_ENTITY_EXCEPTION_HANDLER =
            "org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler";

    /**
     * The client IP resolver, trusting the configured proxies.
     *
     * @param properties the trusted proxies
     * @return the resolver
     * @throws IllegalArgumentException if a trusted proxy entry is malformed, failing startup
     */
    @Bean
    @ConditionalOnMissingBean
    ClientIpResolver clientIpResolver(CtrlPrivacyWebProperties properties) {
        return new ClientIpResolver(new TrustedProxies(properties.getTrustedProxies()));
    }

    /**
     * The error answers, unless the service still has an exception handler of its own extending
     * Spring MVC's base one.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(type = RESPONSE_ENTITY_EXCEPTION_HANDLER)
    static class ErrorAnswers {

        /**
         * Answers every error as a problem detail with a code.
         *
         * @return the handler
         */
        @Bean
        ProblemExceptionHandler problemExceptionHandler() {
            return new ProblemExceptionHandler();
        }
    }
}
