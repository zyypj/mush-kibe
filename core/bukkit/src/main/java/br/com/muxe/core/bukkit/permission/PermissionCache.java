package br.com.muxe.core.bukkit.permission;

import br.com.muxe.core.common.permission.domain.Contexts;
import br.com.muxe.core.common.permission.domain.PermissionData;
import br.com.muxe.core.common.permission.domain.PermissionResolver;
import br.com.muxe.core.common.permission.domain.PermissionUser;
import br.com.muxe.core.common.permission.domain.ResolvedPermissions;
import br.com.muxe.core.common.permission.domain.Tristate;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class PermissionCache {

    private final Supplier<PermissionData> data;
    private final String serverGroup;
    private final Clock clock;
    private final Map<UUID, Entry> entries = new ConcurrentHashMap<>();

    public PermissionCache(Supplier<PermissionData> data, String serverGroup, Clock clock) {
        this.data = data;
        this.serverGroup = serverGroup;
        this.clock = clock;
    }

    public ResolvedPermissions put(PermissionUser user, String world) {
        Entry entry = resolve(user, world);
        entries.put(user.id(), entry);
        return entry.resolved();
    }

    public Optional<ResolvedPermissions> moveTo(UUID player, String world) {
        return Optional.ofNullable(entries.computeIfPresent(player, (id, entry) -> resolve(entry.user(), world))).map(Entry::resolved);
    }

    public Set<UUID> refreshAll() {
        entries.replaceAll((id, entry) -> resolve(entry.user(), entry.world()));
        return Set.copyOf(entries.keySet());
    }

    public List<UUID> refreshExpired() {
        Instant now = clock.instant();
        List<UUID> expired = entries.entrySet().stream()
            .filter(entry -> entry.getValue().resolved().nextExpiry().filter(at -> !now.isBefore(at)).isPresent())
            .map(Map.Entry::getKey)
            .toList();
        expired.forEach(id -> entries.computeIfPresent(id, (ignored, entry) -> resolve(entry.user(), entry.world())));
        return expired;
    }

    public Optional<ResolvedPermissions> resolved(UUID player) {
        return Optional.ofNullable(entries.get(player)).map(Entry::resolved);
    }

    public Optional<PermissionUser> user(UUID player) {
        return Optional.ofNullable(entries.get(player)).map(Entry::user);
    }

    public Optional<String> world(UUID player) {
        return Optional.ofNullable(entries.get(player)).map(Entry::world);
    }

    public Tristate check(UUID player, String permission) {
        Entry entry = entries.get(player);
        return entry == null ? Tristate.UNDEFINED : entry.resolved().check(permission);
    }

    public void remove(UUID player) {
        entries.remove(player);
    }

    public Contexts contexts(String world) {
        try {
            return Contexts.server(serverGroup, world);
        } catch (IllegalArgumentException invalidWorld) {
            return Contexts.of(Contexts.SERVER, serverGroup);
        }
    }

    private Entry resolve(PermissionUser user, String world) {
        return new Entry(user, world, PermissionResolver.resolve(data.get(), user, contexts(world), clock.instant()));
    }

    private record Entry(PermissionUser user, String world, ResolvedPermissions resolved) {
    }
}
