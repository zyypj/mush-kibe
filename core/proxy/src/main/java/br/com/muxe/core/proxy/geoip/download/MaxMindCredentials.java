package br.com.muxe.core.proxy.geoip.download;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

public record MaxMindCredentials(String accountId, String licenseKey) {

    public MaxMindCredentials {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(licenseKey, "licenseKey");
    }

    public String basicAuthorization() {
        return "Basic " + Base64.getEncoder().encodeToString((accountId + ":" + licenseKey).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String toString() {
        return "MaxMindCredentials[accountId=" + accountId + ", licenseKey=***]";
    }
}
