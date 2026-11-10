package br.com.muxe.minigame.api.customroom.bukkit;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.command.framework.Commands;
import br.com.muxe.core.bukkit.customroom.CustomRoomKit;
import br.com.muxe.core.bukkit.customroom.CustomRoomSettings;
import br.com.muxe.core.bukkit.customroom.schema.SchemaReader;
import br.com.muxe.core.common.customroom.schema.RoomSchema;
import br.com.muxe.core.common.messaging.channel.PacketTarget;
import br.com.muxe.core.common.network.customroom.CreateCustomRoomReply;
import br.com.muxe.core.common.network.customroom.CreateCustomRoomRequest;
import br.com.muxe.core.common.network.customroom.CustomRoomCatalogPacket;
import br.com.muxe.core.common.party.domain.PartySnapshot;
import br.com.muxe.minigame.api.customroom.CustomRoom;
import br.com.muxe.minigame.api.customroom.CustomRooms;
import br.com.muxe.minigame.api.customroom.RoomPort;
import br.com.muxe.minigame.api.runtime.MatchService;
import br.com.muxe.minigame.api.runtime.MatchState;
import br.com.muxe.minigame.api.settings.HotbarItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public final class CustomRoomServer<S extends MatchState> implements Listener {

    public static final long PUBLISH_PERIOD_TICKS = 200L;
    public static final long SECOND_TICKS = 20L;

    private final Plugin plugin;
    private final MuxeCore core;
    private final String game;
    private final MatchService<?, S> matches;
    private final Supplier<String> schemaText;
    private final CustomRoomSettings settings;
    private final HotbarItem teamItem;
    private final HotbarItem menuItem;
    private final CustomRooms<S> rooms;
    private final List<String> catalogGroups;
    private final TeamMenu<S> teamMenu;
    private final RoomMenu<S> roomMenu;
    private String parsedText = "";
    private RoomSchema schema;

    public CustomRoomServer(Plugin plugin, MuxeCore core, String game, MatchService<?, S> matches, Supplier<String> schemaText, String lobbyGroup,
                            Consumer<Player> moveToReserved) {
        this.plugin = plugin;
        this.core = core;
        this.game = game;
        this.matches = matches;
        this.schemaText = schemaText;
        CustomRoomKit kit = core.customRooms();
        this.settings = kit.settings();
        this.teamItem = HotbarItem.from(settings.section("hotbar.team"));
        this.menuItem = HotbarItem.from(settings.section("hotbar.room-menu"));
        this.catalogGroups = catalogGroups(settings.text("lobby-group", "lobby"), lobbyGroup);
        RoomPort port = new RoomPort() {
            @Override
            public Optional<PartySnapshot> party(UUID player) {
                return core.parties().of(player);
            }

            @Override
            public Optional<String> onlineName(UUID player) {
                return Optional.ofNullable(Bukkit.getPlayer(player)).map(Player::getName);
            }

            @Override
            public void pull(UUID leader, UUID player) {
                if (matches.gather(leader, List.of(player)).isEmpty()) {
                    return;
                }
                Player here = Bukkit.getPlayer(player);
                if (here != null) {
                    moveToReserved.accept(here);
                } else {
                    core.network().connectToServer(player, core.identity().id());
                }
            }

            @Override
            public void toLobby(UUID player) {
                core.network().connectToGroup(player, lobbyGroup);
            }

            @Override
            public void message(UUID player, String text) {
                Optional.ofNullable(Bukkit.getPlayer(player)).ifPresent(online -> online.sendMessage(text));
            }
        };
        this.rooms = new CustomRooms<>(matches, port, core.messages(), this::waitingItems, settings.codeSuffix());
        this.teamMenu = new TeamMenu<>(settings, rooms, core.messages());
        this.roomMenu = new RoomMenu<>(kit, rooms, matches);
    }

    public static List<String> catalogGroups(String networkLobby, String gameLobby) {
        return Stream.of(networkLobby, gameLobby).filter(group -> group != null && !group.isBlank()).distinct().toList();
    }

    public CustomRooms<S> rooms() {
        return rooms;
    }

    public void start(Commands commands) {
        core.packets().subscribe(CreateCustomRoomRequest.TYPE, context -> {
            CreateCustomRoomRequest request = context.packet();
            if (!request.game().equals(game)) {
                return;
            }
            CompletableFuture.supplyAsync(() -> schema().flatMap(found -> rooms.create(request.leader(), found, request.mode(), request.maps(), request.options(),
                    request.companions())), core.mainThread())
                .thenAccept(code -> context.reply(code.map(CreateCustomRoomReply::created).orElse(CreateCustomRoomReply.refused())))
                .exceptionally(error -> {
                    plugin.getLogger().log(Level.WARNING, "Could not create a custom room for " + request.leaderName(), error);
                    context.reply(CreateCustomRoomReply.refused());
                    return null;
                });
        });
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::publish, SECOND_TICKS, PUBLISH_PERIOD_TICKS);
        plugin.getServer().getScheduler().runTaskTimer(plugin, rooms::second, SECOND_TICKS, SECOND_TICKS);
        commands.register(new RoomMenuCommand(rooms, roomMenu, core.messages()));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getItem() == null) {
            return;
        }
        Player player = event.getPlayer();
        Optional<CustomRoom> room = rooms.of(player.getUniqueId());
        if (room.isEmpty() || !rooms.runtime(room.get()).map(runtime -> runtime.match().phase().beforeStart()).orElse(false)) {
            return;
        }
        int held = player.getInventory().getHeldItemSlot();
        String material = event.getItem().getType().name();
        if (held == teamItem.slot() && material.equals(teamItem.material())) {
            event.setCancelled(true);
            teamMenu.open(player);
        } else if (held == menuItem.slot() && material.equals(menuItem.material()) && room.get().isLeader(player.getUniqueId())) {
            event.setCancelled(true);
            roomMenu.open(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        rooms.left(event.getPlayer().getUniqueId());
    }

    private List<HotbarItem> waitingItems(CustomRoom room, UUID player, List<HotbarItem> standard) {
        List<HotbarItem> items = new ArrayList<>(standard);
        items.add(teamItem);
        if (room.isLeader(player)) {
            items.add(menuItem);
        }
        return items;
    }

    private Optional<RoomSchema> schema() {
        String text = schemaText.get();
        if (schema == null || !text.equals(parsedText)) {
            try {
                schema = SchemaReader.parse(game, text);
                parsedText = text;
            } catch (RuntimeException exception) {
                plugin.getLogger().log(Level.WARNING, "The custom room schema of " + game + " is invalid", exception);
                return Optional.empty();
            }
        }
        return Optional.of(schema);
    }

    private void publish() {
        String text = schemaText.get();
        if (schema().isEmpty()) {
            return;
        }
        List<CustomRoomCatalogPacket.Mode> modes = matches.modes().stream().map(matches::mode).flatMap(Optional::stream)
            .map(mode -> new CustomRoomCatalogPacket.Mode(mode.id(), mode.displayName(), mode.teamSize(), mode.maxPlayers())).toList();
        List<CustomRoomCatalogPacket.Arena> arenas = matches.arenas().stream()
            .map(arena -> new CustomRoomCatalogPacket.Arena(arena.mode(), arena.name(), arena.displayName())).toList();
        CustomRoomCatalogPacket packet = new CustomRoomCatalogPacket(core.identity().id(), game, CustomRoomCatalogPacket.chunks(text), modes, arenas);
        for (String group : catalogGroups) {
            core.packets().publish(PacketTarget.group(group), packet).exceptionally(error -> {
                plugin.getLogger().log(Level.FINE, "Could not publish the custom room catalog to " + group, error);
                return null;
            });
        }
    }
}
