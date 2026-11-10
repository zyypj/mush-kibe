package br.com.muxe.minigame.api.runtime;

import br.com.muxe.core.bukkit.effect.SoundCue;
import br.com.muxe.core.bukkit.text.Title;
import br.com.muxe.core.common.combat.knockback.KnockbackProfile;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.minigame.api.settings.HotbarItem;
import br.com.muxe.minigame.engine.setup.Point;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class FakeStage implements Stage {

    public final Set<UUID> present = new HashSet<>();
    public final Map<UUID, List<String>> messages = new HashMap<>();
    public final Map<UUID, List<String>> centered = new HashMap<>();
    public final Map<UUID, List<Title>> titles = new HashMap<>();
    public final Map<UUID, Look> looks = new HashMap<>();
    public final Map<UUID, Point> positions = new HashMap<>();
    public final Map<UUID, List<HotbarItem>> items = new HashMap<>();
    public final Map<UUID, List<String>> boards = new HashMap<>();
    public final Map<UUID, String> sentTo = new HashMap<>();
    public final Map<UUID, Boolean> endTitles = new HashMap<>();
    public final Set<UUID> returns = new HashSet<>();
    public final Set<UUID> spectators = new HashSet<>();
    public final List<UUID> teleportedOut = new ArrayList<>();
    public final List<Point> erased = new ArrayList<>();
    public final Map<UUID, Double> knockedUp = new HashMap<>();
    public final Map<UUID, Point> raised = new HashMap<>();
    public final Map<UUID, List<SoundCue>> sounds = new HashMap<>();
    public final Map<UUID, KnockbackProfile> knockbacks = new HashMap<>();
    public final Map<UUID, Language> languages = new HashMap<>();
    public final Map<UUID, String> boardTitles = new HashMap<>();

    @Override
    public World world() {
        return null;
    }

    @Override
    public Location location(Point point) {
        return new Location(null, point.x(), point.y(), point.z());
    }

    @Override
    public Optional<Player> player(UUID id) {
        return Optional.empty();
    }

    @Override
    public List<UUID> present(Collection<UUID> ids) {
        return ids.stream().filter(present::contains).toList();
    }

    @Override
    public String rankColored(UUID id, String name) {
        return name;
    }

    @Override
    public Language language(UUID id) {
        return languages.getOrDefault(id, Language.PORTUGUESE);
    }

    @Override
    public void message(UUID id, String text) {
        messages.computeIfAbsent(id, key -> new ArrayList<>()).add(text);
    }

    @Override
    public void centered(UUID id, List<String> lines) {
        centered.computeIfAbsent(id, key -> new ArrayList<>()).addAll(lines);
    }

    @Override
    public void title(UUID id, Title title) {
        titles.computeIfAbsent(id, key -> new ArrayList<>()).add(title);
    }

    @Override
    public void sound(UUID id, Optional<SoundCue> cue) {
        cue.ifPresent(sound -> sounds.computeIfAbsent(id, key -> new ArrayList<>()).add(sound));
    }

    @Override
    public void prepare(UUID id, Look look) {
        looks.put(id, look);
        items.remove(id);
    }

    @Override
    public void teleport(UUID id, Point point) {
        positions.put(id, point);
    }

    @Override
    public void teleportOut(UUID id) {
        teleportedOut.add(id);
    }

    @Override
    public void knockUp(UUID id, double speed) {
        knockedUp.put(id, speed);
    }

    @Override
    public void raiseTo(UUID id, Point floor) {
        raised.put(id, floor);
    }

    @Override
    public void waitingItems(UUID id, List<HotbarItem> list) {
        items.put(id, List.copyOf(list));
    }

    @Override
    public void spectatorItems(UUID id, List<HotbarItem> list) {
        items.put(id, List.copyOf(list));
    }

    @Override
    public void showBoard(UUID id) {
        boards.put(id, List.of());
    }

    @Override
    public void board(UUID id, String title, List<String> lines) {
        boards.put(id, List.copyOf(lines));
        boardTitles.put(id, title);
    }

    @Override
    public void dropBoard(UUID id) {
        boards.remove(id);
    }

    @Override
    public void health(Collection<UUID> viewers, Collection<UUID> alive) {
    }

    @Override
    public void refreshNameTag(UUID id) {
    }

    @Override
    public void visibility(Collection<UUID> online, Predicate<UUID> spectator) {
        spectators.clear();
        online.stream().filter(spectator).forEach(spectators::add);
    }

    @Override
    public void sendTo(UUID id, String group) {
        sentTo.put(id, group);
    }

    @Override
    public void erase(Point first, Point second) {
        erased.add(first);
        erased.add(second);
    }

    @Override
    public void endTitle(UUID id, boolean won) {
        endTitles.put(id, won);
    }

    @Override
    public void rememberReturn(UUID id, Duration timeToLive) {
        returns.add(id);
    }

    @Override
    public void forgetReturn(UUID id) {
        returns.remove(id);
    }

    @Override
    public void knockback(UUID id, Optional<KnockbackProfile> profile) {
        profile.ifPresentOrElse(chosen -> knockbacks.put(id, chosen), () -> knockbacks.remove(id));
    }

    public Optional<KnockbackProfile> knockbackOf(UUID id) {
        return Optional.ofNullable(knockbacks.get(id));
    }

    public List<String> messagesOf(UUID id) {
        return messages.getOrDefault(id, List.of());
    }

    public Optional<Title> lastTitle(UUID id) {
        List<Title> list = titles.getOrDefault(id, List.of());
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getLast());
    }
}
