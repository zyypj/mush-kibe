package br.com.muxe.core.proxy.party;

import br.com.muxe.core.common.messaging.bus.PacketBus;
import br.com.muxe.core.common.messaging.bus.PacketContext;
import br.com.muxe.core.common.messaging.channel.PacketTarget;
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
import br.com.muxe.core.common.party.domain.PartyBook;
import br.com.muxe.core.common.party.domain.PartyNotice;
import br.com.muxe.core.common.party.domain.PartyOutcome;
import br.com.muxe.core.common.party.domain.PartyPlayer;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PartyCoordinator {

    private final PacketBus bus;
    private final PartyBook book;
    private final PlayerDisplays displays;
    private final PartyNetwork network;
    private final Clock clock;
    private final Duration gatherTimeout;
    private final Logger logger;

    public PartyCoordinator(PacketBus bus, PartyBook book, PlayerDisplays displays, PartyNetwork network, Clock clock, Duration gatherTimeout, Logger logger) {
        this.bus = bus;
        this.book = book;
        this.displays = displays;
        this.network = network;
        this.clock = clock;
        this.gatherTimeout = gatherTimeout;
        this.logger = logger;
    }

    public void register() {
        bus.subscribe(PartyActionPacket.TYPE, context -> act(context.packet()));
        bus.subscribe(PlayerDisplayPacket.TYPE, context -> displays.remember(context.packet().playerId(), context.packet().coloredName()));
        bus.subscribe(PartySyncRequest.TYPE, this::sync);
    }

    public void act(PartyActionPacket packet) {
        PartyPlayer actor = new PartyPlayer(packet.actor(), packet.actorName());
        List<PartyOutcome> outcomes = new ArrayList<>();
        synchronized (book) {
            switch (packet.action()) {
                case INVITE -> packet.arguments().forEach(name -> outcomes.add(invite(actor, name)));
                case ACCEPT -> outcomes.add(book.accept(actor, packet.argument(), clock.instant()));
                case LEAVE -> outcomes.add(book.leave(actor));
                case DISBAND -> outcomes.add(book.disband(actor));
                case KICK -> outcomes.add(book.kick(actor, packet.argument()));
                case TRANSFER -> outcomes.add(book.transfer(actor, packet.argument()));
                case JOIN -> outcomes.add(book.join(actor, packet.argument()));
                case REMOVE_OFFLINE -> outcomes.add(book.removeOffline(actor));
                case OPEN -> outcomes.add(book.toggleOpen(actor, packet.allowed()));
                case CHAT -> outcomes.add(book.chat(actor, packet.argument()));
                case TELEPORT -> outcomes.add(book.summon(actor));
            }
        }
        outcomes.forEach(this::apply);
        outcomes.stream().filter(outcome -> !outcome.summoned().isEmpty()).forEach(outcome -> gather(actor.id(), outcome.summoned()));
    }

    public void connected(PartyPlayer player) {
        PartyOutcome outcome;
        synchronized (book) {
            outcome = book.connected(player);
        }
        apply(outcome);
    }

    public void disconnected(UUID player) {
        PartyOutcome outcome;
        synchronized (book) {
            outcome = book.disconnected(player, clock.instant());
        }
        apply(outcome);
    }

    public void tick() {
        PartyOutcome outcome;
        synchronized (book) {
            outcome = book.tick(clock.instant());
            displays.retain(player -> network.serverOf(player).isPresent() || book.partyOf(player).isPresent());
        }
        apply(outcome);
    }

    public void publishOnlinePlayers() {
        bus.publish(PacketTarget.broadcast(), new OnlinePlayersPacket(network.onlineNames())).exceptionally(this::logged);
    }

    private PartyOutcome invite(PartyPlayer actor, String name) {
        Optional<PartyPlayer> target = network.online(name);
        if (target.isEmpty()) {
            return PartyOutcome.refused(actor.id(), "party-player-not-found", "name", name);
        }
        return book.invite(actor, target.get(), clock.instant());
    }

    private void sync(PacketContext<PartySyncRequest> context) {
        PartySyncResponse response;
        synchronized (book) {
            response = new PartySyncResponse(book.all());
        }
        context.reply(response);
    }

    private void apply(PartyOutcome outcome) {
        outcome.notices().forEach(this::deliver);
        for (UUID changed : outcome.changed()) {
            Optional<PartyUpdatedPacket> packet;
            synchronized (book) {
                packet = book.snapshot(changed).map(PartyUpdatedPacket::new);
            }
            packet.ifPresent(update -> bus.publish(PacketTarget.broadcast(), update).exceptionally(this::logged));
        }
        outcome.removed().forEach(removed -> bus.publish(PacketTarget.broadcast(), new PartyRemovedPacket(removed)).exceptionally(this::logged));
    }

    private void deliver(PartyNotice notice) {
        Map<String, List<UUID>> byServer = new LinkedHashMap<>();
        for (UUID recipient : notice.recipients()) {
            network.serverOf(recipient).ifPresent(server -> byServer.computeIfAbsent(server, ignored -> new ArrayList<>()).add(recipient));
        }
        byServer.forEach((server, recipients) ->
            bus.publish(PacketTarget.server(server), new PartyNoticePacket(notice.to(recipients))).exceptionally(this::logged));
    }

    private void gather(UUID leader, List<UUID> members) {
        Optional<String> server = network.serverOf(leader);
        if (server.isEmpty()) {
            return;
        }
        bus.request(PacketTarget.server(server.get()), new PartyGatherRequest(leader, members), PartyGatherResponse.class, gatherTimeout)
            .whenComplete((response, error) -> {
                if (error != null) {
                    logger.log(Level.WARNING, "Server " + server.get() + " did not answer the party gathering of " + leader, error);
                    return;
                }
                for (UUID member : response.accepted()) {
                    if (network.serverOf(member).filter(server.get()::equals).isEmpty()) {
                        network.connect(member, server.get());
                    }
                }
            });
    }

    private Void logged(Throwable error) {
        logger.log(Level.FINE, "Could not publish a party packet", error);
        return null;
    }
}
