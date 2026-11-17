package br.com.muxe.core.common.ultraplus.domain;

import br.com.muxe.core.common.permission.domain.PermissionNode;
import br.com.muxe.core.common.permission.domain.PermissionUser;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public final class UltraPlusGrant {

    public static final String GROUP = "ultra+";
    public static final String NODE = PermissionNode.GROUP_PREFIX + GROUP;

    private static final long SECONDS_PER_DAY = Duration.ofDays(1).toSeconds();

    private UltraPlusGrant() {
    }

    public static int addedDays(PermissionUser before, PermissionUser after, Instant now) {
        return addedDays(node(before), node(after), now);
    }

    public static int addedDays(Optional<PermissionNode> before, Optional<PermissionNode> after, Instant now) {
        if (after.isEmpty() || !after.get().value() || after.get().expiresAt().isEmpty()) {
            return 0;
        }
        Instant start = now;
        Optional<PermissionNode> active = before.filter(node -> node.value() && node.isActive(now));
        if (active.isPresent()) {
            if (active.get().expiresAt().isEmpty()) {
                return 0;
            }
            start = active.get().expiresAt().get().isAfter(now) ? active.get().expiresAt().get() : now;
        }
        long seconds = Duration.between(start, after.get().expiresAt().get()).toSeconds();
        if (seconds <= 0) {
            return 0;
        }
        return (int) ((seconds + SECONDS_PER_DAY / 2) / SECONDS_PER_DAY);
    }

    public static boolean timed(PermissionUser user) {
        return node(user).filter(found -> found.value() && found.expiresAt().isPresent()).isPresent();
    }

    private static Optional<PermissionNode> node(PermissionUser user) {
        return user.nodes().stream().filter(node -> node.key().equals(NODE) && node.contexts().isEmpty()).findFirst();
    }
}
