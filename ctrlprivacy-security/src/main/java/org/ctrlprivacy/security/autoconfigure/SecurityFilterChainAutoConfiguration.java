package org.ctrlprivacy.security.autoconfigure;

import org.ctrlprivacy.security.jwt.PermissionsAuthenticationConverter;
import org.ctrlprivacy.security.web.ProblemSecurityHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Secures every route. The filter chain allows {@code ctrlprivacy.security.public-paths} and the
 * actuator endpoints, requires a token for the rest of {@code /v1/**}, and denies everything else.
 * Each endpoint then declares the permission it needs with {@code @PreAuthorize}. A service that
 * needs a route with other rules, such as a webhook with its own credential, adds a filter chain of
 * its own with a {@code securityMatcher} and a lower {@code @Order}.
 */
@AutoConfiguration(
        after = SecurityAutoConfiguration.class,
        beforeName = "org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration",
        afterName = "org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityFilterChainAutoConfiguration {

    /** The order of the main chain; a service's own chains for specific paths go before it. */
    public static final int ORDER = 10;

    /** The actuator's web endpoint settings, present when the service exposes actuator endpoints. */
    static final String WEB_ENDPOINT_PROPERTIES =
            "org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties";

    /**
     * The main chain: stateless bearer tokens, no CSRF (no cookies for it to ride on), public paths
     * open, {@code /v1/**} authenticated, anything else denied.
     *
     * @param http the builder Spring Security supplies
     * @param properties the public paths
     * @param problems the 401 and 403 answers
     * @return the chain
     * @throws IllegalStateException if the chain can't be built
     */
    @Bean
    @Order(ORDER)
    @ConditionalOnMissingBean(name = "ctrlprivacySecurityFilterChain")
    @ConditionalOnBean(JwtDecoder.class)
    SecurityFilterChain ctrlprivacySecurityFilterChain(
            HttpSecurity http, CtrlPrivacySecurityProperties properties, ProblemSecurityHandler problems) {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> {
                    if (!properties.getPublicPaths().isEmpty()) {
                        requests.requestMatchers(properties.getPublicPaths().toArray(String[]::new))
                                .permitAll();
                    }
                    requests.requestMatchers("/v1/**")
                            .authenticated()
                            .anyRequest()
                            .denyAll();
                })
                .oauth2ResourceServer(server -> server.jwt(
                                jwt -> jwt.jwtAuthenticationConverter(PermissionsAuthenticationConverter.create()))
                        .authenticationEntryPoint(problems)
                        .accessDeniedHandler(problems))
                .exceptionHandling(
                        handling -> handling.authenticationEntryPoint(problems).accessDeniedHandler(problems));
        try {
            return http.build();
        } catch (Exception e) {
            throw new IllegalStateException("Could not build the Ctrl Privacy security filter chain", e);
        }
    }

    /**
     * The actuator chain, set up only when the service exposes the actuator's web endpoints. Its
     * matcher looks the endpoints up at request time, so without them (a test slice, say) it would
     * fail on the first request rather than match nothing.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = WEB_ENDPOINT_PROPERTIES)
    @ConditionalOnBean(type = WEB_ENDPOINT_PROPERTIES)
    static class ActuatorChain {

        /**
         * The actuator endpoints (services expose only health and info) answer without a token, so
         * the cluster's probes can reach them.
         *
         * @param http the builder Spring Security supplies
         * @return the chain
         * @throws IllegalStateException if the chain can't be built
         */
        @Bean
        @Order(ORDER - 1)
        SecurityFilterChain ctrlprivacyActuatorSecurityFilterChain(HttpSecurity http) {
            http.securityMatcher(EndpointRequest.toAnyEndpoint())
                    .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                    .csrf(AbstractHttpConfigurer::disable);
            try {
                return http.build();
            } catch (Exception e) {
                throw new IllegalStateException("Could not build the actuator security filter chain", e);
            }
        }
    }
}
