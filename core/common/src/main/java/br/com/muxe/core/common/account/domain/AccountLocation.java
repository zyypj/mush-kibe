package br.com.muxe.core.common.account.domain;

import java.util.Objects;
import java.util.Optional;

public record AccountLocation(Optional<String> country, Optional<String> region, Optional<String> city, Optional<String> asn) {

    public static final AccountLocation UNKNOWN = new AccountLocation(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

    public AccountLocation {
        Objects.requireNonNull(country, "country");
        Objects.requireNonNull(region, "region");
        Objects.requireNonNull(city, "city");
        Objects.requireNonNull(asn, "asn");
        country = clean(country);
        region = clean(region);
        city = clean(city);
        asn = clean(asn);
    }

    public boolean isUnknown() {
        return country.isEmpty() && region.isEmpty() && city.isEmpty() && asn.isEmpty();
    }

    private static Optional<String> clean(Optional<String> value) {
        return value.map(String::trim).filter(text -> !text.isEmpty());
    }
}
