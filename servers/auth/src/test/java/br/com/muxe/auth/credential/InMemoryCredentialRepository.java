package br.com.muxe.auth.credential;

import br.com.muxe.auth.credential.domain.Credential;
import br.com.muxe.auth.credential.domain.CredentialRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryCredentialRepository implements CredentialRepository {

    private final Map<UUID, Credential> credentials = new ConcurrentHashMap<>();

    @Override
    public Optional<Credential> find(UUID playerId) {
        return Optional.ofNullable(credentials.get(playerId));
    }

    @Override
    public boolean exists(UUID playerId) {
        return credentials.containsKey(playerId);
    }

    @Override
    public void insert(Credential credential) {
        if (credentials.putIfAbsent(credential.playerId(), credential) != null) {
            throw new IllegalStateException("Duplicate credential");
        }
    }

    @Override
    public void update(Credential credential) {
        credentials.replace(credential.playerId(), credential);
    }
}
