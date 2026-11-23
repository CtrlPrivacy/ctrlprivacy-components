package org.ctrlprivacy.web.ip;

import java.net.InetAddress;
import java.util.List;
import java.util.Optional;

/**
 * The proxies allowed to report a client's address in {@code X-Forwarded-For}: single addresses
 * and CIDR blocks, IPv4 or IPv6. Entries are parsed once, as literals only, so a hostname is
 * rejected rather than looked up in DNS, and a malformed entry fails at startup rather than being
 * silently ignored.
 */
public final class TrustedProxies {

    private final List<Block> blocks;

    /**
     * Parses the configured entries.
     *
     * @param entries addresses such as {@code 10.0.0.7} and blocks such as {@code 10.0.0.0/8} or
     *     {@code fd00::/8}
     * @throws IllegalArgumentException if an entry isn't an IP literal or a valid CIDR block
     */
    public TrustedProxies(List<String> entries) {
        this.blocks = entries.stream()
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .map(Block::parse)
                .toList();
    }

    /**
     * Whether no proxy is trusted, in which case {@code X-Forwarded-For} is never believed.
     *
     * @return true if there are no entries
     */
    public boolean isEmpty() {
        return blocks.isEmpty();
    }

    /**
     * Whether an address is one of the trusted proxies.
     *
     * @param address the address to check
     * @return true if it falls in any entry
     */
    public boolean contains(InetAddress address) {
        byte[] bytes = address.getAddress();
        return blocks.stream().anyMatch(block -> block.contains(bytes));
    }

    /**
     * Parses an IP literal without any DNS lookup. A bracketed IPv6 address and an IPv4 address with
     * a port, as some proxies write them, are accepted without the port.
     *
     * @param value the text, such as a socket peer or an {@code X-Forwarded-For} entry
     * @return the address, or empty if the text isn't an IP literal
     */
    static Optional<InetAddress> parseLiteral(String value) {
        String literal = value.trim();
        if (literal.startsWith("[")) {
            int end = literal.indexOf(']');
            literal = end > 0 ? literal.substring(1, end) : literal;
        } else if (literal.indexOf(':') == literal.lastIndexOf(':') && literal.indexOf('.') >= 0) {
            int colon = literal.indexOf(':');
            literal = colon > 0 ? literal.substring(0, colon) : literal;
        }
        try {
            return Optional.of(InetAddress.ofLiteral(literal));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** One entry: a network address and how many leading bits of it must match. */
    private static final class Block {

        private final byte[] network;
        private final int prefixLength;

        /**
         * Creates a block.
         *
         * @param network the network address's bytes, 4 or 16
         * @param prefixLength the number of leading bits that must match
         */
        private Block(byte[] network, int prefixLength) {
            this.network = network.clone();
            this.prefixLength = prefixLength;
        }

        /**
         * Parses an address, which becomes a block of one, or a CIDR block.
         *
         * @param entry the trimmed entry
         * @return the block
         * @throws IllegalArgumentException if the entry isn't valid
         */
        static Block parse(String entry) {
            int slash = entry.indexOf('/');
            String address = slash < 0 ? entry : entry.substring(0, slash);
            InetAddress network;
            try {
                network = InetAddress.ofLiteral(address);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Trusted proxy '" + entry + "' is not an IP address or CIDR block", e);
            }
            int bits = network.getAddress().length * 8;
            if (slash < 0) {
                return new Block(network.getAddress(), bits);
            }
            int prefixLength;
            try {
                prefixLength = Integer.parseInt(entry.substring(slash + 1));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Trusted proxy '" + entry + "' has an invalid prefix length", e);
            }
            if (prefixLength < 0 || prefixLength > bits) {
                throw new IllegalArgumentException(
                        "Trusted proxy '" + entry + "' has a prefix length outside 0-" + bits);
            }
            return new Block(network.getAddress(), prefixLength);
        }

        /**
         * Whether an address's first {@link #prefixLength} bits match the network's. An IPv4 address
         * never matches an IPv6 block or the other way round.
         *
         * @param address the address's bytes
         * @return true if it's in the block
         */
        boolean contains(byte[] address) {
            if (address.length != network.length) {
                return false;
            }
            int fullBytes = prefixLength / 8;
            for (int i = 0; i < fullBytes; i++) {
                if (address[i] != network[i]) {
                    return false;
                }
            }
            int remainingBits = prefixLength % 8;
            if (remainingBits == 0) {
                return true;
            }
            int mask = 0xFF << (8 - remainingBits);
            return (address[fullBytes] & mask) == (network[fullBytes] & mask);
        }
    }
}
