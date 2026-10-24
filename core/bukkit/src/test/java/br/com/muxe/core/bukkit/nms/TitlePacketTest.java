package br.com.muxe.core.bukkit.nms;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class TitlePacketTest {

    @Test
    void aResetAloneLeavesTheSubtitleOnScreen() {
        ClientTitleScreen screen = new ClientTitleScreen();
        screen.receive(TitlePacket.showing("MUXE", "Use /registrar", 0, 120, 10));

        screen.receive(List.of(new TitlePacket.Reset()));

        assertThat(screen.visibleSubtitle()).isEqualTo("Use /registrar");
    }

    @Test
    void clearingHidesTitleAndSubtitleAtOnce() {
        ClientTitleScreen screen = new ClientTitleScreen();
        screen.receive(TitlePacket.showing("MUXE", "Use /registrar", 0, 120, 10));

        screen.receive(TitlePacket.clearing());

        assertThat(screen.visibleTitle()).isEmpty();
        assertThat(screen.visibleSubtitle()).isEmpty();
    }

    @Test
    void aTitleShownAfterClearingUsesItsOwnTimings() {
        ClientTitleScreen screen = new ClientTitleScreen();
        screen.receive(TitlePacket.showing("MUXE", "Use /logar", 0, 120, 10));
        screen.receive(TitlePacket.clearing());

        screen.receive(TitlePacket.showing("MUXE", "Use /logar", 0, 120, 10));

        assertThat(screen.visibleTitle()).isEqualTo("MUXE");
        assertThat(screen.visibleSubtitle()).isEqualTo("Use /logar");
        assertThat(screen.timer).isEqualTo(130);
    }

    @Test
    void showingSendsTimingsAndSubtitleBeforeTheTitle() {
        assertThat(TitlePacket.showing("a", "b", 1, 2, 3)).containsExactly(
            new TitlePacket.Times(1, 2, 3),
            new TitlePacket.Subtitle("b"),
            new TitlePacket.Main("a")
        );
    }

    private static final class ClientTitleScreen {

        private static final int DEFAULT_FADE_IN = 10;
        private static final int DEFAULT_STAY = 70;
        private static final int DEFAULT_FADE_OUT = 20;

        private String title = "";
        private String subtitle = "";
        private int fadeIn = DEFAULT_FADE_IN;
        private int stay = DEFAULT_STAY;
        private int fadeOut = DEFAULT_FADE_OUT;
        private int timer;

        private void receive(List<TitlePacket> packets) {
            for (TitlePacket packet : packets) {
                switch (packet) {
                    case TitlePacket.Main main -> display(main.text(), null, -1, -1, -1);
                    case TitlePacket.Subtitle sub -> display(null, sub.text(), -1, -1, -1);
                    case TitlePacket.Times times -> display(null, null, times.fadeIn(), times.stay(), times.fadeOut());
                    case TitlePacket.Reset reset -> {
                        display("", "", -1, -1, -1);
                        fadeIn = DEFAULT_FADE_IN;
                        stay = DEFAULT_STAY;
                        fadeOut = DEFAULT_FADE_OUT;
                    }
                }
            }
        }

        private void display(String newTitle, String newSubtitle, int newFadeIn, int newStay, int newFadeOut) {
            if (newTitle == null && newSubtitle == null && newFadeIn < 0 && newStay < 0 && newFadeOut < 0) {
                title = "";
                subtitle = "";
                timer = 0;
            } else if (newTitle != null) {
                title = newTitle;
                timer = fadeIn + stay + fadeOut;
            } else if (newSubtitle != null) {
                subtitle = newSubtitle;
            } else {
                if (newFadeIn >= 0) {
                    fadeIn = newFadeIn;
                }
                if (newStay >= 0) {
                    stay = newStay;
                }
                if (newFadeOut >= 0) {
                    fadeOut = newFadeOut;
                }
                if (timer > 0) {
                    timer = fadeIn + stay + fadeOut;
                }
            }
        }

        private String visibleTitle() {
            return timer > 0 ? title : "";
        }

        private String visibleSubtitle() {
            return timer > 0 ? subtitle : "";
        }
    }
}
