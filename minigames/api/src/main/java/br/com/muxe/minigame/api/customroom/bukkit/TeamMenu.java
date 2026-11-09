package br.com.muxe.minigame.api.customroom.bukkit;

import br.com.muxe.core.bukkit.customroom.CustomRoomSettings;
import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.menu.Menu;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.minigame.api.customroom.CustomRoom;
import br.com.muxe.minigame.api.customroom.CustomRoom.Choice;
import br.com.muxe.minigame.api.customroom.CustomRooms;
import br.com.muxe.minigame.api.match.Match;
import br.com.muxe.minigame.api.runtime.MatchState;
import br.com.muxe.minigame.engine.team.TeamColor;
import java.util.List;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

final class TeamMenu<S extends MatchState> {

    private final CustomRoomSettings settings;
    private final CustomRooms<S> rooms;
    private final MessageCatalog messages;

    TeamMenu(CustomRoomSettings settings, CustomRooms<S> rooms, MessageCatalog messages) {
        this.settings = settings;
        this.rooms = rooms;
        this.messages = messages;
    }

    void open(Player player) {
        Optional<CustomRoom> found = rooms.of(player.getUniqueId());
        if (found.isEmpty()) {
            return;
        }
        CustomRoom room = found.get();
        Optional<Match> match = rooms.runtime(room).map(runtime -> runtime.match());
        if (match.isEmpty()) {
            return;
        }
        ConfigurationSection section = settings.section("teams-menu");
        List<Integer> slots = section.getIntegerList("slots");
        Menu.Builder menu = Menu.builder(Colors.translate(section.getString("title", "Selecionar time"))).rows(section.getInt("rows", 4));
        List<TeamColor> teams = List.copyOf(match.get().teams().keySet());
        int index = 0;
        for (TeamColor team : teams) {
            if (index >= slots.size()) {
                break;
            }
            ItemBuilder icon = settings.item("teams-menu.team").material(Material.WOOL).data(team.woolData()).replacing(
                "color", Colors.translate(team.color()), "team", match.get().teamName(team), "players", String.valueOf(room.playersOf(team)),
                "size", String.valueOf(room.teamSize()));
            menu.button(slots.get(index++), icon, context -> chosen(context.player(), Optional.of(team), match.get().teamName(team), team));
        }
        if (index < slots.size()) {
            ItemBuilder spectator = settings.item("teams-menu.spectator").replacing("players", String.valueOf(room.spectatorCount()));
            menu.button(slots.get(index), spectator, context -> chosen(context.player(), Optional.empty(), "", null));
        }
        menu.build().open(player);
    }

    private void chosen(Player player, Optional<TeamColor> team, String name, TeamColor color) {
        Choice choice = rooms.choose(player.getUniqueId(), team);
        switch (choice) {
            case JOINED -> player.sendMessage(messages.get("custom-room-team-chosen", "team", Colors.translate(color.color()) + name));
            case SPECTATOR -> player.sendMessage(messages.get("custom-room-spectator-chosen"));
            case FULL -> player.sendMessage(messages.get("custom-room-team-full"));
            case UNAVAILABLE -> {
            }
        }
        if (choice != Choice.UNAVAILABLE) {
            open(player);
        }
    }
}
