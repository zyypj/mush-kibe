package br.com.muxe.core.proxy.geoip.lookup;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;

public final class PublicAddresses {

    private PublicAddresses() {
    }

    public static boolean isPublic(InetAddress address) {
        if (address == null || address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
            || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return false;
        }
        byte[] bytes = address.getAddress();
        if (address instanceof Inet4Address) {
            int first = bytes[0] & 0xFF;
            int second = bytes[1] & 0xFF;
            boolean sharedAddressSpace = first == 100 && second >= 64 && second <= 127;
            boolean reserved = first == 0 || first >= 240;
            return !sharedAddressSpace && !reserved;
        }
        if (address instanceof Inet6Address) {
            boolean uniqueLocal = (bytes[0] & 0xFE) == 0xFC;
            return !uniqueLocal;
        }
        return true;
    }
}
