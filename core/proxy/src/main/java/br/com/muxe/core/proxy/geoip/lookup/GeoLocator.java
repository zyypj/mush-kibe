package br.com.muxe.core.proxy.geoip.lookup;

import br.com.muxe.core.common.account.domain.AccountLocation;
import java.io.IOException;
import java.net.InetAddress;

public interface GeoLocator {

    GeoLocator NONE = address -> AccountLocation.UNKNOWN;

    AccountLocation locate(InetAddress address) throws IOException;
}
