package br.com.muxe.core.bukkit.effect;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ShadedLayoutTest {

    private final ShadedLayout layout = new ShadedLayout(PixelFont.standard(), 2);

    @Test
    void theTopRowIsLitTheBottomRowIsShadedAndTheRestIsBody() {
        List<ShadedPixel> eight = layout.layout("8");

        assertThat(eight).filteredOn(pixel -> pixel.row() == PixelFont.HEIGHT - 1).extracting(ShadedPixel::shade).containsOnly(PixelShade.HIGHLIGHT);
        assertThat(eight).filteredOn(pixel -> pixel.row() == 0).extracting(ShadedPixel::shade).containsOnly(PixelShade.SHADOW);
        assertThat(eight).filteredOn(pixel -> pixel.row() > 0 && pixel.row() < PixelFont.HEIGHT - 1).extracting(ShadedPixel::shade).containsOnly(PixelShade.BODY);
    }

    @Test
    void rowsCountFromTheBottom() {
        List<ShadedPixel> seven = layout.layout("7");

        assertThat(seven).filteredOn(pixel -> pixel.row() == PixelFont.HEIGHT - 1).hasSize(PixelFont.WIDTH);
    }

    @Test
    void glyphsAreSeparatedByTheLetterSpacing() {
        List<ShadedPixel> pair = layout.layout("11");

        Set<Integer> columns = pair.stream().map(ShadedPixel::column).collect(Collectors.toSet());
        assertThat(layout.width("11")).isEqualTo(PixelFont.WIDTH * 2 + 2);
        assertThat(columns).allMatch(column -> column < PixelFont.WIDTH || column >= PixelFont.WIDTH + 2);
        assertThat(columns).contains(PixelFont.WIDTH + 2 + 2);
    }

    @Test
    void unknownCharactersLeaveAGap() {
        assertThat(layout.layout("?")).isEmpty();
        assertThat(layout.width("?")).isEqualTo(PixelFont.WIDTH);
    }

    @Test
    void anEmptyTextHasNoWidth() {
        assertThat(layout.width("")).isZero();
    }
}
