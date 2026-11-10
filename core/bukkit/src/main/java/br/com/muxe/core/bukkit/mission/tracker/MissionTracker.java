package br.com.muxe.core.bukkit.mission.tracker;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.mission.announce.MissionTexts;
import br.com.muxe.core.bukkit.mission.config.MissionConfig;
import br.com.muxe.core.bukkit.text.ChatButton;
import br.com.muxe.core.bukkit.text.ClickableText;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.core.common.mission.application.command.RecordMissionProgressCommand;
import br.com.muxe.core.common.mission.application.query.FindMissionsQuery;
import br.com.muxe.core.common.mission.domain.Mission;
import br.com.muxe.core.common.mission.domain.MissionBoard;
import br.com.muxe.core.common.mission.domain.MissionSession;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class MissionTracker {

    private final MuxeCore core;
    private final String game;
    private final Localized<MissionConfig> configs;
    private final String label;
    private final Logger logger;
    private final Map<UUID, MissionSession> sessions = new HashMap<>();
    private final Map<UUID, CompletableFuture<Void>> saving = new HashMap<>();

    public MissionTracker(MuxeCore core, String game, MissionConfig config, String label, Logger logger) {
        this(core, game, Localized.same(config), label, logger);
    }

    public MissionTracker(MuxeCore core, String game, Localized<MissionConfig> configs, String label, Logger logger) {
        this.core = core;
        this.game = game;
        this.configs = configs;
        this.label = label;
        this.logger = logger;
    }

    public void begin(Collection<UUID> players) {
        for (UUID id : players) {
            core.queries().ask(new FindMissionsQuery(game, id)).whenCompleteAsync((board, error) -> {
                if (error != null) {
                    logger.log(Level.WARNING, "Could not load the missions of " + id + " in " + label, error);
                    return;
                }
                sessions.putIfAbsent(id, new MissionSession(board));
            }, core.mainThread());
        }
    }

    public void check(Function<UUID, Optional<Map<String, Long>>> totals) {
        sessions.forEach((id, session) -> totals.apply(id).ifPresent(current -> {
            List<Mission> completed = session.update(current);
            if (completed.isEmpty()) {
                return;
            }
            Player online = Bukkit.getPlayer(id);
            for (Mission mission : completed) {
                entry(session, mission).ifPresent(entry -> {
                    if (online != null) {
                        announce(online, MissionTexts.completion(configOf(online), entry));
                    }
                });
            }
            save(id, session);
        }));
    }

    public void finish(Function<UUID, Optional<Map<String, Long>>> totals) {
        check(totals);
        sessions.forEach((id, session) -> {
            Player online = Bukkit.getPlayer(id);
            if (online != null) {
                MissionTexts.summary(configOf(online), session.changes()).forEach(line -> online.sendMessage(Colors.translate(line)));
            }
            save(id, session);
        });
        sessions.clear();
    }

    private MissionConfig configOf(Player player) {
        return configs.in(core.languages().of(player));
    }

    private void announce(Player player, MissionTexts.Completion completion) {
        ClickableText.send(player, ChatButton.text(completion.prefix()), ChatButton.text(completion.name(), completion.hover().toArray(String[]::new)));
        completion.rewards().forEach(line -> player.sendMessage(Colors.translate(line)));
    }

    private void save(UUID id, MissionSession session) {
        Map<String, Long> amounts = session.drain();
        if (amounts.isEmpty()) {
            return;
        }
        CompletableFuture<Void> previous = saving.getOrDefault(id, CompletableFuture.completedFuture(null));
        CompletableFuture<Void> next = previous
            .thenCompose(ignored -> core.commands().dispatch(new RecordMissionProgressCommand(game, id, amounts)))
            .thenAccept(result -> {
                if (!result.isSuccess()) {
                    logger.warning("Could not save the missions of " + id + " in " + label);
                }
            })
            .exceptionally(error -> {
                logger.log(Level.WARNING, "Could not save the missions of " + id + " in " + label, error);
                return null;
            });
        saving.put(id, next);
    }

    private static Optional<MissionBoard.Entry> entry(MissionSession session, Mission mission) {
        return session.board().entries().stream().filter(entry -> entry.mission().id().equals(mission.id())).findFirst();
    }
}
