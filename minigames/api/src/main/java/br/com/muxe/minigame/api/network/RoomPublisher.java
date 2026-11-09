package br.com.muxe.minigame.api.network;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.common.messaging.channel.PacketTarget;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class RoomPublisher {

    public static final long PERIOD_TICKS = 40L;

    private final Plugin plugin;
    private final MuxeCore core;
    private final RoomSource rooms;
    private final MatchPackets packets;
    private final List<String> groups;
    private final List<Consumer<RoomsPacket>> listeners = new ArrayList<>();
    private BukkitTask task;

    public RoomPublisher(Plugin plugin, MuxeCore core, RoomSource rooms, MatchPackets packets, List<String> groups) {
        this.packets = packets;
        this.plugin = plugin;
        this.core = core;
        this.rooms = rooms;
        this.groups = List.copyOf(groups);
    }

    public void start() {
        packets.registerAll(core.packetRegistry());
        core.packets().subscribe(packets.joinRequest(), context -> {
            JoinRoomRequest request = context.packet();
            CompletableFuture.supplyAsync(() -> rooms.reserve(request.playerId(), request.companions(), request.mode(), request.wantedArena(), request.wantedRoom()), core.mainThread())
                .thenAccept(reserved -> context.reply(reserved.map(found -> JoinRoomReply.reserved(found.code(), found.created())).orElse(JoinRoomReply.refused())))
                .exceptionally(error -> {
                    plugin.getLogger().log(Level.WARNING, "Could not reserve a room for " + request.playerId(), error);
                    context.reply(JoinRoomReply.refused());
                    return null;
                });
        });
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::publish, PERIOD_TICKS, PERIOD_TICKS);
    }

    public void onPublish(Consumer<RoomsPacket> listener) {
        listeners.add(listener);
    }

    public void stop() {
        Optional.ofNullable(task).ifPresent(BukkitTask::cancel);
    }

    private void publish() {
        RoomsPacket packet = new RoomsPacket(core.identity().id(), rooms.summaries(), rooms.arenas());
        listeners.forEach(listener -> listener.accept(packet));
        for (String group : groups) {
            core.packets().publish(PacketTarget.group(group), packet).exceptionally(error -> {
                plugin.getLogger().log(Level.FINE, "Could not publish the rooms to " + group, error);
                return null;
            });
        }
    }
}
