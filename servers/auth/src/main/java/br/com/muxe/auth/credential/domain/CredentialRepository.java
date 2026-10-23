package br.com.muxe.auth.credential.domain;

import java.util.Optional;
import java.util.UUID;

public interface CredentialRepository {

    Optional<Credential> find(UUID playerId);

    boolean exists(UUID playerId);

    void insert(Credential credential);

    void update(Credential credential);
}
