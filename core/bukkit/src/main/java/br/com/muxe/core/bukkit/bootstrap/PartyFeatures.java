package br.com.muxe.core.bukkit.bootstrap;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.command.framework.Commands;
import br.com.muxe.core.bukkit.party.ChannelChoices;
import br.com.muxe.core.bukkit.party.NetworkPlayers;
import br.com.muxe.core.bukkit.party.PartyChat;
import br.com.muxe.core.bukkit.party.PartyChatListener;
import br.com.muxe.core.bukkit.party.PartyClient;
import br.com.muxe.core.bukkit.party.PartyPresenceListener;
import br.com.muxe.core.bukkit.party.PartyTexts;
import br.com.muxe.core.bukkit.party.command.ChannelCommand;
import br.com.muxe.core.bukkit.party.command.PartyChatCommand;
import br.com.muxe.core.bukkit.party.command.PartyCommand;
import br.com.muxe.core.bukkit.permission.PermissionService;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.chat.ChatChannels;
import br.com.muxe.core.common.runtime.CoreRuntime;
import br.com.muxe.core.common.server.ServerKind;
import java.util.Optional;
import java.util.concurrent.Executor;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

final class PartyFeatures {

    private static final String CHAT_FORMAT = "messages.chat-format";
    private static final String DEFAULT_CHAT_FORMAT = "{medal}{prefix}{color}{player}: &f{message}";

    private PartyFeatures() {
    }

    static PartyClient client(JavaPlugin plugin, CoreRuntime runtime, Executor mainThread, MessageCatalog messages) {
        Server server = plugin.getServer();
        String chatFormat = plugin.getConfig().getString(CHAT_FORMAT, DEFAULT_CHAT_FORMAT);
        NetworkPlayers networkPlayers = new NetworkPlayers(() -> server.getOnlinePlayers().stream().map(Player::getName).toList());
        return new PartyClient(runtime.packets(), mainThread, runtime.identity().id(), runtime.settings().messaging().requestTimeout(),
            new PartyTexts(messages, chatFormat), id -> Optional.ofNullable(server.getPlayer(id)), networkPlayers, plugin.getLogger());
    }

    static void start(JavaPlugin plugin, MuxeCore core, PartyClient parties, Commands commands) {
        parties.start();
        if (core.identity().kind() == ServerKind.AUTH) {
            return;
        }
        Server server = plugin.getServer();
        PermissionService permissions = core.permissions();
        PartyChat chat = new PartyChat(parties, permissions::display);
        ChannelChoices channels = new ChannelChoices(new ChatChannels(core.store(), ChatChannels.DEFAULT_DURATION), core.async());
        PartyPresenceListener presence = new PartyPresenceListener(parties, permissions::display);
        server.getPluginManager().registerEvents(presence, plugin);
        server.getPluginManager().registerEvents(new PartyChatListener(channels, chat), plugin);
        permissions.onChange(presence::announce);
        server.getOnlinePlayers().forEach(player -> {
            presence.announce(player);
            channels.load(player.getUniqueId());
        });
        commands.suggestions(NetworkPlayers.SUGGESTIONS, (CommandSender sender) -> parties.networkPlayers().namesExcept(sender.getName()));
        commands.suggestions(PartyCommand.MEMBERS, (CommandSender sender) -> PartyCommand.members(parties, sender));
        commands.suggestions(PartyCommand.OPEN_PARTIES, (CommandSender sender) -> PartyCommand.openParties(parties));
        commands.register(new PartyCommand(parties), new PartyChatCommand(chat, core.messages()), new ChannelCommand(channels, chat, core.messages()));
    }
}
