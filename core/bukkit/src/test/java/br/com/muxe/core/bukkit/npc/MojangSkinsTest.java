package br.com.muxe.core.bukkit.npc;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.npc.skin.MojangSkins;
import org.junit.jupiter.api.Test;

class MojangSkinsTest {

    @Test
    void readsTheIdOfAName() {
        assertThat(MojangSkins.parseId("{\"id\":\"069a79f444e94726a5befca90e38aaf5\",\"name\":\"Notch\"}")).contains("069a79f444e94726a5befca90e38aaf5");
        assertThat(MojangSkins.parseId("")).isEmpty();
        assertThat(MojangSkins.parseId("{\"errorMessage\":\"not found\"}")).isEmpty();
    }

    @Test
    void readsTheSignedTextures() {
        String profile = "{\"id\":\"x\",\"name\":\"Notch\",\"properties\":[{\"name\":\"textures\",\"value\":\"dmFsdWU=\",\"signature\":\"c2ln\"}]}";

        assertThat(MojangSkins.parseTextures(profile)).contains(new NpcSkin("dmFsdWU=", "c2ln"));
    }

    @Test
    void unsignedTexturesAreRefused() {
        String profile = "{\"properties\":[{\"name\":\"textures\",\"value\":\"dmFsdWU=\"}]}";

        assertThat(MojangSkins.parseTextures(profile)).isEmpty();
        assertThat(MojangSkins.parseTextures("not json")).isEmpty();
    }
}
