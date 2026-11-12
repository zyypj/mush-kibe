package br.com.muxe.core.bukkit.scheduledgame;

import br.com.muxe.core.bukkit.text.MessageCatalog;
import java.util.Collection;
import java.util.function.Supplier;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

public final class ScheduledAnnouncer implements Runnable {

    public static final long PERIOD_TICKS = 20L;
    public static final String HOVER = "scheduled-game-hover";

    private final ScheduledGames games;
    private final Supplier<Collection<? extends Player>> online;
    private final MessageCatalog messages;

    public ScheduledAnnouncer(ScheduledGames games, Supplier<Collection<? extends Player>> online, MessageCatalog messages) {
        this.games = games;
        this.online = online;
        this.messages = messages;
    }

    @Override
    public void run() {
        for (ScheduledGame game : games.dueAnnouncements()) {
            for (Player player : online.get()) {
                player.spigot().sendMessage(line(player, game));
            }
        }
    }

    BaseComponent[] line(Player player, ScheduledGame game) {
        BaseComponent[] parts = TextComponent.fromLegacyText(messages.get(player, game.announcement()));
        ClickEvent click = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + game.command());
        HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(messages.get(player, HOVER)));
        for (BaseComponent part : parts) {
            part.setClickEvent(click);
            part.setHoverEvent(hover);
        }
        return parts;
    }
}
