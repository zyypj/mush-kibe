package br.com.muxe.minigame.bedwars.custom;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.config.DefaultedSections;
import br.com.muxe.core.bukkit.customroom.schema.SchemaReader;
import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.common.combat.knockback.KnockbackProfile;
import br.com.muxe.core.common.customroom.schema.OptionEntry;
import br.com.muxe.core.common.customroom.schema.RoomSchema;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.minigame.bedwars.BedWarsPlugin;
import br.com.muxe.minigame.bedwars.settings.BedWarsSettings;
import br.com.muxe.minigame.bedwars.shop.ShopCatalog;
import br.com.muxe.minigame.bedwars.shop.UpgradeCatalog;
import org.junit.jupiter.api.Test;

class RoomSchemaLanguageTest {

    private static final LocalizedConfig TEXTS = LocalizedConfig.bundled(BedWarsPlugin.class, "config.yml");
    private static final String YAML = BedWarsRoomSchema.build(TEXTS.map(root -> new BedWarsRoomSchema.Texts(DefaultedSections.of(root, "custom-room"),
            ShopCatalog.from(root.getConfigurationSection("shop")), UpgradeCatalog.from(root.getConfigurationSection("upgrades")))),
        BedWarsSettings.from(TEXTS.standard()).timeline(), KnockbackProfile.VANILLA);
    private static final RoomSchema PORTUGUESE = SchemaReader.parse("bedwars", YAML, Language.PORTUGUESE);
    private static final RoomSchema ENGLISH = SchemaReader.parse("bedwars", YAML, Language.ENGLISH);

    @Test
    void thePagesAreTitledInTheReadersLanguage() {
        assertThat(PORTUGUESE.page("options").orElseThrow().title()).isEqualTo("Opções");
        assertThat(ENGLISH.page("options").orElseThrow().title()).isEqualTo("Options");
        assertThat(PORTUGUESE.page("shop-1").orElseThrow().title()).startsWith("Itens da loja (1/");
        assertThat(ENGLISH.page("shop-1").orElseThrow().title()).startsWith("Shop items (1/");
        assertThat(ENGLISH.page("knockback").orElseThrow().title()).isEqualTo("Change knockback");
        assertThat(PORTUGUESE.page("knockback").orElseThrow().title()).isEqualTo("Alterar knockback");
    }

    @Test
    void theShopItemsUpgradesAndKnockbackAreNamedInTheReadersLanguage() {
        OptionEntry.ShopItem wool = (OptionEntry.ShopItem) ENGLISH.page("shop-1").orElseThrow().at(10).orElseThrow();
        OptionEntry.Upgrade armor = (OptionEntry.Upgrade) ENGLISH.page("upgrades").orElseThrow().at(11).orElseThrow();
        OptionEntry.Decimal vertical = (OptionEntry.Decimal) ENGLISH.page("knockback").orElseThrow().at(10).orElseThrow();
        OptionEntry.Choice limiter = (OptionEntry.Choice) ENGLISH.page("knockback").orElseThrow().at(25).orElseThrow();

        assertThat(wool.name()).isEqualTo("Wool");
        assertThat(wool.key()).isEqualTo("la");
        assertThat(armor.name()).isEqualTo("Armor Reinforcement");
        assertThat(armor.levels()).extracting(OptionEntry.Level::label).containsExactly("Protection I", "Protection II", "Protection III", "Protection IV");
        assertThat(vertical.description()).containsExactly("&7Base value of the vertical", "&7knockback.");
        assertThat(limiter.values()).extracting(OptionEntry.ChoiceValue::label).contains("Off", "High");
    }

    @Test
    void onlyTheTextsChangeBetweenTheLanguages() {
        OptionEntry.ShopItem portuguese = (OptionEntry.ShopItem) PORTUGUESE.page("shop-1").orElseThrow().at(10).orElseThrow();
        OptionEntry.ShopItem english = (OptionEntry.ShopItem) ENGLISH.page("shop-1").orElseThrow().at(10).orElseThrow();

        assertThat(portuguese.name()).isEqualTo("Lã");
        assertThat(english.icon()).isEqualTo(portuguese.icon());
        assertThat(english.price()).isEqualTo(portuguese.price());
        assertThat(ENGLISH.currency("diamond").orElseThrow().many()).isEqualTo("Diamonds");
        assertThat(PORTUGUESE.currency("diamond").orElseThrow().many()).isEqualTo("Diamantes");
        assertThat(ENGLISH.pages().keySet()).isEqualTo(PORTUGUESE.pages().keySet());
    }
}
