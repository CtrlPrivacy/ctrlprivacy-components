package org.ctrlprivacy.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.ctrlprivacy.security.testing.TestTokens;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;

class AccessTokenDecodersTest {

    private final JwtDecoder decoder =
            AccessTokenDecoders.create(TestTokens.publicKey(), TestTokens.ISSUER, TestTokens.AUDIENCE);

    @Test
    void acceptsATokenFromTheIssuerForThisAudience() {
        Jwt jwt = decoder.decode(TestTokens.token(List.of("SCENARIO:WRITE")));

        assertThat(jwt.getSubject()).isEqualTo("kratos-identity");
        assertThat(jwt.getClaimAsStringList("permissions")).containsExactly("SCENARIO:WRITE");
    }

    @Test
    void rejectsATokenForAnotherAudience() {
        String token = TestTokens.sign(TestTokens.claims(
                        TestTokens.ISSUER, "ctrlprivacy-gateway", Instant.now().plusSeconds(60))
                .build());

        assertThatThrownBy(() -> decoder.decode(token))
                .isInstanceOf(JwtValidationException.class)
                .hasMessageContaining("audience");
    }

    @Test
    void rejectsATokenFromAnotherIssuer() {
        String token = TestTokens.sign(TestTokens.claims(
                        "https://evil.example/",
                        TestTokens.AUDIENCE,
                        Instant.now().plusSeconds(60))
                .build());

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
    }

    @Test
    void rejectsAnExpiredToken() {
        String token = TestTokens.sign(TestTokens.claims(
                        TestTokens.ISSUER, TestTokens.AUDIENCE, Instant.now().minusSeconds(600))
                .build());

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }
}
