package org.ctrlprivacy.security.autoconfigure;

import java.net.URI;
import java.util.List;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * How a service verifies Hydra's access tokens.
 *
 * <pre>
 * ctrlprivacy.security:
 *   issuer: https://auth.ctrlprivacy.org
 *   jwk-set-uri: http://hydra-public.ctrl-privacy-prod.svc.cluster.local:4444/.well-known/jwks.json
 *   audience: ctrlprivacy-activity
 *   public-paths:
 *     - /v1/connection/**
 * </pre>
 */
@ConfigurationProperties("ctrlprivacy.security")
@Value
public class CtrlPrivacySecurityProperties {

    /** Hydra's issuer URL, exactly as tokens carry it in {@code iss}. Required with {@code jwk-set-uri}. */
    private final @Nullable String issuer;

    /** Hydra's JWKS. Token verification, and the default filter chain, are on once it's set. */
    private final @Nullable URI jwkSetUri;

    /** This API's audience, such as {@code ctrlprivacy-activity}. Required with {@code jwk-set-uri}. */
    private final @Nullable String audience;

    /** Paths under {@code /v1} reachable without a token, such as public reads of published content. */
    private final List<String> publicPaths;

    /**
     * Binds the properties.
     *
     * @param issuer Hydra's issuer URL
     * @param jwkSetUri Hydra's JWKS
     * @param audience this API's audience
     * @param publicPaths request matcher patterns open without a token
     */
    public CtrlPrivacySecurityProperties(
            @Nullable String issuer,
            @Nullable URI jwkSetUri,
            @Nullable String audience,
            @DefaultValue List<String> publicPaths) {
        this.issuer = issuer;
        this.jwkSetUri = jwkSetUri;
        this.audience = audience;
        this.publicPaths = List.copyOf(publicPaths);
    }
}
