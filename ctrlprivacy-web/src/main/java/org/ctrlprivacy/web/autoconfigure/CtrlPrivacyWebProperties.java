package org.ctrlprivacy.web.autoconfigure;

import java.util.List;
import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * The shared web layer's settings.
 *
 * <pre>
 * ctrlprivacy.web:
 *   trusted-proxies:
 *     - 10.42.0.0/16
 * </pre>
 */
@ConfigurationProperties("ctrlprivacy.web")
@Value
public class CtrlPrivacyWebProperties {

    /**
     * Addresses and CIDR blocks of the proxies allowed to report the client's address in
     * {@code X-Forwarded-For}. Empty, the header is never believed.
     */
    private final List<String> trustedProxies;

    /**
     * Binds the properties.
     *
     * @param trustedProxies the trusted proxies' addresses and CIDR blocks
     */
    public CtrlPrivacyWebProperties(@DefaultValue List<String> trustedProxies) {
        this.trustedProxies = List.copyOf(trustedProxies);
    }
}
