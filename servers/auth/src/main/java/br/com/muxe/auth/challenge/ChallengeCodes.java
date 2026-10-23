package br.com.muxe.auth.challenge;

import java.util.random.RandomGenerator;

public final class ChallengeCodes {

    public static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final RandomGenerator random;
    private final int length;

    public ChallengeCodes(RandomGenerator random, int length) {
        if (length < 1) {
            throw new IllegalArgumentException("Challenge codes need at least one character");
        }
        this.random = random;
        this.length = length;
    }

    public String next() {
        StringBuilder code = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
