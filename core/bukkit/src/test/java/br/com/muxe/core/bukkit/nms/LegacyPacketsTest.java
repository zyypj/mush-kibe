package br.com.muxe.core.bukkit.nms;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class LegacyPacketsTest {

    @Test
    void actionBarsKeepTheColorCodesInsideTheText() {
        JsonObject json = new JsonParser().parse(LegacyPackets.actionBarJson("§eDigite o código")).getAsJsonObject();

        assertThat(json.get("text").getAsString()).isEqualTo("§eDigite o código");
        assertThat(json.has("extra")).isFalse();
        assertThat(json.has("color")).isFalse();
    }

    @Test
    void anEmptyActionBarIsEmptyText() {
        JsonObject json = new JsonParser().parse(LegacyPackets.actionBarJson(null)).getAsJsonObject();

        assertThat(json.get("text").getAsString()).isEmpty();
    }
}
