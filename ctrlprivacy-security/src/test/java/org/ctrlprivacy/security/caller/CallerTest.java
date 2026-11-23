package org.ctrlprivacy.security.caller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class CallerTest {

    private static final UUID USER_KEY = UUID.fromString("5c2f7a1e-9b3d-4e8a-a1f0-0d6c2b7e4f91");

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void readsTheUserKeyFromTheSubject() {
        signIn(USER_KEY.toString());

        assertThat(Caller.currentUserKey()).contains(USER_KEY);
    }

    @Test
    void hasNoUserKeyWithoutACaller() {
        assertThat(Caller.currentUserKey()).isEmpty();

        SecurityContextHolder.getContext()
                .setAuthentication(new AnonymousAuthenticationToken(
                        "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        assertThat(Caller.currentUserKey()).isEmpty();
    }

    @Test
    void hasNoUserKeyWhenTheSubjectIsNotAUuid() {
        signIn("not-a-uuid");

        assertThat(Caller.currentUserKey()).isEmpty();
    }

    /**
     * Signs a caller in with a verified token, as the resource server does.
     *
     * @param subject the token's {@code sub}
     */
    private static void signIn(String subject) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(subject)
                .build();
        SecurityContextHolder.getContext()
                .setAuthentication(new JwtAuthenticationToken(jwt, AuthorityUtils.NO_AUTHORITIES));
    }
}
