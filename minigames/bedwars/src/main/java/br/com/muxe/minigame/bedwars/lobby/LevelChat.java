package br.com.muxe.minigame.bedwars.lobby;

import br.com.muxe.core.bukkit.stats.LevelBadges;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.common.stats.domain.BedWarsLevels;
import br.com.muxe.minigame.bedwars.preferences.PlayerPreferences;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public final class LevelChat implements Listener {

    private final StatsCache stats;
    private final LevelBadges badges;
    private final PlayerPreferences preferences;

    public LevelChat(StatsCache stats, LevelBadges badges, PlayerPreferences preferences) {
        this.stats = stats;
        this.badges = badges;
        this.preferences = preferences;
    }

    public static String badge(LevelBadges badges, int level, boolean shown) {
        return badges.of(shown ? level : 0);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        int level = BedWarsLevels.progress(Math.max(0, stats.of(event.getPlayer().getUniqueId()).general().xp())).level();
        String badge = badge(badges, level, preferences.of(event.getPlayer().getUniqueId()).showLevel());
        event.setFormat(Colors.translate(badge) + " " + event.getFormat());
    }
}
