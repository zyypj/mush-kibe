package br.com.muxe.core.common.customroom.domain;

import java.util.Locale;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

public final class PresetCodes {

    public static final int LENGTH = 8;

    private static final String ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789";

    private final RandomGenerator random;

    public PresetCodes(RandomGenerator random) {
        this.random = random;
    }

    public String next(Predicate<String> taken) {
        while (true) {
            StringBuilder code = new StringBuilder(LENGTH);
            for (int index = 0; index < LENGTH; index++) {
                code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
            if (!taken.test(code.toString())) {
                return code.toString();
            }
        }
    }

    public static String normalize(String typed) {
        return typed.trim().toLowerCase(Locale.ROOT);
    }
}
