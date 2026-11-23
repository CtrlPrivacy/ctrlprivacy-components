package org.ctrlprivacy.security.jwt;

import java.util.List;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/** Accepts only tokens issued for this API's audience. */
public final class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final String audience;

    /**
     * Creates a validator for one API.
     *
     * @param audience the audience this API's tokens must name in {@code aud}, such as
     *     {@code ctrlprivacy-activity}
     */
    public AudienceValidator(String audience) {
        this.audience = audience;
    }

    /**
     * Succeeds when the token's {@code aud} claim contains this API's audience, and fails with
     * {@code invalid_token} otherwise, including when the token has no {@code aud} at all.
     *
     * @param jwt the decoded token, whose signature has already been checked
     * @return success, or a failure describing why the token is not for this API
     */
    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        List<String> audiences = jwt.getAudience();
        if (audiences != null && audiences.contains(audience)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "The token is not for this audience.", null));
    }
}
