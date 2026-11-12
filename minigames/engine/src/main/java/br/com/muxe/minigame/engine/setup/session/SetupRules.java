package br.com.muxe.minigame.engine.setup.session;

import java.util.Objects;

public final class SetupRules {

    public static final double VOID_Y = -60;

    private SetupRules() {
    }

    public static boolean hears(String speakerArena, String listenerArena) {
        return Objects.equals(speakerArena, listenerArena);
    }

    public static boolean fellIntoVoid(double y) {
        return y < VOID_Y;
    }
}
