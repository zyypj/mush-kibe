package br.com.muxe.core.bukkit.effect;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PixelFontTest {

    private final PixelFont font = PixelFont.standard();

    @Test
    void everyDigitIsDrawnAndLooksDifferent() {
        Set<String> shapes = new HashSet<>();
        for (char digit = '0'; digit <= '9'; digit++) {
            boolean[][] glyph = font.glyph(digit).orElseThrow();
            shapes.add(Arrays.deepToString(glyph));
        }

        assertThat(shapes).hasSize(10);
    }

    @Test
    void digitsTouchTheTopAndTheBottomRow() {
        for (char digit = '0'; digit <= '9'; digit++) {
            boolean[][] glyph = font.glyph(digit).orElseThrow();
            assertThat(contains(glyph[0])).as("top of %s", digit).isTrue();
            assertThat(contains(glyph[PixelFont.HEIGHT - 1])).as("bottom of %s", digit).isTrue();
        }
    }

    @Test
    void zeroHasNoSlashInside() {
        boolean[][] zero = font.glyph('0').orElseThrow();

        for (int row = 1; row < PixelFont.HEIGHT - 1; row++) {
            for (int column = 1; column < PixelFont.WIDTH - 1; column++) {
                assertThat(zero[row][column]).as("row %s column %s", row, column).isFalse();
            }
        }
    }

    private static boolean contains(boolean[] row) {
        for (boolean pixel : row) {
            if (pixel) {
                return true;
            }
        }
        return false;
    }
}
