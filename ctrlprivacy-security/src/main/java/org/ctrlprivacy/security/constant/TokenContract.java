package org.ctrlprivacy.security.constant;

/**
 * The claims of a Hydra access token that Ctrl Privacy relies on. auth-service's token hook adds
 * {@link #PERMISSIONS}, and Hydra promotes it to the top level of the token through
 * {@code oauth2.allowed_top_level_claims}.
 */
public final class TokenContract {

    /**
     * The caller's subject: the user's key in auth-service, a UUID, which the services record as
     * who made a change. Read it with {@link org.ctrlprivacy.security.caller.Caller}.
     */
    public static final String SUBJECT = "sub";

    /** The caller's permission codes, such as {@code SCENARIO:WRITE}, as a JSON array of strings. */
    public static final String PERMISSIONS = "permissions";

    /** Hydra's container for custom claims that were not promoted to the top level. */
    public static final String EXTENSIONS = "ext";

    /** Not instantiable: a holder of constants. */
    private TokenContract() {}
}
