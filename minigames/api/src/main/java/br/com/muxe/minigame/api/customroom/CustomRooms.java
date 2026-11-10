package br.com.muxe.minigame.api.customroom;

import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.combat.knockback.KnockbackProfile;
import br.com.muxe.core.common.combat.knockback.KnockbackSetting;
import br.com.muxe.core.common.customroom.domain.RoomOptions;
import br.com.muxe.core.common.customroom.schema.RoomSchema;
import br.com.muxe.core.common.party.domain.PartySnapshot;
import br.com.muxe.minigame.api.customroom.CustomRoom.Choice;
import br.com.muxe.minigame.api.match.Match;
import br.com.muxe.minigame.api.match.MatchPhase;
import br.com.muxe.minigame.api.runtime.MatchRuntime;
import br.com.muxe.minigame.api.runtime.MatchService;
import br.com.muxe.minigame.api.runtime.MatchService.RoomShape;
import br.com.muxe.minigame.api.runtime.MatchState;
import br.com.muxe.minigame.api.settings.HotbarItem;
import br.com.muxe.minigame.engine.mode.GameMode;
import br.com.muxe.minigame.engine.team.TeamColor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class CustomRooms<S extends MatchState> {

    public static final int CLOSE_DELAY = 3;

    private final MatchService<?, S> matches;
    private final RoomPort port;
    private final MessageCatalog messages;
    private final RoomItems items;
    private final String codeSuffix;
    private final Map<String, CustomRoom> rooms = new LinkedHashMap<>();

    public CustomRooms(MatchService<?, S> matches, RoomPort port, MessageCatalog messages, RoomItems items, String codeSuffix) {
        this.matches = matches;
        this.port = port;
        this.messages = messages;
        this.items = items;
        this.codeSuffix = codeSuffix;
    }

    public Optional<String> create(UUID leader, RoomSchema schema, String mode, List<String> maps, RoomOptions options, Collection<UUID> companions) {
        Optional<GameMode> base = matches.mode(mode);
        if (base.isEmpty()) {
            return Optional.empty();
        }
        CustomRoom draft = new CustomRoom("", schema.game(), leader, schema, base.get(), maps, options, companions);
        Optional<MatchRuntime<S>> opened = open(Optional.empty(), draft);
        if (opened.isEmpty()) {
            return Optional.empty();
        }
        String code = opened.get().match().code();
        CustomRoom room = new CustomRoom(code, schema.game(), leader, schema, base.get(), maps, options, companions);
        rooms.put(code, room);
        opened.get().control(new CustomRoomControl(room, items, this::restart));
        matches.hold(leader, opened.get());
        companions.forEach(companion -> matches.hold(companion, opened.get()));
        return Optional.of(code);
    }

    public Optional<CustomRoom> room(String code) {
        return Optional.ofNullable(rooms.get(code));
    }

    public Optional<CustomRoom> of(UUID player) {
        return matches.of(player).map(runtime -> rooms.get(runtime.match().code()));
    }

    public Optional<MatchRuntime<S>> runtime(CustomRoom room) {
        return matches.room(room.code());
    }

    public Collection<CustomRoom> all() {
        return List.copyOf(rooms.values());
    }

    public boolean start(UUID player) {
        return leading(player).flatMap(this::runtime).map(MatchRuntime::startNow).orElse(false);
    }

    public boolean pause(UUID player) {
        return leading(player).flatMap(this::runtime).map(runtime -> {
            boolean paused = runtime.pauseCountdown();
            if (paused) {
                runtime.broadcast(messages.get("custom-room-paused"));
            }
            return paused;
        }).orElse(false);
    }

    public Optional<String> skipEvent(UUID player) {
        return leading(player).flatMap(this::runtime).filter(runtime -> runtime.match().phase() == MatchPhase.PLAYING)
            .flatMap(runtime -> runtime.state().skipEvent().map(name -> {
                runtime.broadcast(messages.get("custom-room-event-skipped", "event", name));
                runtime.refreshBoards();
                return name;
            }));
    }

    public Choice choose(UUID player, Optional<TeamColor> team) {
        Optional<CustomRoom> room = of(player);
        Optional<MatchRuntime<S>> runtime = room.flatMap(this::runtime);
        if (room.isEmpty() || runtime.isEmpty() || !runtime.get().match().phase().beforeStart()) {
            return Choice.UNAVAILABLE;
        }
        return room.get().choose(player, team, List.copyOf(runtime.get().match().teams().keySet()));
    }

    public void changeOptions(CustomRoom room, RoomOptions options) {
        RoomOptions before = room.options();
        room.options(options);
        runtime(room).ifPresent(runtime -> {
            Match match = runtime.match();
            List<TeamColor> arenaTeams = match.arena().teamColors();
            if (match.phase().beforeStart() && shapeChanged(before, options)) {
                if (match.reshape(room.effectiveMode(arenaTeams), room.teams(arenaTeams))) {
                    room.keepChoicesOf(match.joinOrder());
                    room.chosen().forEach((player, team) -> {
                        if (!match.teams().containsKey(team)) {
                            room.left(player);
                        }
                    });
                }
            }
            match.teamNames(room.teamNames(arenaTeams));
            runtime.online().forEach(runtime.stage()::refreshNameTag);
            knockback(runtime, room);
            runtime.state().optionsChanged();
            runtime.refreshBoards();
        });
    }

    public void changeMaps(CustomRoom room, List<String> maps) {
        room.maps(maps);
    }

    public void changeMode(CustomRoom room, GameMode mode) {
        room.mode(mode);
        runtime(room).filter(runtime -> runtime.match().phase().beforeStart()).ifPresent(runtime -> restart(room.code()));
        runtime(room).ifPresent(runtime -> runtime.broadcast(messages.get("custom-room-mode-changed", "mode", mode.displayName())));
    }

    public void restart(String code) {
        CustomRoom room = rooms.get(code);
        Optional<MatchRuntime<S>> old = matches.room(code);
        if (room == null || old.isEmpty()) {
            return;
        }
        List<UUID> present = new ArrayList<>(old.get().online());
        present.remove(room.leader());
        if (old.get().online().contains(room.leader())) {
            present.addFirst(room.leader());
        }
        old.get().close();
        Optional<MatchRuntime<S>> opened = open(Optional.of(code), room);
        if (opened.isEmpty()) {
            present.forEach(port::toLobby);
            rooms.remove(code);
            return;
        }
        opened.get().control(new CustomRoomControl(room, items, this::restart));
        room.keepChoicesOf(present);
        for (UUID player : present) {
            port.onlineName(player).ifPresent(name -> matches.place(player, name, opened.get()));
        }
        opened.get().broadcast(messages.get("custom-room-restarted"));
    }

    public void close(String code) {
        CustomRoom room = rooms.get(code);
        if (room == null || room.closing()) {
            return;
        }
        room.closeIn(CLOSE_DELAY);
        matches.room(code).ifPresent(runtime -> {
            List<UUID> everyone = runtime.online();
            everyone.forEach(player -> port.message(player, messages.get("custom-room-closed")));
            everyone.forEach(port::toLobby);
        });
    }

    public void second() {
        for (CustomRoom room : List.copyOf(rooms.values())) {
            Optional<MatchRuntime<S>> runtime = runtime(room);
            if (runtime.isEmpty()) {
                rooms.remove(room.code());
                continue;
            }
            if (room.closing()) {
                if (room.closeTick()) {
                    rooms.remove(room.code());
                    runtime.get().close();
                }
                continue;
            }
            List<UUID> online = runtime.get().online();
            if (online.contains(room.leader())) {
                room.leaderArrived(true);
            } else if (room.leaderArrived()) {
                close(room.code());
                continue;
            }
            Optional<PartySnapshot> party = port.party(room.leader());
            if (party.isPresent() && !party.get().isLeader(room.leader())) {
                close(room.code());
                continue;
            }
            if (party.isEmpty()) {
                continue;
            }
            List<UUID> partyMembers = party.get().memberIds();
            for (UUID member : room.newMembers(partyMembers)) {
                if (party.get().members().stream().anyMatch(entry -> entry.id().equals(member) && entry.online())) {
                    port.pull(room.leader(), member);
                }
            }
            for (UUID former : room.formerMembers(partyMembers)) {
                if (online.contains(former)) {
                    port.toLobby(former);
                }
            }
        }
    }

    public void left(UUID player) {
        of(player).ifPresent(room -> room.left(player));
    }

    public Optional<CustomRoom> leading(UUID player) {
        return of(player).filter(room -> room.isLeader(player));
    }

    public String label(String code) {
        return code + codeSuffix;
    }

    private Optional<MatchRuntime<S>> open(Optional<String> code, CustomRoom room) {
        RoomShape shape = new RoomShape() {
            @Override
            public GameMode mode(List<TeamColor> arenaTeams) {
                return room.effectiveMode(arenaTeams);
            }

            @Override
            public List<TeamColor> teams(List<TeamColor> arenaTeams) {
                return room.teams(arenaTeams);
            }
        };
        return matches.openPrivate(code, room.baseMode().id(), room.maps(), shape, runtime -> {
            runtime.match().manual(true);
            runtime.match().label(label(runtime.match().code()));
            runtime.match().teamNames(room.teamNames(runtime.match().arena().teamColors()));
            knockback(runtime, room);
        });
    }

    private void knockback(MatchRuntime<S> runtime, CustomRoom room) {
        Map<String, String> changed = room.options().startingWith("knockback.");
        if (changed.isEmpty() && runtime.knockback().isEmpty()) {
            return;
        }
        KnockbackProfile profile = runtime.environment().knockback().forMode(room.baseMode().id()).orElse(KnockbackProfile.VANILLA);
        for (Map.Entry<String, String> entry : changed.entrySet()) {
            Optional<KnockbackSetting> setting = KnockbackSetting.byKey(entry.getKey());
            if (setting.isPresent()) {
                try {
                    profile = setting.get().write(profile, entry.getValue());
                } catch (IllegalArgumentException ignored) {
                    continue;
                }
            }
        }
        runtime.knockback(profile);
    }

    private static boolean shapeChanged(RoomOptions before, RoomOptions after) {
        return !before.startingWith("teams.").equals(after.startingWith("teams.")) || !before.get(CustomRoom.TEAM_SIZE).equals(after.get(CustomRoom.TEAM_SIZE));
    }

    public interface RoomItems {

        List<HotbarItem> waiting(CustomRoom room, UUID player, List<HotbarItem> standard);
    }
}
