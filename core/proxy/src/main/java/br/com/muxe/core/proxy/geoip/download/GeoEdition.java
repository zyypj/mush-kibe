package br.com.muxe.core.proxy.geoip.download;

public enum GeoEdition {
    CITY("GeoLite2-City"),
    ASN("GeoLite2-ASN");

    private final String id;

    GeoEdition(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String fileName() {
        return id + ".mmdb";
    }
}
