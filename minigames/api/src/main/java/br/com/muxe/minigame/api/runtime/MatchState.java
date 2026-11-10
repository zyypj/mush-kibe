package br.com.muxe.minigame.api.runtime;

import br.com.muxe.core.bukkit.cosmetic.animation.Area;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.Text;
import br.com.muxe.minigame.api.match.Participant;
import br.com.muxe.minigame.api.timeline.Timeline.Upcoming;
import br.com.muxe.minigame.engine.team.TeamColor;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface MatchState {

    default void arrived(UUID player) {
    }

    default void left(UUID player) {
    }

    default void everySecond() {
    }

    default void fastTick() {
    }

    default void starting() {
    }

    default void spawned(Participant player) {
    }

    default void started() {
    }

    default void playing() {
    }

    default void died(Death death) {
    }

    default void standingsChanged() {
    }

    default void ending() {
    }

    default void ended(Optional<TeamColor> winner) {
    }

    default void closed() {
    }

    default Optional<String> deathMessage(Death death, String victimName, Optional<String> killerName) {
        return Optional.empty();
    }

    default Optional<Text> deathText(Death death, String victimName, Optional<String> killerName) {
        return deathMessage(death, victimName, killerName).map(Text::plain);
    }

    default Map<String, String> placeholders(Optional<Participant> viewer) {
        return Map.of();
    }

    default Map<String, String> placeholders(Optional<Participant> viewer, Language language) {
        return placeholders(viewer);
    }

    default Optional<String> skipEvent() {
        return Optional.empty();
    }

    default Optional<Text> skipEventText() {
        return skipEvent().map(Text::plain);
    }

    default void optionsChanged() {
    }

    default Optional<Upcoming> upcoming() {
        return Optional.empty();
    }

    default int score(String ranking, Participant player) {
        return switch (ranking) {
            case "kills" -> player.kills();
            case "final-kills" -> player.finalKills();
            default -> 0;
        };
    }

    default Optional<Area> area() {
        return Optional.empty();
    }

    default String chatBadge(UUID player) {
        return "";
    }
}
