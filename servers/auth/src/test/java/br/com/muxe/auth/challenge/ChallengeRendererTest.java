package br.com.muxe.auth.challenge;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.hologram.SpawnedHologram;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

class ChallengeRendererTest {

    private final ChallengeTracker tracker = new ChallengeTracker(new ChallengeCodes(new Random(1), 4), 3);
    private final RecordingScreen screen = new RecordingScreen();
    private final ChallengeRenderer renderer = new ChallengeRenderer(tracker, screen);
    private final UUID player = UUID.randomUUID();
    private final Location anchor = new Location(null, 1, 2, 3);

    @Test
    void theCodeIsShownOnceUntilItChanges() {
        String code = tracker.start(player, false);
        renderer.track(player, anchor);

        renderer.run();
        renderer.run();

        assertThat(screen.shown).containsExactly(code);
    }

    @Test
    void aNewCodeReplacesTheOldHologram() {
        String first = tracker.start(player, false);
        renderer.track(player, anchor);
        renderer.run();

        tracker.answer(player, "wrong");
        String second = tracker.code(player).orElseThrow();
        renderer.run();

        assertThat(screen.shown).containsExactly(first, second);
        assertThat(screen.holograms.getFirst().removed).isTrue();
        assertThat(screen.holograms.getLast().removed).isFalse();
    }

    @Test
    void theHologramIsResentPeriodically() {
        tracker.start(player, false);
        renderer.track(player, anchor);

        for (int run = 0; run <= ChallengeRenderer.RESEND_EVERY_RUNS; run++) {
            renderer.run();
        }

        assertThat(screen.holograms).hasSize(1);
        assertThat(screen.holograms.getFirst().resends).isEqualTo(1);
    }

    @Test
    void untrackingRemovesTheHologram() {
        tracker.start(player, false);
        renderer.track(player, anchor);
        renderer.run();

        renderer.untrack(player);
        renderer.run();

        assertThat(screen.holograms.getFirst().removed).isTrue();
        assertThat(screen.shown).hasSize(1);
    }

    @Test
    void aPassedChallengeRemovesTheHologram() {
        String code = tracker.start(player, false);
        renderer.track(player, anchor);
        renderer.run();

        tracker.answer(player, code);
        renderer.run();

        assertThat(screen.holograms.getFirst().removed).isTrue();
    }

    @Test
    void anOfflinePlayerIsRetriedLater() {
        String code = tracker.start(player, false);
        renderer.track(player, anchor);
        screen.online = false;
        renderer.run();

        screen.online = true;
        renderer.run();

        assertThat(screen.shown).containsExactly(code);
    }

    private static final class RecordingScreen implements ChallengeScreen {

        private final List<String> shown = new ArrayList<>();
        private final List<RecordingHologram> holograms = new ArrayList<>();
        private boolean online = true;

        @Override
        public Optional<SpawnedHologram> show(UUID playerId, Location anchor, String code) {
            if (!online) {
                return Optional.empty();
            }
            shown.add(code);
            RecordingHologram hologram = new RecordingHologram();
            holograms.add(hologram);
            return Optional.of(hologram);
        }
    }

    private static final class RecordingHologram implements SpawnedHologram {

        private int resends;
        private boolean removed;

        @Override
        public void resend() {
            resends++;
        }

        @Override
        public void remove() {
            removed = true;
        }
    }
}
