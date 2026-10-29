package br.com.muxe.lobby.bossbar;

import br.com.muxe.core.bukkit.bossbar.BossBarFrames;
import br.com.muxe.core.bukkit.bossbar.BossBars;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class LobbyBossBar implements Runnable {

    private final BossBars bars;
    private final BossBarFrames frames;
    private long tick;

    public LobbyBossBar(BossBars bars, BossBarFrames frames) {
        this.bars = bars;
        this.frames = frames;
    }

    public long period() {
        return frames.ticksPerFrame();
    }

    public void show(Player player) {
        bars.show(player, frames.at(tick));
    }

    @Override
    public void run() {
        tick += frames.ticksPerFrame();
        String frame = frames.at(tick);
        for (Player player : Bukkit.getOnlinePlayers()) {
            bars.show(player, frame);
        }
    }
}
