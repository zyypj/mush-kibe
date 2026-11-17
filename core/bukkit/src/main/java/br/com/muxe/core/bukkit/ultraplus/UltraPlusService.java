package br.com.muxe.core.bukkit.ultraplus;

import br.com.muxe.core.bukkit.permission.PlayerRanks;
import br.com.muxe.core.bukkit.permission.display.RankDisplay;
import br.com.muxe.core.common.cqrs.command.CommandBus;
import br.com.muxe.core.common.cqrs.query.QueryBus;
import br.com.muxe.core.common.permission.domain.Group;
import br.com.muxe.core.common.permission.domain.PermissionUser;
import br.com.muxe.core.common.result.Result;
import br.com.muxe.core.common.ultraplus.application.command.ChangeUltraPlusStyleCommand;
import br.com.muxe.core.common.ultraplus.application.command.ClaimUltraPlusRewardCommand;
import br.com.muxe.core.common.ultraplus.application.query.FindUltraPlusQuery;
import br.com.muxe.core.common.ultraplus.domain.UltraPlusGrant;
import br.com.muxe.core.common.ultraplus.domain.UltraPlusProfile;
import br.com.muxe.core.common.ultraplus.domain.UltraPlusStyle;
import java.time.Instant;
import java.time.YearMonth;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class UltraPlusService implements Listener {

    public static final int LIFETIME_MONTHS = Integer.MAX_VALUE;

    private final UltraPlusSettings settings;
    private final PlayerRanks ranks;
    private final CommandBus commands;
    private final QueryBus queries;
    private final Executor mainThread;
    private final Consumer<Player> refresh;
    private final Logger logger;
    private final Map<UUID, UltraPlusProfile> online = new ConcurrentHashMap<>();

    public UltraPlusService(UltraPlusSettings settings, PlayerRanks ranks, CommandBus commands, QueryBus queries, Executor mainThread, Consumer<Player> refresh,
                            Logger logger) {
        this.settings = settings;
        this.ranks = ranks;
        this.commands = commands;
        this.queries = queries;
        this.mainThread = mainThread;
        this.refresh = refresh;
        this.logger = logger;
    }

    public UltraPlusSettings settings() {
        return settings;
    }

    public UltraPlusProfile of(UUID player) {
        return online.getOrDefault(player, UltraPlusProfile.empty(player));
    }

    public boolean member(UUID player) {
        return ranks.resolved(player).map(resolved -> resolved.groups().stream().map(Group::id).anyMatch(UltraPlusGrant.GROUP::equals)).orElse(false);
    }

    public int months(UUID player) {
        if (lifetime(player)) {
            return LIFETIME_MONTHS;
        }
        return of(player).months();
    }

    public UltraPlusPages.Viewer viewer(Player player) {
        UUID id = player.getUniqueId();
        return new UltraPlusPages.Viewer(player.getName(), member(id), months(id), of(id).style());
    }

    public RankDisplay style(UUID player, RankDisplay display) {
        if (!member(player)) {
            return display;
        }
        return UltraPlusLook.of(settings, of(player).style(), months(player)).apply(settings, display);
    }

    public YearMonth month(Instant now) {
        return UltraPlusPages.month(now, settings.zone());
    }

    public void change(Player player, UltraPlusStyle style) {
        UUID id = player.getUniqueId();
        online.compute(id, (ignored, current) -> (current == null ? UltraPlusProfile.empty(id) : current).withStyle(style));
        refresh.accept(player);
        commands.dispatch(new ChangeUltraPlusStyleCommand(id, style)).whenComplete((result, error) -> {
            if (error != null) {
                logger.log(Level.WARNING, "Could not save the Ultra+ style of " + player.getName() + ": " + error.getMessage());
            } else if (result instanceof Result.Failure<Void> failure) {
                logger.warning("Could not save the Ultra+ style of " + player.getName() + ": " + failure.problem().message());
            }
        });
    }

    public CompletableFuture<Result<Void>> claim(Player player, YearMonth month) {
        UUID id = player.getUniqueId();
        return commands.dispatch(new ClaimUltraPlusRewardCommand(id, month, settings.rewardCredits())).thenApply(result -> {
            if (result.isSuccess()) {
                online.compute(id, (ignored, current) -> (current == null ? UltraPlusProfile.empty(id) : current).claim(month));
            }
            return result;
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        queries.ask(new FindUltraPlusQuery(id)).whenComplete((profile, error) -> {
            if (error != null) {
                logger.log(Level.WARNING, "Could not load the Ultra+ profile of " + player.getName() + ": " + error.getMessage());
                return;
            }
            mainThread.execute(() -> {
                if (player.isOnline()) {
                    online.put(id, profile);
                    refresh.accept(player);
                }
            });
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        online.remove(event.getPlayer().getUniqueId());
    }

    private boolean lifetime(UUID player) {
        if (!member(player)) {
            return false;
        }
        Instant now = ranks.clock().instant();
        Optional<PermissionUser> user = ranks.user(player);
        return user.map(found -> found.groupNodes(now).stream()
            .filter(node -> node.groupId().equals(UltraPlusGrant.GROUP) && node.contexts().isEmpty())
            .noneMatch(node -> node.expiresAt().isPresent())).orElse(true);
    }
}
