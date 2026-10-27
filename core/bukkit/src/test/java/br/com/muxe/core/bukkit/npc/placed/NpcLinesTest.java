package br.com.muxe.core.bukkit.npc.placed;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.shop.ShopFixtures;
import br.com.muxe.core.common.placement.domain.NpcAction;
import br.com.muxe.core.common.placement.domain.NpcPlacement;
import br.com.muxe.core.common.placement.domain.Position;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class NpcLinesTest {

    private static final Position SPAWN = new Position("lobby", 0, 40, 0, 0, 0);

    private final NpcLines lines = NpcLines.from(ShopFixtures.CONFIG.getConfigurationSection("npcs.lines"));

    @Test
    void gameNpcsShowTheirNameAndHowManyArePlaying() {
        NpcPlacement npc = new NpcPlacement("lobby", "skywars", SPAWN, "SkyWars", NpcAction.game("skywars"), "", Optional.empty());

        assertThat(lines.render(npc, 12)).containsExactly("&b&lSkyWars", "&e12 jogando");
    }

    @Test
    void eachActionHasItsOwnLines() {
        NpcPlacement shop = new NpcPlacement("lobby", "vendedor", SPAWN, "Loja", NpcAction.SHOP, "", Optional.empty());
        NpcPlacement plain = new NpcPlacement("lobby", "guia", SPAWN, "Guia", NpcAction.NONE, "", Optional.empty());

        assertThat(lines.render(shop, 0)).containsExactly("&b&lLoja", "&eClique para abrir a loja");
        assertThat(lines.render(plain, 0)).containsExactly("&b&lGuia");
    }

    @Test
    void missingTemplatesShowNoLines() {
        NpcPlacement npc = new NpcPlacement("lobby", "guia", SPAWN, "Guia", NpcAction.NONE, "", Optional.empty());

        assertThat(NpcLines.from(null).render(npc, 0)).isEmpty();
    }
}
