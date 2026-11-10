package br.com.muxe.core.bukkit.party.command;

import br.com.muxe.core.bukkit.command.framework.annotation.Arg;
import br.com.muxe.core.bukkit.command.framework.annotation.Command;
import br.com.muxe.core.bukkit.command.framework.annotation.Default;
import br.com.muxe.core.bukkit.command.framework.annotation.Suggest;
import br.com.muxe.core.bukkit.command.framework.annotation.Usage;
import br.com.muxe.core.bukkit.party.ChannelChoices;
import br.com.muxe.core.bukkit.party.PartyChat;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.chat.ChatChannel;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Command(name = "canal", description = "Changes your default chat channel.")
public final class ChannelCommand {

    private final ChannelChoices channels;
    private final PartyChat chat;
    private final MessageCatalog messages;

    public ChannelCommand(ChannelChoices channels, PartyChat chat, MessageCatalog messages) {
        this.channels = channels;
        this.chat = chat;
        this.messages = messages;
    }

    @Default
    public void choose(Player player, @Arg("canal") @Suggest({"clan", "default", "party"}) String typed) {
        Optional<ChatChannel> channel = ChatChannel.parse(typed);
        if (channel.isEmpty()) {
            usage(player);
            return;
        }
        switch (channel.get()) {
            case CLAN -> player.sendMessage(messages.get(player, "channel-no-clan"));
            case PARTY -> {
                if (!chat.inParty(player.getUniqueId())) {
                    player.sendMessage(messages.get(player, "party-not-in-party"));
                    return;
                }
                changed(player, ChatChannel.PARTY);
            }
            case DEFAULT -> changed(player, ChatChannel.DEFAULT);
        }
    }

    @Usage
    public void usage(CommandSender sender) {
        sender.sendMessage(messages.get(sender, "channel-usage"));
    }

    private void changed(Player player, ChatChannel channel) {
        channels.choose(player.getUniqueId(), channel);
        String name = messages.get(player, "channel-name-" + channel.id().toLowerCase(Locale.ROOT));
        player.sendMessage(messages.get(player, "channel-changed", "channel", name));
    }
}
