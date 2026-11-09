package br.com.muxe.minigame.api.runtime;

import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.minigame.api.settings.MatchSettings;
import br.com.muxe.minigame.engine.team.PartyGroups;
import java.time.Clock;
import java.util.random.RandomGenerator;

public record MatchEnvironment(MatchSettings settings, MatchRules rules, MessageCatalog messages, Clock clock, RandomGenerator random, MatchEvents events,
                               TeamPreferences preferences, PartyGroups parties) {

    public MatchEnvironment(MatchSettings settings, MatchRules rules, MessageCatalog messages, Clock clock, RandomGenerator random, MatchEvents events,
                            TeamPreferences preferences) {
        this(settings, rules, messages, clock, random, events, preferences, PartyGroups.NONE);
    }
}
