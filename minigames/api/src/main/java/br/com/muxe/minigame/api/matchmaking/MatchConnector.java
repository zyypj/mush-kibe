package br.com.muxe.minigame.api.matchmaking;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.messaging.channel.PacketTarget;
import br.com.muxe.minigame.api.network.JoinRoomReply;
import br.com.muxe.minigame.api.network.JoinRoomRequest;
import br.com.muxe.minigame.api.network.MatchPackets;
import br.com.muxe.minigame.engine.mode.GameMode;
import br.com.muxe.minigame.engine.mode.ModeNames;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.bukkit.entity.Player;

public final class MatchConnector {

    public static final Duration DIRECTORY_TIME_TO_LIVE = Duration.ofSeconds(10);

    private final MuxeCore core;
    private final MessageCatalog messages;
    private final RoomDirectory directory;
    private final ModeNames modeNames;
    private final Function<String, Optional<GameMode>> modes;
    private final Set<UUID> pending = ConcurrentHashMap.newKeySet();
    private final LocalJoin local;
    private final MatchPackets packets;

    public MatchConnector(MuxeCore core, MatchPackets packets, MessageCatalog messages, Function<String, String> modeNames,
                          Function<String, Optional<GameMode>> modes) {
        this(core, packets, messages, ModeNames.plain(modeNames), modes, (player, request) -> false);
    }

    public MatchConnector(MuxeCore core, MatchPackets packets, MessageCatalog messages, ModeNames modeNames, Function<String, Optional<GameMode>> modes) {
        this(core, packets, messages, modeNames, modes, (player, request) -> false);
    }

    public MatchConnector(MuxeCore core, MatchPackets packets, MessageCatalog messages, Function<String, String> modeNames,
                          Function<String, Optional<GameMode>> modes, LocalJoin local) {
        this(core, packets, messages, ModeNames.plain(modeNames), modes, local);
    }

    public MatchConnector(MuxeCore core, MatchPackets packets, MessageCatalog messages, ModeNames modeNames,
                          Function<String, Optional<GameMode>> modes, LocalJoin local) {
        this.packets = packets;
        this.local = local;
        this.core = core;
        this.messages = messages;
        this.modeNames = modeNames;
        this.modes = modes;
        this.directory = new RoomDirectory(core.clock(), DIRECTORY_TIME_TO_LIVE);
    }

    public void start() {
        packets.registerAll(core.packetRegistry());
        core.packets().subscribe(packets.rooms(), context -> directory.update(context.packet()));
    }

    public RoomDirectory directory() {
        return directory;
    }

    public void join(Player player, String mode, Optional<String> arena) {
        UUID id = player.getUniqueId();
        PartyCheck check = PartyCheck.of(core.parties(), id, modes.apply(mode));
        if (check.refusal().isPresent()) {
            player.sendMessage(messages.get(player, check.refusal().get()));
            return;
        }
        List<UUID> companions = check.companions();
        Optional<Matchmaker.Target> target = Matchmaker.plan(directory.alive(), mode, arena, 1 + companions.size());
        if (target.isEmpty()) {
            player.sendMessage(messages.get(player, "room-unavailable", "mode", modeNames.of(mode)));
            return;
        }
        if (!pending.add(id)) {
            return;
        }
        JoinNotices.beforeRequest(target.get()).forEach(key -> player.sendMessage(messages.get(player, key)));
        JoinRoomRequest request = new JoinRoomRequest(id, mode, arena.orElse(""), target.get().roomCode(), companions);
        if (target.get().serverId().equals(core.identity().id())) {
            pending.remove(id);
            if (!local.join(player, request)) {
                player.sendMessage(messages.get(player, "room-unavailable", "mode", modeNames.of(mode)));
            }
            return;
        }
        core.packets().request(PacketTarget.server(target.get().serverId()), request, JoinRoomReply.class, core.requestTimeout())
            .whenCompleteAsync((reply, error) -> {
                pending.remove(id);
                if (!player.isOnline()) {
                    return;
                }
                if (error != null || reply == null || !reply.accepted()) {
                    player.sendMessage(messages.get(player, "room-unavailable", "mode", modeNames.of(mode)));
                    return;
                }
                JoinNotices.afterReply(reply).forEach(key -> player.sendMessage(messages.get(player, key, "room", reply.roomCode())));
                core.network().connectToServer(id, target.get().serverId()).exceptionally(failure -> {
                    core.mainThread().execute(() -> player.sendMessage(messages.get(player, "room-unavailable", "mode", modeNames.of(mode))));
                    return null;
                });
                companions.forEach(companion -> core.network().connectToServer(companion, target.get().serverId()));
            }, core.mainThread());
    }

    public interface LocalJoin {

        boolean join(Player player, JoinRoomRequest request);
    }
}
