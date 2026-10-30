package br.com.muxe.minigame.engine.room;

import java.util.Set;
import java.util.random.RandomGenerator;

public final class RoomCodes {

    public static final int LENGTH = 6;

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz";

    private final RandomGenerator random;

    public RoomCodes(RandomGenerator random) {
        this.random = random;
    }

    public String next(Set<String> taken) {
        while (true) {
            StringBuilder code = new StringBuilder(LENGTH);
            for (int index = 0; index < LENGTH; index++) {
                code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
            if (Character.isDigit(code.charAt(0)) && !taken.contains(code.toString())) {
                return code.toString();
            }
        }
    }
}
