package br.com.muxe.core.bukkit.permission;

import br.com.muxe.core.bukkit.permission.display.RankDisplay;
import br.com.muxe.core.common.permission.domain.PermissionData;
import br.com.muxe.core.common.permission.domain.PermissionUser;
import br.com.muxe.core.common.permission.domain.ResolvedPermissions;
import br.com.muxe.core.common.permission.sync.PermissionSync;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

public interface PlayerRanks {

    PermissionSync sync();

    PermissionData data();

    Clock clock();

    Optional<ResolvedPermissions> resolved(UUID player);

    Optional<PermissionUser> user(UUID player);

    RankDisplay display(UUID player);
}
