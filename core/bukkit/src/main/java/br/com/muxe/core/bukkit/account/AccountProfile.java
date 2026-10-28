package br.com.muxe.core.bukkit.account;

import br.com.muxe.core.bukkit.permission.display.RankDisplay;
import br.com.muxe.core.common.account.domain.AccountDetails;
import br.com.muxe.core.common.permission.domain.Contexts;
import br.com.muxe.core.common.permission.domain.HeldRank;
import br.com.muxe.core.common.permission.domain.PermissionData;
import br.com.muxe.core.common.permission.domain.PermissionResolver;
import br.com.muxe.core.common.permission.domain.PermissionUser;
import br.com.muxe.core.common.permission.domain.ResolvedPermissions;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record AccountProfile(AccountDetails details, String name, RankDisplay display, List<HeldRank> ranks, Instant now) {

    public AccountProfile {
        Objects.requireNonNull(details, "details");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(display, "display");
        ranks = List.copyOf(ranks);
        Objects.requireNonNull(now, "now");
    }

    public static AccountProfile of(AccountDetails details, String fallbackName, PermissionUser user, PermissionData data, Instant now) {
        ResolvedPermissions resolved = PermissionResolver.resolve(data, user, Contexts.NONE, now);
        List<HeldRank> ranks = HeldRank.of(data, user, now);
        if (ranks.isEmpty()) {
            ranks = HeldRank.defaultRank(data, details.account().firstSeenAt()).stream().toList();
        }
        return new AccountProfile(
            details,
            details.account().currentUsername().orElse(fallbackName),
            RankDisplay.of(data, resolved, user.selectedTag()),
            ranks,
            now
        );
    }
}
