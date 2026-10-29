package br.com.muxe.core.proxy.geoip.download;

import java.io.IOException;
import java.io.InputStream;

public interface DatabaseSource {

    InputStream openTarGz(GeoEdition edition) throws IOException;
}
