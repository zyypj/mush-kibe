package br.com.muxe.minigame.bedwars.shop;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.common.customroom.domain.RoomOptions;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.minigame.bedwars.BedWarsPlugin;
import br.com.muxe.minigame.bedwars.custom.CustomRules;
import br.com.muxe.minigame.bedwars.custom.CustomShop;
import br.com.muxe.minigame.bedwars.hotbar.HotbarChoices;
import br.com.muxe.minigame.bedwars.room.RoomPlayer;
import br.com.muxe.minigame.bedwars.shop.ShopCatalog.Tab;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ShopLanguageTest {

    private static final LocalizedConfig TEXTS = LocalizedConfig.bundled(BedWarsPlugin.class, "config.yml");
    private static final Localized<ShopCatalog> SHOPS = TEXTS.map(root -> ShopCatalog.from(root.getConfigurationSection("shop")));
    private static final Localized<UpgradeCatalog> UPGRADES = TEXTS.map(root -> UpgradeCatalog.from(root.getConfigurationSection("upgrades")));

    private final RoomPlayer player = new RoomPlayer(UUID.randomUUID());

    @Test
    void theShopItemIsNamedInTheLanguageOfTheBuyer() {
        Offer portuguese = Offer.of(SHOPS.in(Language.PORTUGUESE).items().get("espada-ferro"), player);
        Offer english = Offer.of(SHOPS.in(Language.ENGLISH).items().get("espada-ferro"), player);

        assertThat(portuguese.name(SHOPS.in(Language.PORTUGUESE).texts(), true)).isEqualTo("&aEspada de Ferro");
        assertThat(english.name(SHOPS.in(Language.ENGLISH).texts(), true)).isEqualTo("&aIron Sword");
        assertThat(english.lore(SHOPS.in(Language.ENGLISH).texts(), true, false, false).getFirst()).isEqualTo("&7Cost: &67 Gold");
    }

    @Test
    void thePermanentMarkIsDroppedFromTheBoughtItemInBothLanguages() {
        Offer portuguese = Offer.of(SHOPS.in(Language.PORTUGUESE).items().get("armadura-diamante"), player);
        Offer english = Offer.of(SHOPS.in(Language.ENGLISH).items().get("armadura-diamante"), player);

        assertThat(portuguese.boughtName(SHOPS.in(Language.PORTUGUESE).texts())).isEqualTo("Armadura de Diamante");
        assertThat(english.boughtName(SHOPS.in(Language.ENGLISH).texts())).isEqualTo("Diamond Armor");
    }

    @Test
    void theTabsAndTheToolTiersAreTranslatedItemByItem() {
        ShopCatalog portuguese = SHOPS.in(Language.PORTUGUESE);
        ShopCatalog english = SHOPS.in(Language.ENGLISH);

        assertThat(english.tabs()).extracting(Tab::name).startsWith("&aQuick Buy", "&aBlocks");
        assertThat(english.tabs()).extracting(Tab::material).isEqualTo(portuguese.tabs().stream().map(Tab::material).toList());
        assertThat(english.tabs()).extracting(Tab::category).isEqualTo(portuguese.tabs().stream().map(Tab::category).toList());
        assertThat(english.items().get("picareta").tiers().getFirst().name()).isEqualTo("Wooden Pickaxe (Efficiency I)");
        assertThat(english.items().get("picareta").tiers().getLast().price()).isEqualTo(portuguese.items().get("picareta").tiers().getLast().price());
        assertThat(english.items().get("machado").tiers().get(2).currency().id()).isEqualTo("gold");
    }

    @Test
    void theUpgradesAreNamedAndPricedInEachReadersLanguage() {
        UpgradeCatalog english = UPGRADES.in(Language.ENGLISH);
        UpgradeCatalog.Entry armor = english.entry("reforco").orElseThrow();

        assertThat(armor.boughtName(2)).isEqualTo("Armor Reinforcement II");
        assertThat(UPGRADES.in(Language.PORTUGUESE).entry("reforco").orElseThrow().boughtName(2)).isEqualTo("Reforço na Armadura II");
        assertThat(armor.tiers()).extracting(UpgradeCatalog.Tier::price).containsExactly(2, 4, 8, 16);
        assertThat(armor.tiers().getFirst().label()).isEqualTo("Tier 1: Protection I, &b2 Diamonds");
    }

    @Test
    void aFreeUpgradeInACustomRoomSaysFreeInTheReadersLanguage() {
        CustomShop free = new CustomShop(CustomRules.of(Optional.of(new RoomOptions(Map.of("upgrades.free", "true")))));
        UpgradeCatalog english = UPGRADES.in(Language.ENGLISH);

        UpgradeCatalog.Entry sharpness = free.upgrade(english.entry("afiacao").orElseThrow(), SHOPS.in(Language.ENGLISH).currencies(), english.texts()).orElseThrow();

        assertThat(sharpness.tiers().getFirst().label()).isEqualTo("Tier 1: Sharpness I, &aFree");
    }

    @Test
    void theHotbarChoicesUseTheShopOfEachLanguage() {
        HotbarChoices english = new HotbarChoices(SHOPS.in(Language.ENGLISH));
        HotbarChoices portuguese = new HotbarChoices(SHOPS.in(Language.PORTUGUESE));

        assertThat(english.category("blocos").orElseThrow().name()).isEqualTo("Blocks");
        assertThat(english.items()).extracting(ShopCatalog.ShopItem::id).isEqualTo(portuguese.items().stream().map(ShopCatalog.ShopItem::id).toList());
    }
}
