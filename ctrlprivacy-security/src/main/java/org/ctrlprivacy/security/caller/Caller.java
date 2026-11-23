package org.ctrlprivacy.security.caller;

import java.util.Optional;
import java.util.UUID;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.ClaimAccessor;
import org.springframework.security.oauth2.jwt.JwtClaimNames;

/**
 * The current caller, from the verified access token on this thread. Other modules ask this
 * rather than reading the security context or the token's claims themselves.
 */
public final class Caller {

    /** Not instantiable: a holder of static lookups. */
    private Caller() {}

    /**
     * The caller's user key: the access token's {@code sub}, a UUID.
     *
     * @return the key; empty without an authenticated caller (a Kafka consumer, a scheduled job, an
     *     anonymous request) or when the token's subject isn't a UUID
     */
    public static Optional<UUID> currentUserKey() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof ClaimAccessor claims)) {
            return Optional.empty();
        }
        String subject = claims.getClaimAsString(JwtClaimNames.SUB);
        if (subject == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(subject));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
