package br.com.muxe.minigame.api.runtime;

import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.core.common.language.domain.Text;
import br.com.muxe.minigame.api.settings.MatchSettings;
import br.com.muxe.minigame.engine.mode.GameMode;
import br.com.muxe.minigame.engine.team.PartyGroups;
import java.time.Clock;
import java.util.random.RandomGenerator;

public record MatchEnvironment(Localized<MatchSettings> localized, MatchRules rules, MessageCatalog messages, Clock clock, RandomGenerator random, MatchEvents events,
                               TeamPreferences preferences, PartyGroups parties, KnockbackLookup knockback) {

    public MatchEnvironment(MatchSettings settings, MatchRules rules, MessageCatalog messages, Clock clock, RandomGenerator random, MatchEvents events,
                            TeamPreferences preferences, PartyGroups parties, KnockbackLookup knockback) {
        this(Localized.same(settings), rules, messages, clock, random, events, preferences, parties, knockback);
    }

    public MatchEnvironment(MatchSettings settings, MatchRules rules, MessageCatalog messages, Clock clock, RandomGenerator random, MatchEvents events,
                            TeamPreferences preferences, PartyGroups parties) {
        this(settings, rules, messages, clock, random, events, preferences, parties, KnockbackLookup.NONE);
    }

    public MatchEnvironment(MatchSettings settings, MatchRules rules, MessageCatalog messages, Clock clock, RandomGenerator random, MatchEvents events,
                            TeamPreferences preferences) {
        this(settings, rules, messages, clock, random, events, preferences, PartyGroups.NONE);
    }

    public MatchEnvironment(Localized<MatchSettings> localized, MatchRules rules, MessageCatalog messages, Clock clock, RandomGenerator random, MatchEvents events,
                            TeamPreferences preferences) {
        this(localized, rules, messages, clock, random, events, preferences, PartyGroups.NONE, KnockbackLookup.NONE);
    }

    public MatchSettings settings() {
        return localized.standard();
    }

    public MatchSettings settings(Language language) {
        return localized.in(language);
    }

    public Text modeName(GameMode mode) {
        return language -> localized.in(language).modeName(mode);
    }
}
