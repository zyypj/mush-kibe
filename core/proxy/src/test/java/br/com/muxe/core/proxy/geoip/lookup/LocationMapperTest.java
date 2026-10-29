package br.com.muxe.core.proxy.geoip.lookup;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.account.domain.AccountLocation;
import com.maxmind.geoip2.model.AsnResponse;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Subdivision;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LocationMapperTest {

    @Test
    void namesAreInPortugueseWithTheFirstSubdivisionAsRegion() {
        CityResponse city = city(
            Map.of("en", "Brazil", "pt-BR", "Brasil"),
            List.of(
                new Subdivision(List.of(), null, 1L, "SP", Map.of("en", "Sao Paulo", "pt-BR", "São Paulo")),
                new Subdivision(List.of(), null, 2L, "XX", Map.of("en", "Second"))
            ),
            Map.of("en", "Campinas", "pt-BR", "Campinas")
        );
        AsnResponse asn = new AsnResponse(28573L, "Claro NXT Telecomunicacoes Ltda", null, null);

        AccountLocation location = LocationMapper.map(Optional.of(city), Optional.of(asn));

        assertThat(location).isEqualTo(new AccountLocation(
            Optional.of("Brasil"), Optional.of("São Paulo"), Optional.of("Campinas"), Optional.of("AS28573 Claro NXT Telecomunicacoes Ltda")));
    }

    @Test
    void englishIsUsedWhenThereIsNoPortugueseName() {
        CityResponse city = city(Map.of("en", "Germany", "de", "Deutschland"), List.of(), Map.of("en", "Falkenstein"));

        AccountLocation location = LocationMapper.map(Optional.of(city), Optional.empty());

        assertThat(location.country()).contains("Germany");
        assertThat(location.region()).isEmpty();
        assertThat(location.city()).contains("Falkenstein");
        assertThat(location.asn()).isEmpty();
    }

    @Test
    void namesInOtherLanguagesOnlyAreIgnored() {
        assertThat(LocationMapper.localized(Map.of("ja", "東京"))).isEmpty();
        assertThat(LocationMapper.localized(null)).isEmpty();
    }

    @Test
    void anAsnWithoutOrganizationKeepsTheNumber() {
        AccountLocation location = LocationMapper.map(Optional.empty(), Optional.of(new AsnResponse(15169L, null, null, null)));

        assertThat(location.asn()).contains("AS15169");
    }

    @Test
    void nothingFoundIsAnUnknownLocation() {
        assertThat(LocationMapper.map(Optional.empty(), Optional.empty()).isUnknown()).isTrue();
    }

    private static CityResponse city(Map<String, String> country, List<Subdivision> subdivisions, Map<String, String> city) {
        return new CityResponse(
            new City(List.of(), null, 3L, city),
            null,
            new Country(List.of(), null, 4L, false, "BR", country),
            null,
            null,
            null,
            null,
            null,
            subdivisions,
            null
        );
    }
}
