package br.com.muxe.core.bukkit.combat.knockback;

import br.com.muxe.core.common.combat.knockback.KnockbackHit;
import br.com.muxe.core.common.combat.knockback.KnockbackState;
import br.com.muxe.core.common.combat.knockback.Motion;
import net.minecraft.server.v1_8_R3.EnchantmentManager;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.MinecraftServer;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerVelocityEvent;

public final class KnockbackListener implements Listener {

    private final KnockbackService service;
    private final KnockbackHit hit = new KnockbackHit();

    public KnockbackListener(KnockbackService service) {
        this.service = service;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getCause() != DamageCause.ENTITY_ATTACK || !(event.getEntity() instanceof Player victim) || !(event.getDamager() instanceof Player attacker)) {
            return;
        }
        KnockbackService.Slot slot = service.slot(victim.getUniqueId());
        if (slot == null) {
            return;
        }
        EntityPlayer target = ((CraftPlayer) victim).getHandle();
        EntityPlayer striker = ((CraftPlayer) attacker).getHandle();
        boolean recovering = target.noDamageTicks > target.maxNoDamageTicks / 2.0F;
        hit.at(MinecraftServer.currentTick, target.ping)
            .victim(target.onGround, recovering, target.motX, target.motY, target.motZ)
            .attacker(striker.locX - target.locX, striker.locZ - target.locZ, striker.yaw, EnchantmentManager.a(striker), striker.isSprinting());
        KnockbackState state = slot.state();
        state.prepare(hit, service.profiles().ticks(state.profile().clickLimiter()), attacker);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVelocity(PlayerVelocityEvent event) {
        KnockbackService.Slot slot = service.slot(event.getPlayer().getUniqueId());
        int tick = MinecraftServer.currentTick;
        if (slot == null || !slot.state().isPending(tick)) {
            return;
        }
        KnockbackState state = slot.state();
        Player attacker = state.pendingAttacker() instanceof Player player ? player : null;
        boolean sprinted = state.attackerWasSprinting();
        boolean keepSprint = state.profile().keepSprint();
        if (state.pushes()) {
            Motion motion = state.pending();
            event.setVelocity(slot.vector().setX(motion.x()).setY(motion.y()).setZ(motion.z()));
            state.applied(tick);
        } else {
            state.discard();
            event.setCancelled(true);
        }
        if (attacker != null) {
            afterAttack(attacker, tick, sprinted && keepSprint);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        service.clear(event.getPlayer().getUniqueId());
    }

    private void afterAttack(Player attacker, int tick, boolean keepSprint) {
        EntityPlayer striker = ((CraftPlayer) attacker).getHandle();
        KnockbackService.Slot slot = service.slot(attacker.getUniqueId());
        if (slot != null) {
            slot.state().attacked(tick, striker.onGround);
        }
        if (keepSprint && !striker.isSprinting()) {
            striker.setSprinting(true);
        }
    }
}
