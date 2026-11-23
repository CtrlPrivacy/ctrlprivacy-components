package org.ctrlprivacy.security.autoconfigure;

import org.ctrlprivacy.security.jwt.AccessTokenDecoders;
import org.ctrlprivacy.security.web.ProblemSecurityHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import tools.jackson.databind.json.JsonMapper;

/**
 * Verifies Hydra's access tokens once {@code ctrlprivacy.security.jwk-set-uri} is set. See
 * {@link SecurityFilterChainAutoConfiguration} for the routes.
 */
@AutoConfiguration(beforeName = SecurityAutoConfiguration.RESOURCE_SERVER_AUTO_CONFIGURATION)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(CtrlPrivacySecurityProperties.class)
public class SecurityAutoConfiguration {

    /** Spring Boot's own resource server setup, which this one replaces. */
    static final String RESOURCE_SERVER_AUTO_CONFIGURATION =
            "org.springframework.boot.security.oauth2.server.resource.autoconfigure."
                    + "OAuth2ResourceServerAutoConfiguration";

    /**
     * The decoder for Hydra's access tokens, unless the service defines its own (as tests do).
     *
     * @param properties the issuer, JWKS and audience
     * @return the decoder
     * @throws IllegalStateException if the JWKS is set without the issuer or audience
     */
    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    @ConditionalOnProperty(prefix = "ctrlprivacy.security", name = "jwk-set-uri")
    JwtDecoder jwtDecoder(CtrlPrivacySecurityProperties properties) {
        String issuer = properties.getIssuer();
        String audience = properties.getAudience();
        if (issuer == null || audience == null || properties.getJwkSetUri() == null) {
            throw new IllegalStateException("ctrlprivacy.security.issuer and ctrlprivacy.security.audience "
                    + "must be set with ctrlprivacy.security.jwk-set-uri");
        }
        return AccessTokenDecoders.create(properties.getJwkSetUri(), issuer, audience);
    }

    /**
     * The 401 and 403 answers, written with the application's JSON mapper when it has one.
     *
     * @param jsonMapper the application's mapper, if Spring Boot configured one
     * @return the handler
     */
    @Bean
    @ConditionalOnMissingBean
    ProblemSecurityHandler problemSecurityHandler(ObjectProvider<JsonMapper> jsonMapper) {
        return new ProblemSecurityHandler(
                jsonMapper.getIfAvailable(() -> JsonMapper.builder().build()));
    }
}
