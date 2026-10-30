package br.com.muxe.minigame.bedwars.cosmetic;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.minigame.bedwars.cosmetic.animation.BedAnimations;
import br.com.muxe.minigame.bedwars.cosmetic.catalog.Availability;
import br.com.muxe.minigame.bedwars.cosmetic.catalog.BedCosmetic;
import br.com.muxe.minigame.bedwars.cosmetic.catalog.CosmeticCatalog;
import br.com.muxe.minigame.bedwars.cosmetic.menu.CosmeticIcons;
import br.com.muxe.minigame.bedwars.cosmetic.menu.CosmeticMenuSettings;
import br.com.muxe.minigame.bedwars.cosmetic.preview.PreviewSettings;
import br.com.muxe.minigame.bedwars.cosmetic.profile.CosmeticState;
import br.com.muxe.minigame.bedwars.cosmetic.profile.CosmeticWallet;
import br.com.muxe.minigame.bedwars.room.RoomFixtures;
import java.time.Duration;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CosmeticCatalogTest {

    private static final BedAnimations ANIMATIONS = new BedAnimations();
    private final CosmeticCatalog catalog = CosmeticCatalog.from(RoomFixtures.CONFIG.getConfigurationSection("cosmetics"), id -> ANIMATIONS.find(id).isPresent());
    private final CosmeticIcons icons = new CosmeticIcons(CosmeticMenuSettings.from(RoomFixtures.CONFIG.getConfigurationSection("cosmetics.menus")), catalog);

    @Test
    void theCatalogListsThirtyCosmeticsAndEveryAnimationHasAnEntry() {
        assertThat(catalog.all()).hasSize(30);
        assertThat(catalog.all().stream().filter(BedCosmetic::animated)).hasSize(13);
        ANIMATIONS.all().forEach(animation -> assertThat(catalog.find(animation.id())).as(animation.id()).isPresent());
        assertThat(catalog.price(catalog.find("tinta").orElseThrow())).isEqualTo(1_750);
        assertThat(catalog.price(catalog.find("sal-na-ferida").orElseThrow())).isEqualTo(750);
    }

    @Test
    void cosmeticsWithoutAnAnimationCannotBeBoughtOrSelected() {
        BedCosmetic tornado = catalog.find("tornado").orElseThrow();
        CosmeticWallet rich = new CosmeticWallet(Set.of("tornado"), CosmeticWallet.RANDOM, 10_000, false);

        assertThat(tornado.forSale()).isFalse();
        assertThat(rich.state(tornado)).isEqualTo(CosmeticState.UNAVAILABLE);
    }

    @Test
    void ultraPlayersOwnTheUltraCosmetics() {
        BedCosmetic fireworks = catalog.find("fogos-de-artificio").orElseThrow();

        assertThat(fireworks.availability()).isEqualTo(Availability.ULTRA);
        assertThat(new CosmeticWallet(Set.of(), CosmeticWallet.RANDOM, 0, true).state(fireworks)).isEqualTo(CosmeticState.OWNED);
        assertThat(new CosmeticWallet(Set.of(), CosmeticWallet.RANDOM, 0, false).state(fireworks)).isEqualTo(CosmeticState.UNAVAILABLE);
    }

    @Test
    void randomPicksAnOwnedAnimationAndNonePicksNothing() {
        CosmeticWallet wallet = new CosmeticWallet(Set.of("tinta", "tornado"), CosmeticWallet.RANDOM, 0, false);

        assertThat(wallet.animation(catalog, new Random(1))).contains("tinta");
        assertThat(wallet.selecting(CosmeticWallet.NONE).animation(catalog, new Random(1))).isEmpty();
        assertThat(wallet.selecting("deserto").animation(catalog, new Random(1))).contains("tinta");
        assertThat(CosmeticWallet.EMPTY.animation(catalog, new Random(1))).isEmpty();
    }

    @Test
    void ownedCosmeticsComeFirstThenTheOnesForSale() {
        CosmeticWallet wallet = new CosmeticWallet(Set.of("enderman"), CosmeticWallet.RANDOM, 0, false);

        assertThat(wallet.sorted(catalog).getFirst().id()).isEqualTo("enderman");
        assertThat(wallet.sorted(catalog).get(1).id()).isEqualTo("colheita-de-aboboras");
        assertThat(wallet.sorted(catalog).getLast().forSale()).isFalse();
    }

    @Test
    void aCosmeticForSaleShowsThePriceAndWhatIsMissing() {
        ItemBuilder icon = icons.cosmetic(catalog.find("apavorante").orElseThrow(), new CosmeticWallet(Set.of(), CosmeticWallet.RANDOM, 15, false));

        assertThat(icon.name()).isEqualTo("§cApavorante");
        assertThat(icon.lore()).containsExactly(
            "§6§l✩✩✩✩§7§l✩",
            " ",
            "§6Halloween 2026",
            " ",
            "§7Preço: §d1.750 créditos",
            "§cFaltam 1.735 créditos!",
            " ",
            "§eBotão esquerdo: §fComprar",
            "§eBotão direito: §fVisualizar"
        );
    }

    @Test
    void theSelectedCosmeticGlowsAndUnavailableOnesOnlyPreview() {
        CosmeticWallet wallet = new CosmeticWallet(Set.of("avalanche"), "avalanche", 0, false);

        ItemBuilder avalanche = icons.cosmetic(catalog.find("avalanche").orElseThrow(), wallet);
        ItemBuilder tubarao = icons.cosmetic(catalog.find("tubarao").orElseThrow(), wallet);

        assertThat(avalanche.isGlowing()).isTrue();
        assertThat(avalanche.lore()).endsWith("§aJá selecionado.", "§eBotão direito: §fVisualizar");
        assertThat(tubarao.lore()).endsWith("§aAniversário 2026", " ", "§cIndisponível.", "§eBotão direito: §fVisualizar");
        assertThat(icons.selectedName(wallet)).isEqualTo("Avalanche");
        assertThat(icons.category(wallet).lore().getFirst()).isEqualTo("§81/30 (3%)");
    }

    @Test
    void randomIsTheDefaultSelection() {
        ItemBuilder random = icons.random(CosmeticWallet.EMPTY);

        assertThat(random.isGlowing()).isTrue();
        assertThat(random.lore()).containsExactly("§7Seleciona um cosmético", "§7aleatório que você possui", " ", "§aJá selecionado.");
        assertThat(icons.none(CosmeticWallet.EMPTY).lore()).containsExactly(" ", "§eBotão esquerdo: §fSelecionar");
        assertThat(icons.selectedName(CosmeticWallet.EMPTY)).isEqualTo("Aleatório");
    }

    @Test
    void thePreviewKnowsTheBedOfThePreviewMap() {
        PreviewSettings preview = PreviewSettings.from(RoomFixtures.CONFIG.getConfigurationSection("preview"));

        assertThat(preview.isBed(6, 11, 17)).isTrue();
        assertThat(preview.isBed(6, 11, 16)).isTrue();
        assertThat(preview.isBed(6, 11, 15)).isFalse();
        assertThat(preview.timeout()).isEqualTo(Duration.ofMinutes(2));
        assertThat(preview.lines("Tinta")).containsExactly("", "&fVisualizando:", "&eTinta", "", "&amuxe.com.br");
    }
}
