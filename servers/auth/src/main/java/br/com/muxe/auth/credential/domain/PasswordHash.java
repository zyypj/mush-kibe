package br.com.muxe.auth.credential.domain;

import java.util.Base64;
import java.util.Objects;

public record PasswordHash(String algorithm, int iterations, byte[] salt, byte[] hash) {

    private static final String SEPARATOR = "$";

    public PasswordHash {
        Objects.requireNonNull(algorithm, "algorithm");
        Objects.requireNonNull(salt, "salt");
        Objects.requireNonNull(hash, "hash");
        if (iterations < 1) {
            throw new IllegalArgumentException("Iterations must be positive");
        }
        salt = salt.clone();
        hash = hash.clone();
    }

    public static PasswordHash parse(String encoded) {
        String[] parts = encoded.split("\\$");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Unknown password hash format");
        }
        Base64.Decoder decoder = Base64.getDecoder();
        return new PasswordHash(parts[0], Integer.parseInt(parts[1]), decoder.decode(parts[2]), decoder.decode(parts[3]));
    }

    public String encode() {
        Base64.Encoder encoder = Base64.getEncoder().withoutPadding();
        return algorithm + SEPARATOR + iterations + SEPARATOR + encoder.encodeToString(salt) + SEPARATOR + encoder.encodeToString(hash);
    }

    @Override
    public byte[] salt() {
        return salt.clone();
    }

    @Override
    public byte[] hash() {
        return hash.clone();
    }

    @Override
    public String toString() {
        return "PasswordHash[" + algorithm + ", iterations=" + iterations + "]";
    }
}
