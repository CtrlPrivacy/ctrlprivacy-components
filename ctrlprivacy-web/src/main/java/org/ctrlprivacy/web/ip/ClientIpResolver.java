package org.ctrlprivacy.web.ip;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Finds the address of the client behind the gateway and the cluster's proxies, for rate limiting
 * and geolocation. {@code X-Forwarded-For} is believed only as far as trusted proxies vouch for it:
 * starting from the socket peer, it's walked from the right (the hop nearest this service), and the
 * first address that isn't a trusted proxy is the client. Anything to the left of that was written
 * by the client itself and could be forged, so it's never used.
 */
public final class ClientIpResolver {

    /** The header proxies append the address they received a request from to. */
    public static final String X_FORWARDED_FOR = "X-Forwarded-For";

    /** Separates the entries of one header. */
    private static final Pattern COMMA = Pattern.compile(",");

    private final TrustedProxies trustedProxies;

    /**
     * Creates a resolver.
     *
     * @param trustedProxies the proxies whose {@code X-Forwarded-For} entries are believed
     */
    public ClientIpResolver(TrustedProxies trustedProxies) {
        this.trustedProxies = trustedProxies;
    }

    /**
     * The client's address. When the socket peer isn't a trusted proxy, it's the client and the
     * header is ignored. Otherwise the header's entries are walked from the right until one isn't a
     * trusted proxy; if every entry is trusted, the leftmost is the client. An entry that isn't an
     * IP address stops the walk at the last address before it.
     *
     * @param request the current request
     * @return the client's address, as text
     */
    public String resolve(HttpServletRequest request) {
        String peer = request.getRemoteAddr();
        Optional<InetAddress> peerAddress = TrustedProxies.parseLiteral(peer);
        if (peerAddress.isEmpty() || !trustedProxies.contains(peerAddress.get())) {
            return peer;
        }
        List<String> hops = forwardedFor(request);
        String client = peer;
        for (int i = hops.size() - 1; i >= 0; i--) {
            Optional<InetAddress> hop = TrustedProxies.parseLiteral(hops.get(i));
            if (hop.isEmpty()) {
                return client;
            }
            client = hop.get().getHostAddress();
            if (!trustedProxies.contains(hop.get())) {
                return client;
            }
        }
        return client;
    }

    /**
     * Every {@code X-Forwarded-For} entry, across repeated headers, leftmost first.
     *
     * @param request the current request
     * @return the entries, trimmed, empty ones dropped
     */
    private static List<String> forwardedFor(HttpServletRequest request) {
        List<String> hops = new ArrayList<>();
        for (String header : Collections.list(request.getHeaders(X_FORWARDED_FOR))) {
            for (String entry : COMMA.split(header, -1)) {
                String trimmed = entry.trim();
                if (!trimmed.isEmpty()) {
                    hops.add(trimmed);
                }
            }
        }
        return hops;
    }
}
