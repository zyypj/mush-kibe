package br.com.muxe.auth.credential.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.auth.credential.domain.PasswordHash;
import java.security.SecureRandom;
import org.junit.jupiter.api.Test;

class Pbkdf2PasswordHasherTest {

    private final Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher(1_000, new SecureRandom());

    @Test
    void matchesOnlyTheOriginalPassword() {
        PasswordHash hash = hasher.hash("senha-forte");

        assertThat(hasher.matches("senha-forte", hash)).isTrue();
        assertThat(hasher.matches("senha-fraca", hash)).isFalse();
    }

    @Test
    void saltsEveryHash() {
        assertThat(hasher.hash("same").encode()).isNotEqualTo(hasher.hash("same").encode());
    }

    @Test
    void survivesEncodingRoundTrips() {
        PasswordHash hash = hasher.hash("senha-forte");

        PasswordHash parsed = PasswordHash.parse(hash.encode());

        assertThat(hasher.matches("senha-forte", parsed)).isTrue();
        assertThat(parsed.encode()).startsWith("pbkdf2_sha256$1000$");
    }

    @Test
    void asksForRehashWhenIterationsGoUp() {
        PasswordHash weak = hasher.hash("senha-forte");

        assertThat(new Pbkdf2PasswordHasher(2_000, new SecureRandom()).needsRehash(weak)).isTrue();
        assertThat(hasher.needsRehash(weak)).isFalse();
    }
}
