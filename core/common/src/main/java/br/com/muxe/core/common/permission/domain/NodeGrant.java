package br.com.muxe.core.common.permission.domain;

import br.com.muxe.core.common.result.Result;
import java.time.Instant;
import java.util.Optional;

public final class NodeGrant {

    public static final String ALREADY_PERMANENT = "permission.already-permanent";

    private NodeGrant() {
    }

    public static Result<PermissionNode> grant(PermissionUser user, String key, Contexts contexts, Lifetime lifetime, Instant now) {
        Optional<PermissionNode> current = user.nodes().stream()
            .filter(node -> node.key().equals(key) && node.contexts().equals(contexts))
            .findFirst();
        boolean permanent = current.filter(node -> node.value() && node.isActive(now) && node.expiresAt().isEmpty()).isPresent();
        if (permanent) {
            return Result.failure(ALREADY_PERMANENT, "The user already has " + key + " for good");
        }
        return Result.success(new PermissionNode(key, true, contexts, lifetime.expiryAfter(current, now), now, now));
    }
}
