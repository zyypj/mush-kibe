package br.com.muxe.auth.credential.crypto;

import br.com.muxe.auth.credential.domain.PasswordHash;
import br.com.muxe.auth.credential.domain.PasswordHasher;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class Pbkdf2PasswordHasher implements PasswordHasher {

    public static final String ALGORITHM_ID = "pbkdf2_sha256";
    public static final int RECOMMENDED_ITERATIONS = 600_000;

    private static final String JCA_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;

    private final int iterations;
    private final SecureRandom random;

    public Pbkdf2PasswordHasher(int iterations, SecureRandom random) {
        if (iterations < 1) {
            throw new IllegalArgumentException("Iterations must be positive");
        }
        this.iterations = iterations;
        this.random = random;
    }

    @Override
    public PasswordHash hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        return new PasswordHash(ALGORITHM_ID, iterations, salt, derive(password, salt, iterations));
    }

    @Override
    public boolean matches(String password, PasswordHash stored) {
        if (!ALGORITHM_ID.equals(stored.algorithm())) {
            return false;
        }
        byte[] candidate = derive(password, stored.salt(), stored.iterations());
        return MessageDigest.isEqual(candidate, stored.hash());
    }

    @Override
    public boolean needsRehash(PasswordHash stored) {
        return !ALGORITHM_ID.equals(stored.algorithm()) || stored.iterations() < iterations;
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(JCA_ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("PBKDF2 is not available", exception);
        } finally {
            spec.clearPassword();
        }
    }
}
