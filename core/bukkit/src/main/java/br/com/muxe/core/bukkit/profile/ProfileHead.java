package br.com.muxe.core.bukkit.profile;

import br.com.muxe.core.bukkit.permission.PlayerRanks;
import br.com.muxe.core.common.permission.domain.HeldRank;
import br.com.muxe.core.common.permission.domain.PermissionUser;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record ProfileHead(String player, String displayName, Optional<Instant> firstSeenAt, List<HeldRank> ranks) {

    public ProfileHead {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(firstSeenAt, "firstSeenAt");
        ranks = List.copyOf(ranks);
    }

    public static ProfileHead of(PlayerRanks ranks, UUID id, String player, Optional<Instant> firstSeenAt) {
        PermissionUser user = ranks.user(id).orElse(PermissionUser.empty(id));
        return new ProfileHead(
            player,
            ranks.display(id).formatName(player),
            firstSeenAt,
            HeldRank.of(ranks.data(), user, ranks.clock().instant())
        );
    }
}
