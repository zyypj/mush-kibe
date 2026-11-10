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
import br.com.muxe.core.common.language.domain.Language;
import java.util.List;
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

    @Test
    void theTextsComeFromTheGameAndAnEnglishTranslationRidesInTheSchema() {
        YamlConfiguration portuguese = new YamlConfiguration();
        portuguese.set("title", "Alterar knockback");
        YamlConfiguration english = new YamlConfiguration();
        english.set("title", "Change knockback");
        english.set("entries.vertical.name", "Vertical");
        english.set("entries.restore-velocity.name", "Restore velocity");
        english.set("entries.restore-velocity.description", List.of("&7Whether the previous", "&7velocity is restored."));
        english.set("values.low", "Low");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("root", KnockbackPage.ID);
        KnockbackPage.write(yaml.createSection("pages"), BEDWARS, portuguese, 30, "options");
        KnockbackPage.translate(yaml.createSection(SchemaReader.TRANSLATIONS + "." + Language.ENGLISH.tag() + ".pages"), english);

        RoomSchema standard = SchemaReader.parse("bedwars", yaml.saveToString());
        RoomSchema translated = SchemaReader.parse("bedwars", yaml.saveToString(), Language.ENGLISH);
        RoomLore lore = new RoomLore(new RoomEditor(translated, new ModeContext("solo", 1, 8, 8)), RoomTexts.defaults());
        OptionPage page = translated.page(KnockbackPage.ID).orElseThrow();

        assertThat(standard.page(KnockbackPage.ID).orElseThrow().title()).isEqualTo("Alterar knockback");
        assertThat(page.title()).isEqualTo("Change knockback");
        assertThat(page.entries()).hasSize(14);
        assertThat(lore.entry(page.at(20).orElseThrow(), RoomOptions.EMPTY).name()).isEqualTo("&cRestore velocity");
        assertThat(lore.entry(page.at(20).orElseThrow(), RoomOptions.EMPTY).lore()).contains("&7Whether the previous");
        assertThat(lore.entry(page.at(25).orElseThrow(), RoomOptions.EMPTY).lore()).contains("&7- Low");
        assertThat(lore.entry(page.at(10).orElseThrow(), RoomOptions.EMPTY).lore()).contains("&7Valor: &a0.6");
    }
}
