package br.com.muxe.minigame.bedwars.custom;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.customroom.schema.SchemaReader;
import br.com.muxe.core.common.combat.knockback.KnockbackProfile;
import br.com.muxe.core.common.customroom.domain.RoomOptions;
import br.com.muxe.core.common.customroom.schema.OptionEntry;
import br.com.muxe.core.common.customroom.schema.RoomSchema;
import br.com.muxe.minigame.bedwars.room.RoomFixtures;
import br.com.muxe.minigame.bedwars.shop.ShopCatalog;
import br.com.muxe.minigame.bedwars.shop.UpgradeCatalog;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CustomRoomExtrasTest {

    private static final ShopCatalog SHOP = ShopCatalog.from(RoomFixtures.CONFIG.getConfigurationSection("shop"));
    private static final UpgradeCatalog UPGRADES = UpgradeCatalog.from(RoomFixtures.CONFIG.getConfigurationSection("upgrades"));
    private static final UpgradeCatalog.Entry BED_PROTECTION = UPGRADES.customEntries().getFirst();
    private static final UpgradeCatalog.Entry FATIGUE = UPGRADES.entries().stream().filter(entry -> entry.id().equals("fadiga")).findFirst().orElseThrow();

    @Test
    void bedProtectionOnlyExistsInCustomRoomsAndStartsOff() {
        CustomShop ordinary = new CustomShop(CustomRules.NONE);
        CustomShop untouched = new CustomShop(CustomRules.of(Optional.of(RoomOptions.EMPTY)));
        CustomShop turnedOn = new CustomShop(CustomRules.of(Optional.of(new RoomOptions(Map.of("upgrades.protecao-cama.enabled", "true")))));

        assertThat(UPGRADES.entries()).extracting(UpgradeCatalog.Entry::id).doesNotContain("protecao-cama");
        assertThat(BED_PROTECTION.slot()).isEqualTo(19);
        assertThat(ordinary.upgrade(BED_PROTECTION, SHOP.currencies(), UPGRADES.texts())).isEmpty();
        assertThat(untouched.upgrade(BED_PROTECTION, SHOP.currencies(), UPGRADES.texts())).isEmpty();
        assertThat(turnedOn.upgrade(BED_PROTECTION, SHOP.currencies(), UPGRADES.texts())).isPresent();
    }

    @Test
    void freeLevelsWrapLikeTheReference() {
        CustomShop free = new CustomShop(CustomRules.of(Optional.of(new RoomOptions(Map.of("upgrades.free", "true", "upgrades.protecao-cama.enabled", "true")))));

        UpgradeCatalog.Entry bed = free.upgrade(BED_PROTECTION, SHOP.currencies(), UPGRADES.texts()).orElseThrow();
        UpgradeCatalog.Entry fatigue = free.upgrade(FATIGUE, SHOP.currencies(), UPGRADES.texts()).orElseThrow();

        assertThat(bed.tiers().get(0).label()).isEqualTo("Nível 1: Madeira, Lã, Vidro, &aGrátis");
        assertThat(bed.tiers().get(0).extra()).isNull();
        assertThat(bed.tiers().get(1).label()).isEqualTo("Nível 2: Madeira, Lã, Pedra do Fim,");
        assertThat(bed.tiers().get(1).extra()).isEqualTo("&7&aGrátis");
        assertThat(bed.tiers().get(2).label()).isEqualTo("Nível 3: Madeira, Lã, Pedra do Fim,");
        assertThat(bed.tiers().get(2).extra()).isEqualTo("&7Vidro, &aGrátis");
        assertThat(fatigue.tiers().get(0).label()).isEqualTo("Nível 1: Fadiga I por 7 segundos, &aGrátis");
        assertThat(fatigue.tiers().get(1).extra()).isEqualTo("&7&aGrátis");
    }

    @Test
    void theNewShopItemsAreListedOffAndOnlyOnInCustomRooms() {
        ShopCatalog.ShopItem swap = SHOP.items().get("troca-congelante");
        ShopCatalog.ShopItem obsidian = SHOP.items().get("obsidian");
        CustomShop ordinary = new CustomShop(CustomRules.NONE);
        CustomShop on = new CustomShop(CustomRules.of(Optional.of(new RoomOptions(Map.of("shop.troca-congelante.enabled", "true")))));

        assertThat(SHOP.categories().get("utilidades").items()).doesNotContain("troca-congelante");
        assertThat(SHOP.categories().get("utilidades").customItems()).containsExactly("troca-congelante", "ressurreicao");
        assertThat(SHOP.categories().get("rotativos").items()).isEmpty();
        assertThat(SHOP.categories().get("rotativos").customItems()).containsExactly("ponte-de-gelo", "para-quedas", "rebobinar", "spider", "loja-portatil",
            "bau-do-fim");
        assertThat(ordinary.tier(swap, swap.tiers().getFirst(), SHOP.currencies())).isEmpty();
        assertThat(ordinary.tier(obsidian, obsidian.tiers().getFirst(), SHOP.currencies())).isEmpty();
        assertThat(on.tier(swap, swap.tiers().getFirst(), SHOP.currencies())).isPresent();
    }

    @Test
    void theSchemaShowsTheNewEntriesWithTheirLevelsAndStatus() {
        RoomSchema schema = SchemaReader.parse("bedwars", BedWarsRoomSchema.build(RoomFixtures.CONFIG.getConfigurationSection("custom-room"), SHOP, UPGRADES,
            RoomFixtures.SETTINGS.timeline(), KnockbackProfile.VANILLA));

        OptionEntry.Upgrade bed = (OptionEntry.Upgrade) schema.page("upgrades").orElseThrow().at(19).orElseThrow();
        assertThat(bed.enabled()).isFalse();
        assertThat(bed.levels()).extracting(OptionEntry.Level::label)
            .containsExactly("Madeira, Lã, Vidro", "Madeira, Lã, Pedra do Fim", "Madeira, Lã, Pedra do Fim, Vidro");
        assertThat(schema.entries(OptionEntry.ShopItem.class)).filteredOn(item -> item.key().equals("ressurreicao")).singleElement()
            .satisfies(item -> assertThat(item.enabled()).isFalse());
        assertThat(schema.entries(OptionEntry.ShopItem.class)).filteredOn(item -> item.key().equals("la")).singleElement()
            .satisfies(item -> assertThat(item.enabled()).isTrue());
    }
}
