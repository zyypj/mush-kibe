package br.com.muxe.minigame.api.customroom;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.customroom.schema.SchemaReader;
import br.com.muxe.core.common.combat.knockback.ClickLimiter;
import br.com.muxe.core.common.combat.knockback.KnockbackProfile;
import br.com.muxe.core.common.customroom.domain.RoomOptions;
import br.com.muxe.core.common.customroom.schema.ModeContext;
import br.com.muxe.core.common.customroom.schema.OptionPage;
import br.com.muxe.core.common.customroom.schema.RoomEditor;
import br.com.muxe.core.common.customroom.schema.RoomLore;
import br.com.muxe.core.common.customroom.schema.RoomSchema;
import br.com.muxe.core.common.customroom.schema.RoomTexts;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class KnockbackPageTest {

    private static final KnockbackProfile BEDWARS = new KnockbackProfile(0.6, 0.75, 0.1, 0.5, 0.4, 2.0, 2.0, 0.6, false, true, false, true, false, ClickLimiter.NORMAL);

    @Test
    void thePageShowsTheModeProfileLikeTheReferenceMenu() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("root", KnockbackPage.ID);
        KnockbackPage.write(yaml.createSection("pages"), BEDWARS, "Alterar knockback", 30, "options");

        RoomSchema schema = SchemaReader.parse("bedwars", yaml.saveToString());
        OptionPage page = schema.page(KnockbackPage.ID).orElseThrow();
        RoomLore lore = new RoomLore(new RoomEditor(schema, new ModeContext("solo", 1, 8, 8)), RoomTexts.defaults());

        assertThat(page.title()).isEqualTo("Alterar knockback");
        assertThat(page.entries()).hasSize(14);
        assertThat(lore.entry(page.at(10).orElseThrow(), RoomOptions.EMPTY).lore()).contains("&7Valor: &a0.6");
        assertThat(lore.entry(page.at(11).orElseThrow(), RoomOptions.EMPTY).lore()).contains("&7Valor: &a0.75");
        assertThat(lore.entry(page.at(15).orElseThrow(), RoomOptions.EMPTY).lore()).contains("&7Valor: &a2.0");
        assertThat(lore.entry(page.at(20).orElseThrow(), RoomOptions.EMPTY).name()).isEqualTo("&cRestaurar impulso");
        assertThat(lore.entry(page.at(21).orElseThrow(), RoomOptions.EMPTY).name()).isEqualTo("&aSimular Y");
        assertThat(lore.entry(page.at(25).orElseThrow(), RoomOptions.EMPTY).lore()).contains("&a- Normal", "&7- Baixo");
    }
}
