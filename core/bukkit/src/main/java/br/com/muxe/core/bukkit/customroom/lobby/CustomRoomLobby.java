package br.com.muxe.core.bukkit.customroom.lobby;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.customroom.CatalogDirectory;
import br.com.muxe.core.bukkit.customroom.CustomRoomSettings;
import br.com.muxe.core.bukkit.customroom.menu.MapSelector;
import br.com.muxe.core.bukkit.customroom.menu.ModeSelector;
import br.com.muxe.core.bukkit.customroom.menu.OptionMenus;
import br.com.muxe.core.bukkit.customroom.menu.OptionsTarget;
import br.com.muxe.core.bukkit.customroom.menu.PresetMenu;
import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.menu.Menu;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.common.customroom.domain.RoomOptions;
import br.com.muxe.core.common.customroom.domain.RoomPreset;
import br.com.muxe.core.common.customroom.schema.ModeContext;
import br.com.muxe.core.common.customroom.schema.RoomEditor;
import br.com.muxe.core.common.customroom.schema.RoomLore;
import br.com.muxe.core.common.customroom.schema.RoomSchema;
import br.com.muxe.core.common.messaging.channel.PacketTarget;
import br.com.muxe.core.common.network.customroom.CreateCustomRoomReply;
import br.com.muxe.core.common.network.customroom.CreateCustomRoomRequest;
import br.com.muxe.core.common.network.customroom.CustomRoomCatalogPacket;
import br.com.muxe.core.common.party.domain.PartySnapshot;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public final class CustomRoomLobby {

    public static final String PERMISSION = "muxe.customroom.create";

    private final MuxeCore core;
    private final CustomRoomSettings settings;
    private final CatalogDirectory catalogs;
    private final ModeSelector modes;
    private final MapSelector maps;
    private final OptionMenus options;
    private final PresetMenu presets;
    private final Consumer<Player> gameSelector;
    private final Consumer<Player> shop;
    private final Map<UUID, Draft> drafts = new ConcurrentHashMap<>();
    private final Set<UUID> creating = ConcurrentHashMap.newKeySet();

    public CustomRoomLobby(MuxeCore core, CustomRoomSettings settings, CatalogDirectory catalogs, ModeSelector modes, MapSelector maps, OptionMenus options,
                           PresetMenu presets, Consumer<Player> gameSelector, Consumer<Player> shop) {
        this.core = core;
        this.settings = settings;
        this.catalogs = catalogs;
        this.modes = modes;
        this.maps = maps;
        this.options = options;
        this.presets = presets;
        this.gameSelector = gameSelector;
        this.shop = shop;
    }

    public void start() {
        core.packets().subscribe(CustomRoomCatalogPacket.TYPE, context -> catalogs.update(context.packet()));
    }

    public void open(Player player) {
        if (!player.hasPermission(PERMISSION)) {
            shop.accept(player);
            return;
        }
        Optional<PartySnapshot> party = core.parties().of(player.getUniqueId());
        if (party.isPresent() && !party.get().isLeader(player.getUniqueId())) {
            player.sendMessage(core.messages().get("custom-room-not-leader"));
            return;
        }
        modes.open(player, choice(gameSelector, settings.section("selector").getString("back-title", "Selecionar jogo")));
    }

    public void forget(UUID player) {
        drafts.remove(player);
    }

    private ModeSelector.Choice choice(Consumer<Player> back, String backTitle) {
        return new ModeSelector.Choice(catalogs::has, chosen -> {
            Draft previous = drafts.get(chosen.player().getUniqueId());
            boolean sameGame = previous != null && previous.game().equals(chosen.game());
            Draft draft = new Draft(chosen.game(), chosen.mode(), chosen.name(), chosen.icon(),
                sameGame ? previous.maps() : List.of(), sameGame ? previous.options() : RoomOptions.EMPTY);
            drafts.put(chosen.player().getUniqueId(), draft);
            openCreate(chosen.player());
        }, back, backTitle);
    }

    public void openCreate(Player player) {
        Draft draft = drafts.get(player.getUniqueId());
        if (draft == null) {
            open(player);
            return;
        }
        ConfigurationSection menu = settings.section("room-menu");
        String title = menu.getString("create-title", "Criando sala...");
        Menu.Builder builder = Menu.builder(Colors.translate(title)).rows(menu.getInt("rows", 5));
        builder.button(settings.slot("room-menu.game"), draft.icon().lore(menu.getStringList("game.lore").stream().map(Colors::translate).toList()),
            context -> modes.open(context.player(), choice(this::openCreate, title)));
        List<CustomRoomCatalogPacket.Arena> arenas = arenas(draft);
        String selected = MapSelector.summary(draft.maps(), arenas, menu.getString("maps.none", "Nenhum"));
        builder.button(settings.slot("room-menu.maps"), settings.item("room-menu.maps").replacing("maps", selected), context ->
            maps.open(context.player(), new MapSelector.Selection(arenas, draft(context.player()).maps(),
                changed -> update(context.player(), current -> current.withMaps(changed)), this::openCreate, title), 1));
        builder.button(settings.slot("room-menu.options"), settings.item("room-menu.options"), context -> target(context.player())
            .ifPresent(target -> options.open(context.player(), target, target.lore().editor().schema().root())));
        builder.button(settings.slot("room-menu.create"), settings.item("room-menu.create"), context -> {
            context.close();
            create(context.player());
        });
        String modeMenuTitle = modeMenuTitle(draft.game());
        builder.button(settings.slot("room-menu.back"), OptionMenus.backButton(settings.texts(), modeMenuTitle),
            context -> modes.openModes(context.player(), modeMenu(draft.game()), choice(gameSelector, settings.section("selector").getString("back-title", "Selecionar jogo"))));
        builder.button(settings.slot("room-menu.presets"), settings.item("room-menu.presets"), context -> presets.open(context.player(), presetTarget(context.player(), title)));
        builder.build().open(player);
    }

    private PresetMenu.Target presetTarget(Player player, String title) {
        Draft draft = draft(player);
        return new PresetMenu.Target(draft.game(), mode -> catalogs.catalog(draft.game()).flatMap(packet -> packet.mode(mode))
            .map(CustomRoomCatalogPacket.Mode::displayName).orElse(mode),
            () -> {
                Draft current = draft(player);
                return new PresetMenu.Snapshot(current.mode(), current.maps(), current.options());
            },
            preset -> load(player, preset), this::openCreate, title);
    }

    private void load(Player player, RoomPreset preset) {
        Draft draft = draft(player);
        boolean modeExists = catalogs.has(preset.game(), preset.mode());
        String mode = modeExists ? preset.mode() : draft.mode();
        ItemBuilder icon = modeExists ? modes.icon(preset.game(), preset.mode()).orElse(draft.icon()) : draft.icon();
        String name = modeExists ? Colors.strip(icon.name() == null ? mode : icon.name()) : draft.modeName();
        drafts.put(player.getUniqueId(), new Draft(draft.game(), mode, name, icon, preset.maps(), preset.options()));
    }

    private Optional<OptionsTarget> target(Player player) {
        Draft draft = draft(player);
        Optional<RoomSchema> schema = catalogs.schema(draft.game());
        Optional<CustomRoomCatalogPacket.Mode> mode = catalogs.catalog(draft.game()).flatMap(packet -> packet.mode(draft.mode()));
        if (schema.isEmpty() || mode.isEmpty()) {
            return Optional.empty();
        }
        RoomLore lore = new RoomLore(new RoomEditor(schema.get(), context(mode.get())), settings.texts());
        String title = settings.section("room-menu").getString("create-title", "Criando sala...");
        return Optional.of(new OptionsTarget() {
            @Override
            public RoomLore lore() {
                return lore;
            }

            @Override
            public RoomOptions options() {
                return draft(player).options();
            }

            @Override
            public void change(Player who, RoomOptions changed) {
                update(who, current -> current.withOptions(changed));
            }

            @Override
            public void back(Player who) {
                openCreate(who);
            }

            @Override
            public String parentTitle() {
                return title;
            }
        });
    }

    private void create(Player player) {
        Draft draft = draft(player);
        List<String> servers = catalogs.servers(draft.game());
        if (servers.isEmpty() || !creating.add(player.getUniqueId())) {
            if (servers.isEmpty()) {
                player.sendMessage(core.messages().get("custom-room-unavailable"));
            }
            return;
        }
        Optional<PartySnapshot> party = core.parties().of(player.getUniqueId());
        if (party.isPresent() && !party.get().isLeader(player.getUniqueId())) {
            creating.remove(player.getUniqueId());
            player.sendMessage(core.messages().get("custom-room-not-leader"));
            return;
        }
        List<UUID> companions = core.parties().companions(player.getUniqueId());
        String server = servers.get(ThreadLocalRandom.current().nextInt(servers.size()));
        player.sendMessage(core.messages().get("custom-room-creating"));
        CreateCustomRoomRequest request = new CreateCustomRoomRequest(player.getUniqueId(), player.getName(), draft.game(), draft.mode(), draft.maps(),
            draft.options(), companions);
        core.packets().request(PacketTarget.server(server), request, CreateCustomRoomReply.class, core.requestTimeout()).whenCompleteAsync((reply, error) -> {
            creating.remove(player.getUniqueId());
            if (!player.isOnline()) {
                return;
            }
            if (error != null || reply == null || !reply.accepted()) {
                player.sendMessage(core.messages().get("custom-room-unavailable"));
                return;
            }
            player.sendMessage(core.messages().get("custom-room-sending", "code", reply.code()));
            core.network().connectToServer(player.getUniqueId(), server).exceptionally(failure -> {
                core.mainThread().execute(() -> player.sendMessage(core.messages().get("custom-room-unavailable")));
                return null;
            });
            companions.forEach(companion -> core.network().connectToServer(companion, server));
        }, core.mainThread());
    }

    private List<CustomRoomCatalogPacket.Arena> arenas(Draft draft) {
        return catalogs.catalog(draft.game()).map(packet -> packet.arenasOf(draft.mode())).orElse(List.of());
    }

    private String modeMenu(String game) {
        ConfigurationSection menus = settings.section("mode-menus");
        for (String id : menus.getKeys(false)) {
            if (menus.getString(id + ".game", id).equals(game)) {
                return id;
            }
        }
        return game;
    }

    private String modeMenuTitle(String game) {
        return settings.section("mode-menus." + modeMenu(game)).getString("title", "Selecionar modo");
    }

    private Draft draft(Player player) {
        return drafts.get(player.getUniqueId());
    }

    private void update(Player player, UnaryOperator<Draft> change) {
        drafts.computeIfPresent(player.getUniqueId(), (id, draft) -> change.apply(draft));
    }

    public static ModeContext context(CustomRoomCatalogPacket.Mode mode) {
        return new ModeContext(mode.id(), mode.teamSize(), mode.maxPlayers(), mode.teams());
    }

    private record Draft(String game, String mode, String modeName, ItemBuilder icon, List<String> maps, RoomOptions options) {

        private Draft {
            maps = List.copyOf(maps);
        }

        private Draft withMaps(List<String> changed) {
            return new Draft(game, mode, modeName, icon, changed, options);
        }

        private Draft withOptions(RoomOptions changed) {
            return new Draft(game, mode, modeName, icon, maps, changed);
        }
    }
}
