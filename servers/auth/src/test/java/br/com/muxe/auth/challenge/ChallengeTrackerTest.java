package br.com.muxe.auth.challenge;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.effect.PixelFont;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChallengeTrackerTest {

    private final ChallengeTracker tracker = new ChallengeTracker(new ChallengeCodes(new Random(42), 4), 3);
    private final UUID player = UUID.randomUUID();

    @Test
    void codesUseOnlyUnambiguousCharactersTheFontCanDraw() {
        ChallengeCodes codes = new ChallengeCodes(new Random(7), 6);

        for (int attempt = 0; attempt < 200; attempt++) {
            String code = codes.next();
            assertThat(code).hasSize(6).doesNotContain("0", "O", "1", "I");
            assertThat(PixelFont.standard().supportsAll(code)).isTrue();
        }
    }

    @Test
    void theRightCodePassesIgnoringCaseAndSpaces() {
        String code = tracker.start(player, true);

        ChallengeAnswer answer = tracker.answer(player, " " + code.toLowerCase() + " ");

        assertThat(answer.outcome()).isEqualTo(ChallengeAnswer.Outcome.PASSED);
        assertThat(answer.registered()).isTrue();
        assertThat(tracker.isChallenging(player)).isFalse();
    }

    @Test
    void aWrongCodeGeneratesANewOne() {
        String first = tracker.start(player, false);

        ChallengeAnswer answer = tracker.answer(player, "????");

        assertThat(answer.outcome()).isEqualTo(ChallengeAnswer.Outcome.WRONG);
        assertThat(answer.remainingAttempts()).isEqualTo(2);
        assertThat(tracker.code(player)).isPresent().hasValueSatisfying(code -> assertThat(code).isNotEqualTo(first));
    }

    @Test
    void failsAfterTheMaximumNumberOfWrongAnswers() {
        tracker.start(player, false);
        tracker.answer(player, "????");
        tracker.answer(player, "????");

        ChallengeAnswer answer = tracker.answer(player, "????");

        assertThat(answer.outcome()).isEqualTo(ChallengeAnswer.Outcome.FAILED);
        assertThat(tracker.isChallenging(player)).isFalse();
    }

    @Test
    void ignoresPlayersWithoutAChallenge() {
        assertThat(tracker.answer(player, "ABCD").outcome()).isEqualTo(ChallengeAnswer.Outcome.NOT_IN_CHALLENGE);
    }

    @Test
    void endingRemovesTheChallenge() {
        tracker.start(player, true);

        tracker.end(player);

        assertThat(tracker.code(player)).isEmpty();
    }
}
