package org.ctrlprivacy.web.ip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetAddress;
import java.util.List;
import org.junit.jupiter.api.Test;

class TrustedProxiesTest {

    private final TrustedProxies proxies = new TrustedProxies(List.of("10.0.0.0/8", " 192.168.1.7 ", "fd00::/8", ""));

    @Test
    void matchesSingleAddressesAndCidrBlocks() {
        assertThat(proxies.contains(address("10.1.2.3"))).isTrue();
        assertThat(proxies.contains(address("192.168.1.7"))).isTrue();
        assertThat(proxies.contains(address("fd12::1"))).isTrue();

        assertThat(proxies.contains(address("11.0.0.1"))).isFalse();
        assertThat(proxies.contains(address("192.168.1.8"))).isFalse();
        assertThat(proxies.contains(address("fe80::1"))).isFalse();
    }

    @Test
    void matchesPartialBytesOfAPrefix() {
        TrustedProxies block = new TrustedProxies(List.of("172.16.0.0/12"));

        assertThat(block.contains(address("172.31.255.255"))).isTrue();
        assertThat(block.contains(address("172.32.0.0"))).isFalse();
    }

    @Test
    void neverMatchesAcrossAddressFamilies() {
        assertThat(new TrustedProxies(List.of("::/0")).contains(address("10.0.0.1")))
                .isFalse();
    }

    @Test
    void rejectsMalformedEntriesAtStartup() {
        assertThatThrownBy(() -> new TrustedProxies(List.of("localhost"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TrustedProxies(List.of("10.0.0.0/33")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TrustedProxies(List.of("10.0.0.0/x")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parsesLiteralsWithoutDns() {
        assertThat(TrustedProxies.parseLiteral("example.com")).isEmpty();
        assertThat(TrustedProxies.parseLiteral("unknown")).isEmpty();
        assertThat(TrustedProxies.parseLiteral("203.0.113.9:4711")).contains(address("203.0.113.9"));
        assertThat(TrustedProxies.parseLiteral("[2001:db8::1]:443")).contains(address("2001:db8::1"));
    }

    @Test
    void isEmptyWithNoEntries() {
        assertThat(new TrustedProxies(List.of()).isEmpty()).isTrue();
        assertThat(proxies.isEmpty()).isFalse();
    }

    /**
     * Parses an IP literal.
     *
     * @param literal the address
     * @return the address
     */
    private static InetAddress address(String literal) {
        return InetAddress.ofLiteral(literal);
    }
}
