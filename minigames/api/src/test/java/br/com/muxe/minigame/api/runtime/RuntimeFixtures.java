package br.com.muxe.minigame.api.runtime;

import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.LanguageLookup;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.core.common.testing.MutableClock;
import br.com.muxe.minigame.api.settings.MatchSettings;
import br.com.muxe.minigame.engine.team.PartyGroups;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

public final class RuntimeFixtures {

    public static final String CONFIG = """
        lobby-group: game-lobby
        default-mode: 1v1
        timings:
          respawn-seconds: 5
          rejoin-seconds: 180
          restart-seconds: 10
          respawn-protection-seconds: 3
          empty-room-seconds: 60
        countdown:
          titles:
            '10': "&610 segundos"
            '5': "&c5"
            '1': "&c1"
        sounds:
          match-start:
            sound: NOTE_PLING
            volume: 1
            pitch: 2
          died:
            sound: ZOMBIE_UNFECT
            volume: 1
            pitch: 2
        hotbar:
          waiting:
            menu:
              slot: 0
              material: CHEST
              name: "&aMenu"
            leave:
              slot: 8
              material: BED
              name: "&cSair"
          spectator:
            teleporter:
              slot: 0
              material: COMPASS
              name: "&aTeleportador"
            random-player:
              slot: 3
              material: IRON_SWORD
              name: "&aAleatório"
            player-in-combat:
              slot: 4
              material: DIAMOND_SWORD
              name: "&aCombate"
            play-again:
              slot: 7
              material: PAPER
              name: "&aJogar novamente"
            leave:
              slot: 8
              material: BED
              name: "&aLobby"
        scoreboard:
          title: "&b&lJOGO"
          waiting:
          - "{code}"
          - "Jogadores: {players}/{max}"
          starting:
          - "Inicia em: {seconds}s"
          playing:
          - "{event}"
          - "{teams}"
          - "Kills: {kills}"
          - "Pontos: {points}"
        """;

    public static final Map<String, Object> PORTUGUESE_MESSAGES = Map.ofEntries(
        Map.entry("room-joined", "{player} entrou ({players}/{max})"),
        Map.entry("room-left", "{player} saiu ({players}/{max})"),
        Map.entry("died-title", "MORREU"),
        Map.entry("respawn-subtitle", "renasce em {seconds} segundos"),
        Map.entry("respawn-subtitle-one", "renasce em 1 segundo"),
        Map.entry("respawned", "renasceu"),
        Map.entry("countdown", "inicia em {seconds} segundos"),
        Map.entry("countdown-one", "inicia em 1 segundo"),
        Map.entry("cannot-break", "não pode quebrar"),
        Map.entry("spectator-subtitle", "espectador"),
        Map.entry("reconnected", "{player} voltou"),
        Map.entry("team-eliminated", "{team} eliminado"),
        Map.entry("match-started", "Começou!"),
        Map.entry("result", List.of("Vencedor: {team}", "{top-final-kills}")),
        Map.entry("result-draw", "Empate em {time}"),
        Map.entry("result-nobody", "ninguém"),
        Map.entry("result-rank-1", "1. {player} {score}"),
        Map.entry("result-rank-2", "2. {player} {score}"),
        Map.entry("result-rank-3", "3. {player} {score}"),
        Map.entry("kill", "{victim} morreu para {killer}"),
        Map.entry("death", "{victim} morreu"),
        Map.entry("no-arena", "sem arena para {mode}"),
        Map.entry("party-split", "AVISO Esta partida não conta estatísticas.")
    );

    public static final MessageCatalog MESSAGES = MessageCatalog.of(PORTUGUESE_MESSAGES);

    public static final String ENGLISH_CONFIG = """
        countdown:
          titles:
            '10': "&610 seconds"
        hotbar:
          waiting:
            menu:
              name: "&aMenu"
            leave:
              name: "&cLeave"
        scoreboard:
          title: "&b&lGAME"
          waiting:
          - "{code}"
          - "Players: {players}/{max}"
          starting:
          - "Starts in: {seconds}s"
          playing:
          - "{event}"
          - "{teams}"
          - "Kills: {kills}"
          - "Points: {points}"
          team-you: " &7YOU"
        """;

    public static final Map<String, Object> ENGLISH_MESSAGES = Map.ofEntries(
        Map.entry("room-joined", "{player} joined ({players}/{max})"),
        Map.entry("room-left", "{player} left ({players}/{max})"),
        Map.entry("died-title", "YOU DIED"),
        Map.entry("respawn-subtitle", "respawning in {seconds} seconds"),
        Map.entry("respawn-subtitle-one", "respawning in 1 second"),
        Map.entry("respawned", "respawned"),
        Map.entry("countdown", "starts in {seconds} seconds"),
        Map.entry("countdown-one", "starts in 1 second"),
        Map.entry("cannot-break", "cannot break"),
        Map.entry("spectator-subtitle", "spectator"),
        Map.entry("reconnected", "{player} is back"),
        Map.entry("team-eliminated", "{team} eliminated"),
        Map.entry("match-started", "Started!"),
        Map.entry("result", List.of("Winner: {team}", "{top-final-kills}")),
        Map.entry("result-draw", "Draw at {time}"),
        Map.entry("result-nobody", "nobody"),
        Map.entry("result-rank-1", "1st {player} {score}"),
        Map.entry("kill", "{victim} was killed by {killer}"),
        Map.entry("death", "{victim} died")
    );

    private RuntimeFixtures() {
    }

    public static MatchSettings settings() {
        return MatchSettings.from(yaml(CONFIG));
    }

    public static Localized<MatchSettings> localizedSettings() {
        return LocalizedConfig.of(yaml(CONFIG), Map.of(Language.ENGLISH, yaml(ENGLISH_CONFIG))).map(MatchSettings::from);
    }

    public static MessageCatalog localizedMessages() {
        return MessageCatalog.of(Map.of(Language.PORTUGUESE, PORTUGUESE_MESSAGES, Language.ENGLISH, ENGLISH_MESSAGES), LanguageLookup.DEFAULT);
    }

    public static MatchEnvironment localizedEnvironment(MutableClock clock, MatchRules rules) {
        return new MatchEnvironment(localizedSettings(), rules, localizedMessages(), clock, new Random(3), MatchEvents.NONE, TeamPreferences.NONE);
    }

    private static YamlConfiguration yaml(String text) {
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.loadFromString(text);
        } catch (InvalidConfigurationException exception) {
            throw new IllegalStateException(exception);
        }
        return yaml;
    }

    public static MatchEnvironment environment(MutableClock clock, MatchRules rules) {
        return new MatchEnvironment(settings(), rules, MESSAGES, clock, new Random(3), MatchEvents.NONE, TeamPreferences.NONE);
    }

    public static MatchEnvironment environment(MutableClock clock, MatchRules rules, PartyGroups parties) {
        return new MatchEnvironment(settings(), rules, MESSAGES, clock, new Random(3), MatchEvents.NONE, TeamPreferences.NONE, parties);
    }
}
