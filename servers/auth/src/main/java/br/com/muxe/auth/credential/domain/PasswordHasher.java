package br.com.muxe.auth.credential.domain;

public interface PasswordHasher {

    PasswordHash hash(String password);

    boolean matches(String password, PasswordHash stored);

    boolean needsRehash(PasswordHash stored);
}
