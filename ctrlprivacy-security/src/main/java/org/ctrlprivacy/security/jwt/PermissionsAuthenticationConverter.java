package org.ctrlprivacy.security.jwt;

import org.ctrlprivacy.security.constant.TokenContract;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * Turns a verified access token into the caller's authentication: the {@code sub} claim is the
 * principal name and each entry of the {@code permissions} claim is an authority, unprefixed, so
 * {@code @PreAuthorize("hasAuthority('SCENARIO:WRITE')")} matches the permission code as issued.
 */
public final class PermissionsAuthenticationConverter {

    /** Not instantiable: a factory of converters. */
    private PermissionsAuthenticationConverter() {}

    /**
     * Creates the converter. A token without a {@code permissions} claim converts to an
     * authentication with no authorities, which every permission check then denies.
     *
     * @return a converter reading authorities from {@link TokenContract#PERMISSIONS}
     */
    public static JwtAuthenticationConverter create() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(TokenContract.PERMISSIONS);
        authorities.setAuthorityPrefix("");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        converter.setPrincipalClaimName(TokenContract.SUBJECT);
        return converter;
    }
}
