package br.com.muxe.core.common.language.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LanguageTest {

    @Test
    void tagsAndNamesAreRecognisedInAnyCase() {
        assertThat(Language.fromTag("pt-BR")).contains(Language.PORTUGUESE);
        assertThat(Language.fromTag("PT-br")).contains(Language.PORTUGUESE);
        assertThat(Language.fromTag("en")).contains(Language.ENGLISH);
        assertThat(Language.fromTag("english")).contains(Language.ENGLISH);
    }

    @Test
    void playersMayNameALanguageWithOrWithoutAccents() {
        assertThat(Language.named("Português")).contains(Language.PORTUGUESE);
        assertThat(Language.named("portugues")).contains(Language.PORTUGUESE);
        assertThat(Language.named("inglês")).contains(Language.ENGLISH);
        assertThat(Language.named("English")).contains(Language.ENGLISH);
        assertThat(Language.named("en")).contains(Language.ENGLISH);
        assertThat(Language.named("klingon")).isEmpty();
    }

    @Test
    void unknownTagsFallBackToPortuguese() {
        assertThat(Language.fromTag("es")).isEmpty();
        assertThat(Language.fromTag(null)).isEmpty();
        assertThat(Language.parse("es")).isEqualTo(Language.PORTUGUESE);
    }
}
