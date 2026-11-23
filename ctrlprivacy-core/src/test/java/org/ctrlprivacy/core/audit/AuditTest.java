package org.ctrlprivacy.core.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

class AuditTest {

    private static final UUID USER_KEY = UUID.fromString("5c2f7a1e-9b3d-4e8a-a1f0-0d6c2b7e4f91");

    private final CallerAuditor auditor = new CallerAuditor();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordsTheCallersUserKey() {
        signIn(token(USER_KEY.toString()));

        assertThat(auditor.getCurrentAuditor()).contains(USER_KEY);
    }

    @Test
    void recordsSystemWithoutACaller() {
        assertThat(auditor.getCurrentAuditor()).contains(Auditors.SYSTEM);

        SecurityContextHolder.getContext()
                .setAuthentication(new AnonymousAuthenticationToken(
                        "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        assertThat(auditor.getCurrentAuditor()).contains(Auditors.SYSTEM);
    }

    @Test
    void recordsSystemForATokenWhoseSubjectIsNotAUserKey() {
        signIn(token("not-a-uuid"));

        assertThat(auditor.getCurrentAuditor()).contains(Auditors.SYSTEM);
    }

    @Test
    void theSystemAuditorIsTheNilUuid() {
        assertThat(Auditors.SYSTEM).hasToString("00000000-0000-0000-0000-000000000000");
    }

    @Test
    void stampsAFieldLevelUpdate() {
        signIn(token(USER_KEY.toString()));
        Instant now = Instant.parse("2026-11-23T12:00:00.123Z");
        AuditStamp stamp = new AuditStamp(auditor, Clock.fixed(now.plusNanos(456_789), ZoneOffset.UTC));

        Update update = stamp.touch(new Update().set("title", "New"));

        assertThat(update.getUpdateObject().get("$set", Document.class))
                .containsEntry("title", "New")
                .containsEntry("updatedAt", now)
                .containsEntry("updatedBy", USER_KEY)
                .doesNotContainKeys("createdAt", "createdBy");
    }

    /**
     * Signs a caller in with a verified token, as ctrlprivacy-security does.
     *
     * @param jwt the caller's token
     */
    private static void signIn(Jwt jwt) {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(jwt, null, "X"));
    }

    /**
     * A token for a subject.
     *
     * @param subject the token's {@code sub}: a user key
     * @return the token
     */
    private static Jwt token(String subject) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(subject)
                .build();
    }
}
