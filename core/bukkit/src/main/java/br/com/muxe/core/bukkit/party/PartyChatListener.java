package br.com.muxe.core.bukkit.party;

import br.com.muxe.core.common.chat.ChatChannel;
import java.util.UUID;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PartyChatListener implements Listener {

    private final ChannelChoices channels;
    private final PartyChat chat;

    public PartyChatListener(ChannelChoices channels, PartyChat chat) {
        this.channels = channels;
        this.chat = chat;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        if (channels.of(id) != ChatChannel.PARTY || !chat.inParty(id)) {
            return;
        }
        event.setCancelled(true);
        chat.send(event.getPlayer(), event.getMessage());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        channels.load(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        channels.forget(event.getPlayer().getUniqueId());
    }
}
