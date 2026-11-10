package br.com.muxe.core.common.language.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

class TextTest {

    @Test
    void aPlainTextIsTheSameInEveryLanguage() {
        Text text = Text.plain("tadeu");

        assertThat(text.in(Language.PORTUGUESE)).isEqualTo("tadeu");
        assertThat(text.in(Language.ENGLISH)).isEqualTo("tadeu");
    }

    @Test
    void aTranslatedTextFollowsTheLanguage() {
        Text text = Text.of("Vermelho", "Red");

        assertThat(text.in(Language.PORTUGUESE)).isEqualTo("Vermelho");
        assertThat(text.in(Language.ENGLISH)).isEqualTo("Red");
    }

    @Test
    void aMissingTranslationFallsBackToPortuguese() {
        Text text = Text.of(Map.of(Language.PORTUGUESE, "Azul"));

        assertThat(text.in(Language.ENGLISH)).isEqualTo("Azul");
        assertThatThrownBy(() -> Text.of(Map.of(Language.ENGLISH, "Blue"))).isInstanceOf(NullPointerException.class);
    }

    @Test
    void renderingKeepsPlainValuesAndTranslatesTexts() {
        assertThat(Text.render(3, Language.ENGLISH)).isEqualTo("3");
        assertThat(Text.render(Text.of("Verde", "Green"), Language.ENGLISH)).isEqualTo("Green");
    }

    @Test
    void localizedValuesAreBuiltOncePerLanguage() {
        Localized<String> names = Localized.of(language -> language == Language.ENGLISH ? "Shop" : "Loja");

        assertThat(names.in(Language.ENGLISH)).isEqualTo("Shop");
        assertThat(names.standard()).isEqualTo("Loja");
        assertThat(names.map(String::length).in(Language.PORTUGUESE)).isEqualTo(4);
        assertThat(Localized.same("x").in(Language.ENGLISH)).isEqualTo("x");
    }
}
