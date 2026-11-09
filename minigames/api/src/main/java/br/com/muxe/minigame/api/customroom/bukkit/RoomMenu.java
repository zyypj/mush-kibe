package br.com.muxe.minigame.api.customroom.bukkit;

import br.com.muxe.core.bukkit.customroom.CustomRoomKit;
import br.com.muxe.core.bukkit.customroom.CustomRoomSettings;
import br.com.muxe.core.bukkit.customroom.menu.MapSelector;
import br.com.muxe.core.bukkit.customroom.menu.ModeSelector;
import br.com.muxe.core.bukkit.customroom.menu.OptionsTarget;
import br.com.muxe.core.bukkit.customroom.menu.PresetMenu;
import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.menu.Menu;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.common.customroom.domain.RoomOptions;
import br.com.muxe.core.common.customroom.domain.RoomPreset;
import br.com.muxe.core.common.customroom.schema.RoomLore;
import br.com.muxe.core.common.network.customroom.CustomRoomCatalogPacket.Arena;
import br.com.muxe.minigame.api.customroom.CustomRoom;
import br.com.muxe.minigame.api.customroom.CustomRooms;
import br.com.muxe.minigame.api.match.MatchPhase;
import br.com.muxe.minigame.api.runtime.MatchRuntime;
import br.com.muxe.minigame.api.runtime.MatchService;
import br.com.muxe.minigame.api.runtime.MatchState;
import br.com.muxe.minigame.engine.mode.GameMode;
import java.util.List;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

final class RoomMenu<S extends MatchState> {

    private final CustomRoomKit kit;
    private final CustomRooms<S> rooms;
    private final MatchService<?, S> matches;

    RoomMenu(CustomRoomKit kit, CustomRooms<S> rooms, MatchService<?, S> matches) {
        this.kit = kit;
        this.rooms = rooms;
        this.matches = matches;
    }

    void open(Player player) {
        Optional<CustomRoom> leading = rooms.leading(player.getUniqueId());
        if (leading.isEmpty()) {
            return;
        }
        CustomRoom room = leading.get();
        Optional<MatchRuntime<S>> runtime = rooms.runtime(room);
        if (runtime.isEmpty()) {
            return;
        }
        CustomRoomSettings settings = kit.settings();
        ConfigurationSection section = settings.section("room-menu");
        String title = section.getString("room-title", "Sua sala");
        Menu.Builder menu = Menu.builder(Colors.translate(title)).rows(section.getInt("rows", 5));
        ItemBuilder modeIcon = kit.modes().icon(room.game(), room.baseMode().id()).orElse(ItemBuilder.of(Material.PAPER).name(Colors.translate("&a" + room.baseMode().displayName())));
        menu.button(settings.slot("room-menu.game"), modeIcon.lore(section.getStringList("game.lore").stream().map(Colors::translate).toList()),
            context -> kit.modes().open(context.player(), modeChoice(room, title)));
        List<Arena> arenas = matches.arenas().stream().filter(arena -> arena.mode().equals(room.baseMode().id()))
            .map(arena -> new Arena(arena.mode(), arena.name(), arena.displayName())).toList();
        String selected = MapSelector.summary(room.maps(), arenas, section.getString("maps.none", "Nenhum"));
        ItemBuilder maps = settings.item("room-menu.maps").lore(section.getStringList("maps.room-lore").stream().map(Colors::translate).toList()).replacing("maps", selected);
        menu.button(settings.slot("room-menu.maps"), maps, context -> kit.maps().open(context.player(),
            new MapSelector.Selection(arenas, room.maps(), changed -> rooms.changeMaps(room, changed), this::open, title), 1));
        menu.button(settings.slot("room-menu.options"), settings.item("room-menu.options"),
            context -> kit.options().open(context.player(), target(room, title), room.schema().root()));
        MatchPhase phase = runtime.get().match().phase();
        if (phase.beforeStart()) {
            menu.button(settings.slot("room-menu.start"), settings.item("room-menu.start"), context -> {
                context.close();
                rooms.start(context.player().getUniqueId());
            });
            menu.button(settings.slot("room-menu.pause"), settings.item("room-menu.pause"), context -> {
                context.close();
                rooms.pause(context.player().getUniqueId());
            });
            menu.button(settings.slot("room-menu.restart"), settings.item("room-menu.restart"), context -> restart(context.player(), room));
            menu.button(settings.slot("room-menu.close"), settings.item("room-menu.close"), context -> close(context.player(), room));
        } else {
            runtime.get().state().upcoming().ifPresent(next -> menu.button(settings.slot("room-menu.skip-event"),
                settings.item("room-menu.skip-event").replacing("event", next.name()), context -> {
                    rooms.skipEvent(context.player().getUniqueId());
                    open(context.player());
                }));
            menu.button(section.getInt("restart.playing-slot", 30), settings.item("room-menu.restart"), context -> restart(context.player(), room));
            menu.button(section.getInt("close.playing-slot", 32), settings.item("room-menu.close"), context -> close(context.player(), room));
        }
        menu.button(settings.slot("room-menu.presets"), settings.item("room-menu.presets"), context -> kit.presets().open(context.player(), presets(room, title)));
        menu.build().open(player);
    }

    private void restart(Player player, CustomRoom room) {
        player.closeInventory();
        rooms.restart(room.code());
    }

    private void close(Player player, CustomRoom room) {
        player.closeInventory();
        rooms.close(room.code());
    }

    private ModeSelector.Choice modeChoice(CustomRoom room, String title) {
        return new ModeSelector.Choice((game, mode) -> game.equals(room.game()) && matches.mode(mode).isPresent(), chosen -> {
            matches.mode(chosen.mode()).filter(mode -> !mode.id().equals(room.baseMode().id())).ifPresent(mode -> rooms.changeMode(room, mode));
            if (rooms.leading(chosen.player().getUniqueId()).isPresent()) {
                open(chosen.player());
            }
        }, this::open, title);
    }

    private OptionsTarget target(CustomRoom room, String title) {
        RoomLore lore = new RoomLore(room.editor(), kit.settings().texts());
        return new OptionsTarget() {
            @Override
            public RoomLore lore() {
                return lore;
            }

            @Override
            public RoomOptions options() {
                return room.options();
            }

            @Override
            public void change(Player player, RoomOptions changed) {
                rooms.changeOptions(room, changed);
            }

            @Override
            public void back(Player player) {
                open(player);
            }

            @Override
            public String parentTitle() {
                return title;
            }
        };
    }

    private PresetMenu.Target presets(CustomRoom room, String title) {
        return new PresetMenu.Target(room.game(), mode -> matches.mode(mode).map(GameMode::displayName).orElse(mode),
            () -> new PresetMenu.Snapshot(room.baseMode().id(), room.maps(), room.options()),
            preset -> load(room, preset), this::open, title);
    }

    private void load(CustomRoom room, RoomPreset preset) {
        rooms.changeMaps(room, preset.maps());
        rooms.changeOptions(room, preset.options());
        matches.mode(preset.mode()).filter(mode -> !mode.id().equals(room.baseMode().id())).ifPresent(mode -> rooms.changeMode(room, mode));
    }
}
