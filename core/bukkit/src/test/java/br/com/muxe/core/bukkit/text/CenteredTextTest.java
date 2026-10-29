package br.com.muxe.core.bukkit.text;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CenteredTextTest {

    @Test
    void widthsFollowTheDefaultFont() {
        assertThat(CenteredText.width(Colors.translate("&cClique aqui para fazer o pagamento via o site."))).isEqualTo(232);
        assertThat(CenteredText.width(Colors.translate("&efinalizar o &6pagamento&e!"))).isEqualTo(110);
    }

    @Test
    void boldAddsOnePixelPerCharacterUntilTheColorChanges() {
        assertThat(CenteredText.width(Colors.translate("&b&lPAGAMENTO"))).isEqualTo(63);
        assertThat(CenteredText.width(Colors.translate("&lAB&aCD"))).isEqualTo(26);
    }

    @Test
    void centeringPadsWithSpacesAroundTheCenterPixel() {
        assertThat(CenteredText.center(Colors.translate("&b&lPAGAMENTO"), CenteredText.DEFAULT_CENTER)).startsWith(" ".repeat(21) + "§b");
        assertThat(CenteredText.center(Colors.translate("&eEscaneie o &aQR Code &eem sua tela para"), CenteredText.DEFAULT_CENTER)).startsWith(" ".repeat(5) + "§e");
        assertThat(CenteredText.center(Colors.translate("&efinalizar o &6pagamento&e!"), CenteredText.DEFAULT_CENTER)).startsWith(" ".repeat(15) + "§e");
    }

    @Test
    void textWiderThanTheLineIsNotPadded() {
        String wide = "W".repeat(60);

        assertThat(CenteredText.center(wide, CenteredText.DEFAULT_CENTER)).isEqualTo(wide);
    }

    @Test
    void markersAreReadInAnyOrder() {
        assertThat(ChatLine.parse("<center>&aOi")).isEqualTo(new ChatLine("&aOi", true, false));
        assertThat(ChatLine.parse("<link><center>Abrir")).isEqualTo(new ChatLine("Abrir", true, true));
        assertThat(ChatLine.parse("Normal")).isEqualTo(new ChatLine("Normal", false, false));
    }

    @Test
    void onlyCenteredLinesAreRenderedWithPadding() {
        assertThat(new ChatLine("Abc", false, true).render(CenteredText.DEFAULT_CENTER)).isEqualTo("Abc");
        assertThat(new ChatLine("Abc", true, false).render(CenteredText.DEFAULT_CENTER)).startsWith(" ").endsWith("Abc");
    }
}
