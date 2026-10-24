package br.com.muxe.auth.challenge;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChallengeTracker {

    private final ChallengeCodes codes;
    private final int maxAttempts;
    private final Map<UUID, Challenge> challenges = new ConcurrentHashMap<>();

    public ChallengeTracker(ChallengeCodes codes, int maxAttempts) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be positive");
        }
        this.codes = codes;
        this.maxAttempts = maxAttempts;
    }

    public String start(UUID playerId, boolean registered) {
        Challenge challenge = new Challenge(codes.next(), registered, maxAttempts);
        challenges.put(playerId, challenge);
        return challenge.code();
    }

    public ChallengeAnswer answer(UUID playerId, String text) {
        Challenge challenge = challenges.get(playerId);
        if (challenge == null) {
            return new ChallengeAnswer(ChallengeAnswer.Outcome.NOT_IN_CHALLENGE, false, 0);
        }
        if (normalize(text).equals(challenge.code())) {
            challenges.remove(playerId);
            return new ChallengeAnswer(ChallengeAnswer.Outcome.PASSED, challenge.registered(), challenge.attemptsLeft());
        }
        int remaining = challenge.attemptsLeft() - 1;
        if (remaining <= 0) {
            challenges.remove(playerId);
            return new ChallengeAnswer(ChallengeAnswer.Outcome.FAILED, challenge.registered(), 0);
        }
        challenges.put(playerId, new Challenge(codes.next(), challenge.registered(), remaining));
        return new ChallengeAnswer(ChallengeAnswer.Outcome.WRONG, challenge.registered(), remaining);
    }

    public Optional<String> code(UUID playerId) {
        return Optional.ofNullable(challenges.get(playerId)).map(Challenge::code);
    }

    public boolean isChallenging(UUID playerId) {
        return challenges.containsKey(playerId);
    }

    public void end(UUID playerId) {
        challenges.remove(playerId);
    }

    private static String normalize(String text) {
        return text == null ? "" : text.trim().replace(" ", "").toUpperCase(Locale.ROOT);
    }

    private record Challenge(String code, boolean registered, int attemptsLeft) {
    }
}
