package br.com.muxe.core.common.text;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextWidthTest {

    @Test
    void lettersFollowTheMinecraftFont() {
        assertThat(TextWidth.of("MUXE")).isEqualTo(24);
        assertThat(TextWidth.of("il!")).isEqualTo(7);
        assertThat(TextWidth.of(" ")).isEqualTo(TextWidth.SPACE);
        assertThat(TextWidth.of("ÇÉ")).isEqualTo(12);
    }

    @Test
    void colorCodesTakeNoSpaceAndBoldAddsOnePixelUntilTheNextColor() {
        assertThat(TextWidth.of("&b&lMUXE")).isEqualTo(28);
        assertThat(TextWidth.of("§lAB&aCD")).isEqualTo(26);
        assertThat(TextWidth.of("&lA&mB&rC")).isEqualTo(20);
        assertThat(TextWidth.of("&zA")).isEqualTo(TextWidth.of("&zA".replace("&z", "")) + 12);
    }

    @Test
    void centeringPadsWithSpacesSoTheTextSitsInTheMiddle() {
        String centered = TextWidth.center("&a&lVENHA JOGAR", TextWidth.SERVER_LIST_WIDTH);

        int padding = centered.indexOf('&');
        int left = padding * TextWidth.SPACE;
        int right = TextWidth.SERVER_LIST_WIDTH - left - TextWidth.of("&a&lVENHA JOGAR");
        assertThat(centered).endsWith("&a&lVENHA JOGAR").startsWith(" ");
        assertThat(Math.abs(left - right)).isLessThanOrEqualTo(TextWidth.SPACE);
    }

    @Test
    void textWiderThanTheAreaIsLeftAlone() {
        String wide = "W".repeat(60);

        assertThat(TextWidth.center(wide, TextWidth.SERVER_LIST_WIDTH)).isEqualTo(wide);
    }
}
