package org.ctrlprivacy.security.jwt;

import java.net.URI;
import java.security.interfaces.RSAPublicKey;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Builds the decoder for Hydra's JWT access tokens. Hydra signs them with RS256 using its
 * {@code hydra.jwt.access-token} key set, published at {@code /.well-known/jwks.json}.
 */
public final class AccessTokenDecoders {

    /** Not instantiable: a factory of decoders. */
    private AccessTokenDecoders() {}

    /**
     * A decoder that accepts only RS256 tokens from {@code issuer} for {@code audience}, within
     * their lifetime, verified against the keys at {@code jwkSetUri}. The {@code typ} header is not
     * checked: the audience check already rejects Hydra's ID tokens, whose {@code aud} is the
     * client id rather than an API.
     *
     * @param jwkSetUri Hydra's JWKS, ideally its in-cluster address
     * @param issuer Hydra's issuer URL, exactly as it appears in {@code iss}
     * @param audience this API's audience
     * @return the configured decoder
     */
    public static JwtDecoder create(URI jwkSetUri, String issuer, String audience) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri.toString())
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .validateType(false)
                .build();
        decoder.setJwtValidator(validator(issuer, audience));
        return decoder;
    }

    /**
     * The same checks against a known public key, for tests that sign their own tokens.
     *
     * @param publicKey the key the test tokens are signed with
     * @param issuer the issuer the tokens must carry
     * @param audience the audience the tokens must carry
     * @return the configured decoder
     */
    public static JwtDecoder create(RSAPublicKey publicKey, String issuer, String audience) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .validateType(false)
                .build();
        decoder.setJwtValidator(validator(issuer, audience));
        return decoder;
    }

    /**
     * Combines the lifetime, issuer and audience checks every decoder applies.
     *
     * @param issuer the required {@code iss}
     * @param audience the audience {@code aud} must contain
     * @return the combined validator
     */
    private static OAuth2TokenValidator<Jwt> validator(String issuer, String audience) {
        return new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(), new JwtIssuerValidator(issuer), new AudienceValidator(audience));
    }
}
