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
    PasswordPolicy passwordPolicy
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
            )
        );
    }
}
