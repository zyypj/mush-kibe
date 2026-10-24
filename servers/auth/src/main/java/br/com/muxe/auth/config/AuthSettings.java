package br.com.muxe.auth.config;

import br.com.muxe.auth.credential.crypto.Pbkdf2PasswordHasher;
import br.com.muxe.auth.credential.policy.PasswordPolicy;
import br.com.muxe.core.common.config.source.ConfigSource;
import java.time.Duration;

public record AuthSettings(
    int hashIterations,
    Duration loginTimeout,
    int maxAttempts,
    Duration attemptWindow,
    PasswordPolicy passwordPolicy,
    ChallengeSettings challenge,
    Duration reminderInterval,
    int voidLevel
) {

    public static AuthSettings from(ConfigSource source) {
        return new AuthSettings(
            source.integer("security.pbkdf2-iterations", Pbkdf2PasswordHasher.RECOMMENDED_ITERATIONS),
            source.duration("security.login-timeout", Duration.ofSeconds(60)),
            source.integer("security.max-attempts", 5),
            source.duration("security.attempt-window", Duration.ofMinutes(5)),
            new PasswordPolicy(
                source.integer("password.min-length", 6),
                source.integer("password.max-length", 64)
            ),
            new ChallengeSettings(
                source.bool("challenge.enabled", true),
                source.integer("challenge.length", 4),
                source.integer("challenge.max-attempts", 3),
                source.integer("challenge.distance", 7)
            ),
            source.duration("reminder.interval", Duration.ofSeconds(5)),
            source.integer("protection.void-level", 0)
        );
    }

    public record ChallengeSettings(boolean enabled, int length, int maxAttempts, int distance) {
    }
}
