package br.com.muxe.auth.challenge;

public record ChallengeAnswer(Outcome outcome, boolean registered, int remainingAttempts) {

    public enum Outcome {
        PASSED,
        WRONG,
        FAILED,
        NOT_IN_CHALLENGE
    }
}
