package org.ctrlprivacy.web.ip;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {

    private final ClientIpResolver resolver = new ClientIpResolver(new TrustedProxies(List.of("10.0.0.0/8")));

    @Test
    void ignoresTheHeaderFromAnUntrustedPeer() {
        assertThat(resolver.resolve(request("203.0.113.5", "198.51.100.1"))).isEqualTo("203.0.113.5");
    }

    @Test
    void believesATrustedProxy() {
        assertThat(resolver.resolve(request("10.0.0.2", "198.51.100.1"))).isEqualTo("198.51.100.1");
    }

    @Test
    void ignoresWhatTheClientWroteItself() {
        // The client sent "X-Forwarded-For: 1.2.3.4"; the trusted proxies appended the real address.
        assertThat(resolver.resolve(request("10.0.0.2", "1.2.3.4, 198.51.100.1, 10.0.0.9")))
                .isEqualTo("198.51.100.1");
    }

    @Test
    void readsRepeatedHeadersInOrder() {
        MockHttpServletRequest request = request("10.0.0.2", "1.2.3.4");
        request.addHeader(ClientIpResolver.X_FORWARDED_FOR, "198.51.100.1");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.1");
    }

    @Test
    void takesTheLeftmostWhenEveryHopIsTrusted() {
        assertThat(resolver.resolve(request("10.0.0.2", "10.0.0.3, 10.0.0.4"))).isEqualTo("10.0.0.3");
    }

    @Test
    void stopsAtAnEntryThatIsNotAnAddress() {
        assertThat(resolver.resolve(request("10.0.0.2", "198.51.100.1, unknown, 10.0.0.4")))
                .isEqualTo("10.0.0.4");
    }

    @Test
    void fallsBackToThePeerWithoutTheHeader() {
        assertThat(resolver.resolve(request("10.0.0.2", null))).isEqualTo("10.0.0.2");
    }

    @Test
    void neverBelievesTheHeaderWithNoTrustedProxies() {
        ClientIpResolver none = new ClientIpResolver(new TrustedProxies(List.of()));

        assertThat(none.resolve(request("10.0.0.2", "198.51.100.1"))).isEqualTo("10.0.0.2");
    }

    /**
     * A request from a socket peer, with an optional {@code X-Forwarded-For}.
     *
     * @param peer the socket peer's address
     * @param forwardedFor the header's value, or null for none
     * @return the request
     */
    private static MockHttpServletRequest request(String peer, String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(peer);
        if (forwardedFor != null) {
            request.addHeader(ClientIpResolver.X_FORWARDED_FOR, forwardedFor);
        }
        return request;
    }
}
