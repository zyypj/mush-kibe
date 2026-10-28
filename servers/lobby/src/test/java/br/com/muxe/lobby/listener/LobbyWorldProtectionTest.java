package br.com.muxe.lobby.listener;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.junit.jupiter.api.Test;

class LobbyWorldProtectionTest {

    private final LobbyProtectionListener listener = new LobbyProtectionListener(null, null, 0);

    @Test
    void blocksNeverBreakOrDropBecauseOfPhysics() {
        BlockPhysicsEvent event = new BlockPhysicsEvent(block(Material.SUGAR_CANE_BLOCK), Material.DIRT.getId());

        listener.onPhysics(event);

        assertThat(event.isCancelled()).isTrue();
    }

    @Test
    void leavesNeverDecayAndLiquidsNeverFlow() {
        LeavesDecayEvent decay = new LeavesDecayEvent(block(Material.LEAVES));
        BlockFromToEvent flow = new BlockFromToEvent(block(Material.WATER), block(Material.AIR));

        listener.onLeavesDecay(decay);
        listener.onLiquidFlow(flow);

        assertThat(decay.isCancelled()).isTrue();
        assertThat(flow.isCancelled()).isTrue();
    }

    @Test
    void iceAndSnowNeverMeltButAFireCanGoOut() {
        BlockFadeEvent ice = new BlockFadeEvent(block(Material.ICE), null);
        BlockFadeEvent fire = new BlockFadeEvent(block(Material.FIRE), null);

        listener.onFade(ice);
        listener.onFade(fire);

        assertThat(ice.isCancelled()).isTrue();
        assertThat(fire.isCancelled()).isFalse();
    }

    @Test
    void mobsNeverSpawnInTheLobbyUnlessAPluginSpawnsThem() {
        CreatureSpawnEvent natural = new CreatureSpawnEvent(null, SpawnReason.NATURAL);
        CreatureSpawnEvent egg = new CreatureSpawnEvent(null, SpawnReason.SPAWNER_EGG);
        CreatureSpawnEvent plugin = new CreatureSpawnEvent(null, SpawnReason.CUSTOM);

        listener.onCreatureSpawn(natural);
        listener.onCreatureSpawn(egg);
        listener.onCreatureSpawn(plugin);

        assertThat(natural.isCancelled()).isTrue();
        assertThat(egg.isCancelled()).isTrue();
        assertThat(plugin.isCancelled()).isFalse();
    }

    private static Block block(Material type) {
        return (Block) Proxy.newProxyInstance(Block.class.getClassLoader(), new Class<?>[] {Block.class},
            (proxy, method, args) -> method.getName().equals("getType") ? type : null);
    }
}
