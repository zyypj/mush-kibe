package br.com.muxe.auth.challenge;

import br.com.muxe.core.bukkit.hologram.SpawnedHologram;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;

public final class ChallengeRenderer implements Runnable {

    public static final long PERIOD_TICKS = 5L;
    public static final int RESEND_EVERY_RUNS = 12;

    private final ChallengeTracker tracker;
    private final ChallengeScreen screen;
    private final Map<UUID, Display> displays = new ConcurrentHashMap<>();

    public ChallengeRenderer(ChallengeTracker tracker, ChallengeScreen screen) {
        this.tracker = tracker;
        this.screen = screen;
    }

    public void track(UUID playerId, Location anchor) {
        Display previous = displays.put(playerId, new Display(anchor.clone()));
        if (previous != null) {
            previous.clear();
        }
    }

    public void untrack(UUID playerId) {
        Display display = displays.remove(playerId);
        if (display != null) {
            display.clear();
        }
    }

    @Override
    public void run() {
        displays.forEach((playerId, display) -> display.render(playerId, tracker.code(playerId)));
    }

    private final class Display {

        private final Location anchor;
        private String shownCode;
        private SpawnedHologram hologram;
        private int runs;

        private Display(Location anchor) {
            this.anchor = anchor;
        }

        private void render(UUID playerId, Optional<String> code) {
            if (code.isEmpty()) {
                clear();
                return;
            }
            if (hologram == null || !code.get().equals(shownCode)) {
                clear();
                screen.show(playerId, anchor, code.get()).ifPresent(shown -> {
                    hologram = shown;
                    shownCode = code.get();
                });
                return;
            }
            if (++runs % RESEND_EVERY_RUNS == 0) {
                hologram.resend();
            }
        }

        private void clear() {
            if (hologram != null) {
                hologram.remove();
            }
            hologram = null;
            shownCode = null;
            runs = 0;
        }
    }
}
