package br.com.muxe.core.common.preference.domain;

import java.time.Instant;
import java.util.UUID;

public interface PreferenceRepository {

    Preferences find(UUID playerId);

    void save(UUID playerId, Preference preference, String value, Instant updatedAt);
}
