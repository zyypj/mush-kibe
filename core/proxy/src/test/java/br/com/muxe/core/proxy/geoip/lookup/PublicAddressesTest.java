package br.com.muxe.core.proxy.geoip.lookup;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PublicAddressesTest {

    @ParameterizedTest
    @ValueSource(strings = {"127.0.0.1", "10.0.0.5", "172.16.3.4", "192.168.1.10", "169.254.1.1", "100.64.0.1", "0.0.0.0", "::1", "fd12::1", "fe80::1", "224.0.0.1", "255.255.255.255"})
    void privateAndSpecialAddressesAreNotLookedUp(String address) throws UnknownHostException {
        assertThat(PublicAddresses.isPublic(InetAddress.getByName(address))).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"151.244.40.191", "8.8.8.8", "2804:14c::1"})
    void publicAddressesAreLookedUp(String address) throws UnknownHostException {
        assertThat(PublicAddresses.isPublic(InetAddress.getByName(address))).isTrue();
    }
}
