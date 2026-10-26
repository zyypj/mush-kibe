package br.com.muxe.core.bukkit.item;

import static org.assertj.core.api.Assertions.assertThat;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class SkullTextureTest {

    private static final String HASH = "d545b0ddd82cc4fd2eac64e445bee91b4706408514f71594d18c4b90a5491d74";
    private static final String VALUE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDU0NWIwZGRkODJjYzRmZDJlYWM2NGU0NDViZWU5MWI0NzA2NDA4NTE0ZjcxNTk0ZDE4YzRiOTBhNTQ5MWQ3NCJ9fX0=";

    @Test
    void aTextureHashBecomesTheEncodedValue() {
        assertThat(SkullTexture.value(HASH)).isEqualTo(VALUE);
        assertThat(SkullTexture.value(HASH.toUpperCase())).isEqualTo(VALUE);
    }

    @Test
    void aTextureUrlBecomesTheEncodedValue() {
        assertThat(SkullTexture.value(SkullTexture.TEXTURE_URL + HASH)).isEqualTo(VALUE);
    }

    @Test
    void encodedValuesAreKeptAsTheyAre() {
        assertThat(SkullTexture.value(VALUE)).isEqualTo(VALUE);
        assertThat(SkullTexture.value("  ")).isNull();
        assertThat(SkullTexture.value(null)).isNull();
    }

    @Test
    void theItemBuilderAcceptsHashes() {
        assertThat(ItemBuilder.skull().skullTexture(HASH).skullTexture()).isEqualTo(VALUE);
        assertThat(ItemBuilder.of(Material.SKULL_ITEM, 3).skullTexture()).isNull();
    }
}
