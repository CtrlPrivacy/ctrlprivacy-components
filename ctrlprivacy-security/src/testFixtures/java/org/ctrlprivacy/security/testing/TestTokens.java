package org.ctrlprivacy.security.testing;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Signs access tokens shaped like Hydra's with a throwaway RSA key, for tests. Pair it with
 * {@code AccessTokenDecoders.create(TestTokens.publicKey(), TestTokens.ISSUER, audience)} as the
 * test's {@code JwtDecoder} bean.
 */
public final class TestTokens {

    /** The issuer the tokens carry: Hydra's local address. */
    public static final String ISSUER = "http://localhost:4444/";

    /** The audience {@link #token(List)} signs with. */
    public static final String AUDIENCE = "ctrlprivacy-test";

    private static final KeyPair KEYS = generate();

    /** Not instantiable: a holder of test helpers. */
    private TestTokens() {}

    /**
     * The public half of the test key, to build a decoder with.
     *
     * @return the key the test tokens are signed with
     */
    public static RSAPublicKey publicKey() {
        return (RSAPublicKey) KEYS.getPublic();
    }

    /**
     * A valid token for the test issuer and audience, expiring in an hour.
     *
     * @param permissions the permission codes for the {@code permissions} claim
     * @return the signed token
     */
    public static String token(List<String> permissions) {
        return token(AUDIENCE, permissions);
    }

    /**
     * A valid token for the test issuer and the given audience, expiring in an hour.
     *
     * @param audience the API audience, such as {@code ctrlprivacy-auth}
     * @param permissions the permission codes for the {@code permissions} claim
     * @return the signed token
     */
    public static String token(String audience, List<String> permissions) {
        return sign(claims(ISSUER, audience, Instant.now().plusSeconds(3600))
                .claim("permissions", permissions)
                .build());
    }

    /**
     * Claims with the given issuer, audience and expiry, and subject {@code kratos-identity}.
     *
     * @param issuer the {@code iss}
     * @param audience the single {@code aud}
     * @param expiresAt the {@code exp}
     * @return the claims builder, to add more claims to
     */
    public static JWTClaimsSet.Builder claims(String issuer, String audience, Instant expiresAt) {
        return new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(audience)
                .subject("kratos-identity")
                .issueTime(Date.from(Instant.now().minusSeconds(60)))
                .expirationTime(Date.from(expiresAt));
    }

    /**
     * Signs claims with the test key, as Hydra signs with RS256.
     *
     * @param claims the claims to sign
     * @return the compact serialized token
     * @throws IllegalStateException if signing fails
     */
    public static String sign(JWTClaimsSet claims) {
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test").build(), claims);
        try {
            jwt.sign(new RSASSASigner((RSAPrivateKey) KEYS.getPrivate()));
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
        return jwt.serialize();
    }

    /**
     * Generates the test key pair once.
     *
     * @return a 2048-bit RSA key pair
     * @throws IllegalStateException if the JDK has no RSA
     */
    private static KeyPair generate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
