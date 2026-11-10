package br.com.muxe.core.bukkit.party.command;

import br.com.muxe.core.bukkit.command.framework.annotation.Arg;
import br.com.muxe.core.bukkit.command.framework.annotation.Command;
import br.com.muxe.core.bukkit.command.framework.annotation.Default;
import br.com.muxe.core.bukkit.command.framework.annotation.Greedy;
import br.com.muxe.core.bukkit.command.framework.annotation.Usage;
import br.com.muxe.core.bukkit.party.PartyChat;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Command(name = "pc", description = "Talks in the party chat.")
public final class PartyChatCommand {

    private final PartyChat chat;
    private final MessageCatalog messages;

    public PartyChatCommand(PartyChat chat, MessageCatalog messages) {
        this.chat = chat;
        this.messages = messages;
    }

    @Default
    public void talk(Player player, @Arg("mensagem") @Greedy String message) {
        if (!chat.inParty(player.getUniqueId())) {
            player.sendMessage(messages.get(player, "party-not-in-party"));
            return;
        }
        chat.send(player, message);
    }

    @Usage
    public void usage(CommandSender sender) {
        sender.sendMessage(messages.get(sender, "party-chat-usage"));
    }
}
