package br.com.muxe.core.bukkit.effect;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ParticleTextTest {

    private final PixelFont font = PixelFont.standard();

    @Test
    void theStandardFontCoversDigitsLettersAndSpace() {
        assertThat(font.supportsAll("ABCDEFGHIJKLMNOPQRSTUVWXYZ 0123456789")).isTrue();
        assertThat(font.supports('a')).isTrue();
        assertThat(font.supports('?')).isFalse();
    }

    @Test
    void everyGlyphHasTheFontSize() {
        for (char character : "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray()) {
            boolean[][] glyph = font.glyph(character).orElseThrow();
            assertThat(glyph).as("rows of " + character).hasNumberOfRows(PixelFont.HEIGHT);
            assertThat(glyph[0]).as("columns of " + character).hasSize(PixelFont.WIDTH);
        }
    }

    @Test
    void layoutIsCenteredHorizontallyAndStartsAtTheBottom() {
        ParticleText text = new ParticleText(font, 1.0, ParticleColor.GREEN);

        List<ParticleText.Pixel> pixels = text.layout("L");

        assertThat(pixels).hasSize(11);
        assertThat(pixels).extracting(ParticleText.Pixel::y).contains(0.0, 6.0);
        assertThat(pixels).extracting(ParticleText.Pixel::x).contains(-2.5, 1.5);
    }

    @Test
    void lettersAreSeparatedBySpacing() {
        ParticleText text = new ParticleText(font, 1.0, ParticleColor.GREEN);

        double widest = text.layout("LL").stream().mapToDouble(ParticleText.Pixel::x).max().orElseThrow();

        assertThat(widest).isEqualTo(4.5);
    }

    @Test
    void unknownCharactersAreSkipped() {
        assertThat(ParticleText.standard(ParticleColor.WHITE).layout("??")).isEmpty();
    }
}
