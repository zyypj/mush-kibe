package br.com.muxe.minigame.bedwars.game;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.minigame.bedwars.game.RandomItems.Draw;
import br.com.muxe.minigame.bedwars.game.RandomItems.GameDraw;
import br.com.muxe.minigame.bedwars.game.RandomItems.ShopDraw;
import br.com.muxe.minigame.bedwars.room.RoomFixtures;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class RandomItemsTest {

    private final RandomItems items = RoomFixtures.SETTINGS.randomItems();

    @Test
    void pilaresGetsAnItemEveryFiveSecondsHalfFromTheShop() {
        assertThat(items.in("pilares")).isTrue();
        assertThat(items.in("solo")).isFalse();
        assertThat(items.every()).isEqualTo(Duration.ofSeconds(5));
        assertThat(items.shopChance()).isEqualTo(0.5);
        assertThat(items.maxAmount()).isEqualTo(4);
    }

    @Test
    void thePoolHasCommonItemsButNoAdminBlocksNoBlockFormsAndNoChestOrBed() {
        assertThat(items.pool()).contains(Material.IRON_INGOT, Material.ACACIA_FENCE_GATE, Material.JUKEBOX, Material.CHAINMAIL_BOOTS, Material.NETHER_BRICK)
            .doesNotContain(Material.BEDROCK, Material.BARRIER, Material.COMMAND, Material.COMMAND_MINECART, Material.MOB_SPAWNER, Material.MONSTER_EGG,
                Material.MONSTER_EGGS, Material.ENDER_PORTAL_FRAME, Material.ENDER_PORTAL, Material.PORTAL, Material.DRAGON_EGG, Material.CHEST, Material.BED,
                Material.AIR, Material.BED_BLOCK, Material.WATER, Material.WOODEN_DOOR);
    }

    @Test
    void aGameItemComesOneToFourAtATimeAndAlwaysAloneWhenItDoesNotStack() {
        Random random = new Random(7);
        List<GameDraw> draws = IntStream.range(0, 2000).mapToObj(index -> items.game(random)).toList();

        assertThat(draws).allSatisfy(draw -> {
            assertThat(draw.amount()).isBetween(1, Math.min(4, draw.material().getMaxStackSize()));
        });
        assertThat(draws).filteredOn(draw -> draw.material().getMaxStackSize() == 1).allSatisfy(draw -> assertThat(draw.amount()).isEqualTo(1));
        assertThat(draws).extracting(GameDraw::amount).contains(1, 2, 3, 4);
    }

    @Test
    void aboutHalfTheDrawsAreShopItemsFromTheGivenList() {
        Random random = new Random(11);
        List<Draw> draws = IntStream.range(0, 2000).mapToObj(index -> items.draw(random, List.of("la", "tnt", "torre-compacta"))).toList();

        long shop = draws.stream().filter(ShopDraw.class::isInstance).count();
        assertThat(shop).isBetween(900L, 1100L);
        assertThat(draws).filteredOn(ShopDraw.class::isInstance).extracting(draw -> ((ShopDraw) draw).itemId()).containsOnly("la", "tnt", "torre-compacta");
        assertThat(items.draw(random, List.of())).isInstanceOf(GameDraw.class);
    }
}
