package br.com.muxe.core.bukkit.credit;

import br.com.muxe.core.bukkit.api.MuxeCore;
import java.time.Duration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public final class CreditBalancesListener implements Listener {

    private static final long TICKS_PER_SECOND = 20;

    private final CreditBalances balances;

    private CreditBalancesListener(CreditBalances balances) {
        this.balances = balances;
    }

    public static CreditBalances start(Plugin plugin, MuxeCore core, Duration refresh) {
        CreditBalances balances = new CreditBalances(core.queries(), core.mainThread());
        plugin.getServer().getPluginManager().registerEvents(new CreditBalancesListener(balances), plugin);
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            balances.track(online.getUniqueId());
        }
        long period = Math.max(TICKS_PER_SECOND, refresh.toSeconds() * TICKS_PER_SECOND);
        plugin.getServer().getScheduler().runTaskTimer(plugin, balances::refreshAll, period, period);
        return balances;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        balances.track(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        balances.forget(event.getPlayer().getUniqueId());
    }
}
