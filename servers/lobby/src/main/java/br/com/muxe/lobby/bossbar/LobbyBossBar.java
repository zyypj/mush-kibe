package br.com.muxe.lobby.bossbar;

import br.com.muxe.core.bukkit.bossbar.BossBarFrames;
import br.com.muxe.core.bukkit.bossbar.BossBars;
import br.com.muxe.core.common.language.domain.LanguageLookup;
import br.com.muxe.core.common.language.domain.Localized;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class LobbyBossBar implements Runnable {

    private final BossBars bars;
    private final Localized<BossBarFrames> frames;
    private final LanguageLookup languages;
    private long tick;

    public LobbyBossBar(BossBars bars, Localized<BossBarFrames> frames, LanguageLookup languages) {
        this.bars = bars;
        this.frames = frames;
        this.languages = languages;
    }

    public long period() {
        return frames.standard().ticksPerFrame();
    }

    public String frame(UUID player) {
        return frames.in(languages.of(player)).at(tick);
    }

    public void show(Player player) {
        bars.show(player, frame(player.getUniqueId()));
    }

    public void advance() {
        tick += period();
    }

    @Override
    public void run() {
        advance();
        for (Player player : Bukkit.getOnlinePlayers()) {
            show(player);
        }
    }
}
