package br.com.muxe.core.proxy.geoip.lookup;

import br.com.muxe.core.common.account.domain.AccountLocation;
import com.maxmind.geoip2.NamedRecord;
import com.maxmind.geoip2.model.AsnResponse;
import com.maxmind.geoip2.model.CityResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class LocationMapper {

    public static final List<String> LOCALES = List.of("pt-BR", "en");

    private LocationMapper() {
    }

    public static AccountLocation map(Optional<CityResponse> city, Optional<AsnResponse> asn) {
        return new AccountLocation(
            city.map(CityResponse::country).flatMap(LocationMapper::name),
            city.map(CityResponse::subdivisions).filter(list -> !list.isEmpty()).map(List::getFirst).flatMap(LocationMapper::name),
            city.map(CityResponse::city).flatMap(LocationMapper::name),
            asn.flatMap(LocationMapper::asn)
        );
    }

    public static Optional<String> localized(Map<String, String> names) {
        if (names == null) {
            return Optional.empty();
        }
        return LOCALES.stream()
            .map(names::get)
            .filter(name -> name != null && !name.isBlank())
            .findFirst();
    }

    private static Optional<String> name(NamedRecord record) {
        return record == null ? Optional.empty() : localized(record.names());
    }

    private static Optional<String> asn(AsnResponse response) {
        Long number = response.autonomousSystemNumber();
        if (number == null) {
            return Optional.empty();
        }
        String organization = response.autonomousSystemOrganization();
        return Optional.of(organization == null || organization.isBlank() ? "AS" + number : "AS" + number + " " + organization.strip());
    }
}
