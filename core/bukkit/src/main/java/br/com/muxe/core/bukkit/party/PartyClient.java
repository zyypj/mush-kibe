package br.com.muxe.core.bukkit.party;

import br.com.muxe.core.bukkit.text.ChatButton;
import br.com.muxe.core.bukkit.text.ClickableText;
import br.com.muxe.core.common.messaging.bus.PacketBus;
import br.com.muxe.core.common.messaging.bus.PacketContext;
import br.com.muxe.core.common.messaging.channel.PacketTarget;
import br.com.muxe.core.common.network.party.PartyAction;
import br.com.muxe.core.common.network.party.PartyActionPacket;
import br.com.muxe.core.common.network.party.PartyGatherRequest;
import br.com.muxe.core.common.network.party.PartyGatherResponse;
import br.com.muxe.core.common.network.party.PartyNoticePacket;
import br.com.muxe.core.common.network.party.PartyRemovedPacket;
import br.com.muxe.core.common.network.party.PartySyncRequest;
import br.com.muxe.core.common.network.party.PartySyncResponse;
import br.com.muxe.core.common.network.party.PartyUpdatedPacket;
import br.com.muxe.core.common.network.player.OnlinePlayersPacket;
import br.com.muxe.core.common.network.player.PlayerDisplayPacket;
import br.com.muxe.core.common.party.domain.PartyDirectory;
import br.com.muxe.core.common.party.domain.PartyNotice;
import br.com.muxe.core.common.party.domain.PartySnapshot;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.entity.Player;

public final class PartyClient implements Parties {

    private final PacketBus packets;
    private final Executor mainThread;
    private final String serverId;
    private final Duration timeout;
    private final PartyTexts texts;
    private final Function<UUID, Optional<Player>> players;
    private final NetworkPlayers networkPlayers;
    private final Logger logger;
    private final PartyDirectory directory = new PartyDirectory();
    private volatile PartyGathering gathering = PartyGathering.EVERYONE;

    public PartyClient(PacketBus packets, Executor mainThread, String serverId, Duration timeout, PartyTexts texts,
                       Function<UUID, Optional<Player>> players, NetworkPlayers networkPlayers, Logger logger) {
        this.packets = packets;
        this.mainThread = mainThread;
        this.serverId = serverId;
        this.timeout = timeout;
        this.texts = texts;
        this.players = players;
        this.networkPlayers = networkPlayers;
        this.logger = logger;
    }

    public void start() {
        packets.subscribe(PartyUpdatedPacket.TYPE, context -> directory.put(context.packet().party()));
        packets.subscribe(PartyRemovedPacket.TYPE, context -> directory.remove(context.packet().partyId()));
        packets.subscribe(PartyNoticePacket.TYPE, context -> mainThread.execute(() -> deliver(context.packet().notice())));
        packets.subscribe(PartyGatherRequest.TYPE, this::gather);
        packets.subscribe(OnlinePlayersPacket.TYPE, context -> networkPlayers.update(context.packet().names()));
        sync();
    }

    public void sync() {
        packets.request(PacketTarget.proxy(), new PartySyncRequest(serverId), PartySyncResponse.class, timeout).whenComplete((response, error) -> {
            if (error != null) {
                logger.log(Level.WARNING, "Could not load the parties from the proxy: " + error.getMessage());
                return;
            }
            directory.replaceAll(response.parties());
        });
    }

    public PartyTexts texts() {
        return texts;
    }

    public NetworkPlayers networkPlayers() {
        return networkPlayers;
    }

    public void send(Player player, PartyAction action, boolean allowed, List<String> arguments) {
        PartyActionPacket packet = new PartyActionPacket(player.getUniqueId(), player.getName(), action, arguments, allowed);
        packets.publish(PacketTarget.proxy(), packet).exceptionally(error -> {
            mainThread.execute(() -> player.sendMessage(texts.messages().get("command-failed")));
            return null;
        });
    }

    public void send(Player player, PartyAction action, String... arguments) {
        send(player, action, true, List.of(arguments));
    }

    public void announce(UUID player, String coloredName) {
        packets.publish(PacketTarget.proxy(), new PlayerDisplayPacket(player, coloredName)).exceptionally(error -> {
            logger.log(Level.FINE, "Could not tell the proxy the name colour of " + player, error);
            return null;
        });
    }

    public void deliver(PartyNotice notice) {
        for (UUID recipient : notice.recipients()) {
            players.apply(recipient).ifPresent(player -> show(player, notice));
        }
    }

    public void show(Player player, PartyNotice notice) {
        List<String> lines = texts.lines(notice);
        Optional<String> click = notice.clickCommand();
        if (click.isEmpty() || lines.isEmpty()) {
            lines.forEach(player::sendMessage);
            return;
        }
        lines.subList(0, lines.size() - 1).forEach(player::sendMessage);
        ClickableText.send(player, ChatButton.run(lines.getLast(), click.get()));
    }

    @Override
    public Optional<PartySnapshot> of(UUID player) {
        return directory.of(player);
    }

    public List<PartySnapshot> all() {
        return directory.all();
    }

    @Override
    public boolean mayQueue(UUID player) {
        return directory.mayQueue(player);
    }

    @Override
    public List<UUID> companions(UUID leader) {
        return directory.companions(leader);
    }

    @Override
    public List<List<UUID>> groupsOf(Collection<UUID> players) {
        return directory.groupsOf(players);
    }

    @Override
    public void onGather(PartyGathering gathering) {
        this.gathering = gathering;
    }

    private void gather(PacketContext<PartyGatherRequest> context) {
        PartyGatherRequest request = context.packet();
        CompletableFuture.supplyAsync(() -> gathering.gather(request.leader(), request.members()), mainThread).whenComplete((accepted, error) -> {
            if (error != null) {
                logger.log(Level.WARNING, "Could not gather the party of " + request.leader(), error);
                context.reply(new PartyGatherResponse(List.of()));
                return;
            }
            context.reply(new PartyGatherResponse(accepted));
        });
    }
}
