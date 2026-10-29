package br.com.muxe.core.bukkit.effect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class FireworkBurstTest {

    @Test
    void colorsAreWrittenInHex() {
        assertThat(FireworkBurst.color("#55FFFF")).isEqualTo(Color.fromRGB(0x55, 0xFF, 0xFF));
        assertThat(FireworkBurst.color("FF5555")).isEqualTo(Color.fromRGB(0xFF, 0x55, 0x55));
        assertThatThrownBy(() -> FireworkBurst.color("#FFF")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anEffectIsReadFromConfiguration() throws InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(String.join("\n",
            "burst:",
            "  type: burst",
            "  colors:",
            "    - \"#55FF55\"",
            "  fade-colors:",
            "    - \"#FFFFFF\"",
            "  flicker: true"
        ));

        FireworkEffect effect = FireworkBurst.effect(yaml.getConfigurationSection("burst"));

        assertThat(effect.getType()).isEqualTo(FireworkEffect.Type.BURST);
        assertThat(effect.getColors()).containsExactly(Color.fromRGB(0x55, 0xFF, 0x55));
        assertThat(effect.getFadeColors()).containsExactly(Color.WHITE);
        assertThat(effect.hasFlicker()).isTrue();
        assertThat(effect.hasTrail()).isFalse();
    }

    @Test
    void anEffectNeedsAColor() throws InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString("burst:\n  type: ball\n");

        assertThatThrownBy(() -> FireworkBurst.effect(yaml.getConfigurationSection("burst"))).isInstanceOf(IllegalArgumentException.class);
    }
}
