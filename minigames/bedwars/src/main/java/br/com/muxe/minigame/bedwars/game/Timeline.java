package br.com.muxe.minigame.bedwars.game;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class Timeline {

    private final List<Event> events;

    public Timeline(List<Event> events) {
        this.events = events.stream().sorted(Comparator.comparingInt(Event::at)).toList();
    }

    public List<Event> events() {
        return events;
    }

    public Optional<Upcoming> next(int elapsedSeconds) {
        for (Event event : events) {
            if (event.at() > elapsedSeconds) {
                return Optional.of(new Upcoming(event, event.at() - elapsedSeconds));
            }
        }
        return Optional.empty();
    }

    public List<Event> dueAt(int elapsedSeconds) {
        List<Event> due = new ArrayList<>();
        for (Event event : events) {
            if (event.at() == elapsedSeconds) {
                due.add(event);
            }
        }
        return due;
    }

    public enum Kind {
        DIAMOND_TIER,
        EMERALD_TIER,
        BEDS_DESTROYED,
        DRAGONS,
        GAME_END
    }

    public record Event(Kind kind, int tier, int at, String name) {

        public Event {
            if (at < 0) {
                throw new IllegalArgumentException("Event " + name + " cannot happen before the start");
            }
        }
    }

    public record Upcoming(Event event, int seconds) {

        public String clock() {
            return seconds / 60 + ":" + String.format("%02d", seconds % 60);
        }
    }
}
