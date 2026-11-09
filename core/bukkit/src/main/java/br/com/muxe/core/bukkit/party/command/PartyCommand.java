package br.com.muxe.core.bukkit.party.command;

import br.com.muxe.core.bukkit.command.framework.annotation.Arg;
import br.com.muxe.core.bukkit.command.framework.annotation.Command;
import br.com.muxe.core.bukkit.command.framework.annotation.Default;
import br.com.muxe.core.bukkit.command.framework.annotation.Greedy;
import br.com.muxe.core.bukkit.command.framework.annotation.Subcommand;
import br.com.muxe.core.bukkit.command.framework.annotation.Suggest;
import br.com.muxe.core.bukkit.command.framework.annotation.Usage;
import br.com.muxe.core.bukkit.party.NetworkPlayers;
import br.com.muxe.core.bukkit.party.PartyClient;
import br.com.muxe.core.common.network.party.PartyAction;
import br.com.muxe.core.common.party.domain.PartySnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Command(name = "party", description = "Invites players and manages your party.")
public final class PartyCommand {

    public static final String OPEN_PERMISSION = "muxe.party.open";
    public static final String MEMBERS = "party-members";
    public static final String OPEN_PARTIES = "open-parties";

    private final PartyClient parties;

    public PartyCommand(PartyClient parties) {
        this.parties = parties;
    }

    @Default
    public void invite(Player player, @Arg("jogadores") @Suggest(provider = NetworkPlayers.SUGGESTIONS) @Greedy String names) {
        parties.send(player, PartyAction.INVITE, true, distinct(names));
    }

    @Subcommand("aceitar")
    public void accept(Player player, @Arg("jogador") @Suggest(provider = NetworkPlayers.SUGGESTIONS) String inviter) {
        parties.send(player, PartyAction.ACCEPT, inviter);
    }

    @Subcommand("sair")
    public void leave(Player player) {
        parties.send(player, PartyAction.LEAVE);
    }

    @Subcommand("acabar")
    public void disband(Player player) {
        parties.send(player, PartyAction.DISBAND);
    }

    @Subcommand("expulsar")
    public void kick(Player player, @Arg("jogador") @Suggest(provider = MEMBERS) String member) {
        parties.send(player, PartyAction.KICK, member);
    }

    @Subcommand("transferir")
    public void transfer(Player player, @Arg("jogador") @Suggest(provider = MEMBERS) String member) {
        parties.send(player, PartyAction.TRANSFER, member);
    }

    @Subcommand("membros")
    public void members(Player player) {
        Optional<PartySnapshot> party = parties.of(player.getUniqueId());
        if (party.isEmpty()) {
            player.sendMessage(parties.texts().messages().get("party-not-in-party"));
            return;
        }
        player.sendMessage(parties.texts().members(party.get()));
    }

    @Subcommand(value = "teleportar", aliases = "warp")
    public void teleport(Player player) {
        parties.send(player, PartyAction.TELEPORT);
    }

    @Subcommand("entrar")
    public void join(Player player, @Arg("nome") @Suggest(provider = OPEN_PARTIES) String leader) {
        parties.send(player, PartyAction.JOIN, leader);
    }

    @Subcommand("removeroffline")
    public void removeOffline(Player player) {
        parties.send(player, PartyAction.REMOVE_OFFLINE);
    }

    @Subcommand("abrir")
    public void open(Player player) {
        parties.send(player, PartyAction.OPEN, player.hasPermission(OPEN_PERMISSION), List.of());
    }

    @Usage
    public void usage(CommandSender sender) {
        parties.texts().messages().lines("party-usage").forEach(sender::sendMessage);
    }

    static List<String> distinct(String names) {
        List<String> found = new ArrayList<>();
        for (String name : names.trim().split("\\s+")) {
            if (!name.isEmpty() && found.stream().noneMatch(name::equalsIgnoreCase)) {
                found.add(name);
            }
        }
        return found;
    }

    public static List<String> members(PartyClient parties, CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }
        return parties.of(player.getUniqueId()).stream()
            .flatMap(party -> party.members().stream())
            .map(PartySnapshot.Member::name)
            .filter(name -> !name.equalsIgnoreCase(player.getName()))
            .toList();
    }

    public static List<String> openParties(PartyClient parties) {
        return parties.all().stream()
            .filter(PartySnapshot::open)
            .flatMap(party -> party.members().stream().filter(member -> party.isLeader(member.id())))
            .map(PartySnapshot.Member::name)
            .toList();
    }
}
