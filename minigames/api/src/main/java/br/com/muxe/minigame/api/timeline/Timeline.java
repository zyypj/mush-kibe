package br.com.muxe.minigame.api.timeline;

import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.Text;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class Timeline<E> {

    private final List<Entry<E>> entries;

    public Timeline(List<Entry<E>> entries) {
        this.entries = entries.stream().sorted(Comparator.comparingInt(Entry::at)).toList();
    }

    public static <E> Timeline<E> empty() {
        return new Timeline<>(List.of());
    }

    public List<Entry<E>> entries() {
        return entries;
    }

    public Optional<Upcoming> next(int elapsedSeconds) {
        for (Entry<E> entry : entries) {
            if (entry.at() > elapsedSeconds) {
                return Optional.of(new Upcoming(entry.label(), entry.at() - elapsedSeconds));
            }
        }
        return Optional.empty();
    }

    public List<E> dueAt(int elapsedSeconds) {
        List<E> due = new ArrayList<>();
        for (Entry<E> entry : entries) {
            if (entry.at() == elapsedSeconds) {
                due.add(entry.event());
            }
        }
        return due;
    }

    public record Entry<E>(E event, int at, Text label) {

        public Entry {
            Objects.requireNonNull(label, "label");
            if (at < 0) {
                throw new IllegalArgumentException("Event " + label.in(Language.DEFAULT) + " cannot happen before the start");
            }
        }

        public Entry(E event, int at, String name) {
            this(event, at, Text.plain(name));
        }

        public String name() {
            return label.in(Language.DEFAULT);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Entry<?> entry && Objects.equals(event, entry.event) && at == entry.at && rendered(label).equals(rendered(entry.label));
        }

        @Override
        public int hashCode() {
            return Objects.hash(event, at, rendered(label));
        }

        @Override
        public String toString() {
            return "Entry[event=" + event + ", at=" + at + ", label=" + rendered(label) + "]";
        }
    }

    public record Upcoming(Text label, int seconds) {

        public Upcoming(String name, int seconds) {
            this(Text.plain(name), seconds);
        }

        public String name() {
            return label.in(Language.DEFAULT);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Upcoming upcoming && seconds == upcoming.seconds && rendered(label).equals(rendered(upcoming.label));
        }

        @Override
        public int hashCode() {
            return Objects.hash(rendered(label), seconds);
        }

        @Override
        public String toString() {
            return "Upcoming[label=" + rendered(label) + ", seconds=" + seconds + "]";
        }

        public String clock() {
            return seconds / 60 + ":" + String.format("%02d", seconds % 60);
        }
    }

    private static Map<Language, String> rendered(Text text) {
        Map<Language, String> rendered = new EnumMap<>(Language.class);
        for (Language language : Language.values()) {
            rendered.put(language, text.in(language));
        }
        return rendered;
    }
}
