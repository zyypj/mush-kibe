package br.com.muxe.core.common.customroom.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PresetRepository {

    List<RoomPreset> ofOwner(UUID owner, String game);

    Optional<RoomPreset> byCode(String code);

    boolean codeTaken(String code);

    void save(RoomPreset preset);

    boolean delete(UUID owner, UUID id);
}
