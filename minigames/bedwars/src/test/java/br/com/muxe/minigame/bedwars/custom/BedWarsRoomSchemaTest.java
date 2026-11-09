package br.com.muxe.minigame.bedwars.custom;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.customroom.schema.SchemaReader;
import br.com.muxe.core.common.combat.knockback.KnockbackProfile;
import br.com.muxe.core.common.customroom.domain.RoomOptions;
import br.com.muxe.core.common.customroom.schema.ModeContext;
import br.com.muxe.core.common.customroom.schema.OptionEntry;
import br.com.muxe.core.common.customroom.schema.OptionPage;
import br.com.muxe.core.common.customroom.schema.RoomEditor;
import br.com.muxe.core.common.customroom.schema.RoomLore;
import br.com.muxe.core.common.customroom.schema.RoomSchema;
import br.com.muxe.core.common.customroom.schema.RoomTexts;
import br.com.muxe.minigame.bedwars.room.RoomFixtures;
import br.com.muxe.minigame.bedwars.shop.ShopCatalog;
import br.com.muxe.minigame.bedwars.shop.UpgradeCatalog;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class BedWarsRoomSchemaTest {

    private static final ShopCatalog SHOP = ShopCatalog.from(RoomFixtures.CONFIG.getConfigurationSection("shop"));
    private static final UpgradeCatalog UPGRADES = UpgradeCatalog.from(RoomFixtures.CONFIG.getConfigurationSection("upgrades"));
    private static final RoomSchema SCHEMA = SchemaReader.parse("bedwars", BedWarsRoomSchema.build(RoomFixtures.CONFIG.getConfigurationSection("custom-room"),
        SHOP, UPGRADES, RoomFixtures.SETTINGS.timeline(), KnockbackProfile.VANILLA));
    private static final RoomLore LORE = new RoomLore(new RoomEditor(SCHEMA, new ModeContext("solo", 1, 8, 8)), RoomTexts.defaults());

    @Test
    void theOptionsPageFollowsTheReference() {
        OptionPage options = SCHEMA.page("options").orElseThrow();

        assertThat(options.title()).isEqualTo("Opções");
        assertThat(options.entries()).extracting(OptionEntry::slot).containsExactly(10, 11, 12, 13, 14, 15, 16, 19, 20);
        assertThat(LORE.entry(options.at(10).orElseThrow(), RoomOptions.EMPTY).lore()).contains("&7Valor: &a1");
        assertThat(SCHEMA.currencies()).containsKeys("iron", "gold", "emerald", "diamond");
    }

    @Test
    void theShopIsSplitIntoPagesWithEveryTierAndTheFreeButton() {
        OptionPage first = SCHEMA.page("shop-1").orElseThrow();
        OptionEntry.ShopItem wool = (OptionEntry.ShopItem) first.at(10).orElseThrow();

        assertThat(first.title()).startsWith("Itens da loja (1/");
        assertThat(wool.key()).isEqualTo("la");
        assertThat(wool.icon().amount()).isEqualTo(16);
        assertThat(LORE.entry(wool, RoomOptions.EMPTY).lore()).contains("&7Preço: &f4 Ferros");
        assertThat(first.at(44).orElseThrow()).isInstanceOf(OptionEntry.FreeAll.class);
        assertThat(first.next()).isPresent();
        assertThat(BedWarsRoomSchema.shopEntries(SHOP)).extracting(BedWarsRoomSchema.Entry::key).contains("picareta-1", "picareta-5", "machado-4", "estilingue");
    }

    @Test
    void upgradesShowTheLevelNamesWithoutThePrices() {
        OptionEntry.Upgrade armor = (OptionEntry.Upgrade) SCHEMA.page("upgrades").orElseThrow().at(11).orElseThrow();

        assertThat(armor.levels()).extracting(OptionEntry.Level::label).containsExactly("Proteção I", "Proteção II", "Proteção III", "Proteção IV");
        assertThat(BedWarsRoomSchema.levelLabel("Nível 1: Fadiga I por 7 segundos, &b1")).isEqualTo("Fadiga I por 7 segundos");
        assertThat(BedWarsRoomSchema.levelLabel("")).isEmpty();
    }

    @Test
    void eventsStartAtTheIntervalsOfTheMatch() {
        OptionEntry.Timer diamond = (OptionEntry.Timer) SCHEMA.page("events").orElseThrow().at(10).orElseThrow();

        assertThat(LORE.entry(diamond, RoomOptions.EMPTY).lore()).contains("&7Tempo: &f6:00");
    }

    @Test
    void customPricesAndLimitsReachTheShop() {
        CustomShop shop = new CustomShop(CustomRules.of(Optional.of(new RoomOptions(Map.of("shop.la.price", "2", "shop.la.currency", "emerald",
            "shop.la.limit-type", "team", "shop.la.limit", "1", "shop.tnt.enabled", "false")))), SHOP.currencies());
        ShopCatalog.ShopItem wool = SHOP.items().get("la");

        ShopCatalog.Tier tier = shop.tier(wool, wool.tiers().getFirst()).orElseThrow();

        assertThat(tier.price()).isEqualTo(2);
        assertThat(tier.currency().id()).isEqualTo("emerald");
        assertThat(shop.tier(SHOP.items().get("tnt"), SHOP.items().get("tnt").tiers().getFirst())).isEmpty();
        assertThat(shop.allows(wool, tier, "vermelho", "a", 0)).isTrue();
        shop.bought(wool, tier, "vermelho", "a");
        assertThat(shop.allows(wool, tier, "vermelho", "b", 0)).isFalse();
        assertThat(shop.allows(wool, tier, "azul", "c", 0)).isTrue();
    }

    @Test
    void customUpgradePricesRewriteTheLevelLines() {
        CustomShop shop = new CustomShop(CustomRules.of(Optional.of(new RoomOptions(Map.of("upgrades.reforco.1.price", "3", "upgrades.reforco.1.currency", "iron",
            "upgrades.pressa.enabled", "false")))), SHOP.currencies());
        UpgradeCatalog.Entry armor = UPGRADES.entries().stream().filter(entry -> entry.id().equals("reforco")).findFirst().orElseThrow();
        UpgradeCatalog.Entry haste = UPGRADES.entries().stream().filter(entry -> entry.id().equals("pressa")).findFirst().orElseThrow();

        UpgradeCatalog.Entry priced = shop.upgrade(armor).orElseThrow();

        assertThat(priced.nextPrice(0)).isEqualTo(3);
        assertThat(priced.currency(0)).isEqualTo(Material.IRON_INGOT);
        assertThat(priced.tiers().getFirst().label()).isEqualTo("Nível 1: Proteção I, &f3 Ferros");
        assertThat(shop.upgrade(haste)).isEmpty();
    }
}
