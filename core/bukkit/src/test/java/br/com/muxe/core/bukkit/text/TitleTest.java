package br.com.muxe.core.bukkit.text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

class TitleTest {

    @Test
    void translatesColorsAndUsesDefaultTimings() {
        Title title = Title.of("&b&lMUXE", "&eUse /logar <senha>");

        assertThat(title.title()).isEqualTo(ChatColor.AQUA + "" + ChatColor.BOLD + "MUXE");
        assertThat(title.subtitle()).isEqualTo(ChatColor.YELLOW + "Use /logar <senha>");
        assertThat(title.fadeInTicks()).isEqualTo(Title.DEFAULT_FADE_IN);
        assertThat(title.stayTicks()).isEqualTo(Title.DEFAULT_STAY);
        assertThat(title.fadeOutTicks()).isEqualTo(Title.DEFAULT_FADE_OUT);
    }

    @Test
    void builderSetsCustomTimings() {
        Title title = Title.builder().title("Oi").timings(0, 40, 5).build();

        assertThat(title.subtitle()).isEmpty();
        assertThat(title.stayTicks()).isEqualTo(40);
    }

    @Test
    void refusesNegativeTimings() {
        assertThatThrownBy(() -> Title.builder().stay(-1).build()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void stripsColors() {
        assertThat(Colors.strip("&aOlá &bMuxe")).isEqualTo("Olá Muxe");
    }
}
