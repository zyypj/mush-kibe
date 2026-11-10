package br.com.muxe.minigame.api.cosmetic.message;

import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.core.common.language.domain.Text;
import br.com.muxe.minigame.api.cosmetic.message.LifetimeTally.Counter;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.function.Function;

public final class KillAnnouncer {

    private final Localized<MessageSets> sets;
    private final Function<UUID, MessageSet> chosen;
    private final LifetimeTally tally;

    public KillAnnouncer(MessageSets sets, Function<UUID, MessageSet> chosen, LifetimeTally tally) {
        this(Localized.same(sets), chosen, tally);
    }

    public KillAnnouncer(Localized<MessageSets> sets, Function<UUID, MessageSet> chosen, LifetimeTally tally) {
        this.sets = sets;
        this.chosen = chosen;
        this.tally = tally;
    }

    public String kill(UUID killer, String killerName, UUID victim, String victimName, boolean finalKill, boolean voidDeath) {
        return killText(killer, killerName, victim, victimName, finalKill, voidDeath).in(Language.DEFAULT);
    }

    public String death(UUID victim, String victimName, boolean finalDeath, boolean voidDeath) {
        return deathText(victim, victimName, finalDeath, voidDeath).in(Language.DEFAULT);
    }

    public String bed(UUID breaker, String breakerName, String teamColor, String team) {
        return bedText(breaker, breakerName, teamColor, Text.plain(team)).in(Language.DEFAULT);
    }

    public Text killText(UUID killer, String killerName, UUID victim, String victimName, boolean finalKill, boolean voidDeath) {
        OptionalLong kills = tally.record(killer, finalKill ? Counter.FINAL_KILLS : Counter.KILLS);
        tally.record(victim, finalKill ? Counter.FINAL_DEATHS : Counter.DEATHS);
        String set = chosen.apply(killer).id();
        MessageValues values = MessageValues.kill(victimName, killerName, kills);
        return language -> render(language, set, MessageKind.kill(finalKill, voidDeath), values);
    }

    public Text deathText(UUID victim, String victimName, boolean finalDeath, boolean voidDeath) {
        OptionalLong deaths = tally.record(victim, finalDeath ? Counter.FINAL_DEATHS : Counter.DEATHS);
        String set = chosen.apply(victim).id();
        MessageValues values = MessageValues.death(victimName, deaths);
        return language -> render(language, set, MessageKind.death(finalDeath, voidDeath), values);
    }

    public Text bedText(UUID breaker, String breakerName, String teamColor, Text team) {
        OptionalLong beds = tally.record(breaker, Counter.BEDS);
        String set = chosen.apply(breaker).id();
        return language -> sets.in(language).bedPrefix()
            + render(language, set, MessageKind.BED, MessageValues.bed(breakerName, teamColor, team.in(language), beds));
    }

    private String render(Language language, String set, MessageKind kind, MessageValues values) {
        MessageSets shown = sets.in(language);
        return shown.render(shown.find(set).orElse(shown.fallback()), kind, values);
    }
}
