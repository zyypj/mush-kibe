package br.com.muxe.core.common.config.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.core.common.config.ConfigException;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConfigSourceTest {

    @Test
    void acceptsIntegerValuesWithinTheSupportedRange() {
        ConfigSource source = new MapConfigSource(Map.of("minimum", Integer.MIN_VALUE, "maximum", (long) Integer.MAX_VALUE));

        assertThat(source.integer("minimum", 0)).isEqualTo(Integer.MIN_VALUE);
        assertThat(source.integer("maximum", 0)).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void rejectsNumbersOutsideTheIntegerRange() {
        ConfigSource source = new MapConfigSource(Map.of("limit", 4_294_967_297L));

        assertThatThrownBy(() -> source.integer("limit", 0))
            .isInstanceOf(ConfigException.class)
            .hasMessageContaining("limit");
    }

    @Test
    void rejectsFractionalNumbersInsteadOfTruncatingThem() {
        ConfigSource source = new MapConfigSource(Map.of("limit", 2.5));

        assertThatThrownBy(() -> source.integer("limit", 0))
            .isInstanceOf(ConfigException.class)
            .hasMessageContaining("limit");
    }

    @Test
    void reportsTheConfigurationKeyWhenADurationOverflows() {
        ConfigSource source = new MapConfigSource(Map.of("timeout", "9223372036854775807d"));

        assertThatThrownBy(() -> source.duration("timeout", Duration.ZERO))
            .isInstanceOf(ConfigException.class)
            .hasMessageContaining("timeout");
    }
}
