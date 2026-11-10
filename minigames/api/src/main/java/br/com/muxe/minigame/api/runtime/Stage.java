package br.com.muxe.minigame.api.runtime;

import br.com.muxe.core.bukkit.effect.SoundCue;
import br.com.muxe.core.bukkit.text.Title;
import br.com.muxe.core.common.combat.knockback.KnockbackProfile;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.minigame.api.settings.HotbarItem;
import br.com.muxe.minigame.engine.setup.Point;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public interface Stage {

    World world();

    Location location(Point point);

    Optional<Player> player(UUID id);

    List<UUID> present(Collection<UUID> ids);

    String rankColored(UUID id, String name);

    Language language(UUID id);

    void message(UUID id, String text);

    void centered(UUID id, List<String> lines);

    void title(UUID id, Title title);

    void sound(UUID id, Optional<SoundCue> cue);

    void prepare(UUID id, Look look);

    void teleport(UUID id, Point point);

    void teleportOut(UUID id);

    void knockUp(UUID id, double speed);

    void raiseTo(UUID id, Point floor);

    void waitingItems(UUID id, List<HotbarItem> items);

    void spectatorItems(UUID id, List<HotbarItem> items);

    void showBoard(UUID id);

    void board(UUID id, String title, List<String> lines);

    void dropBoard(UUID id);

    void health(Collection<UUID> viewers, Collection<UUID> alive);

    void refreshNameTag(UUID id);

    void visibility(Collection<UUID> online, Predicate<UUID> spectator);

    void sendTo(UUID id, String group);

    void erase(Point first, Point second);

    void endTitle(UUID id, boolean won);

    void rememberReturn(UUID id, Duration timeToLive);

    void forgetReturn(UUID id);

    void knockback(UUID id, Optional<KnockbackProfile> profile);

    enum Look {
        PLAYER,
        SPECTATOR
    }
}
