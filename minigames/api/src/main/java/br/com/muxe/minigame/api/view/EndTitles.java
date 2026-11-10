package br.com.muxe.minigame.api.view;

import br.com.muxe.core.bukkit.effect.SoundCue;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.bukkit.text.Title;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.LanguageLookup;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.core.common.language.domain.Text;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class EndTitles {

    private static final int STAY_TICKS = 20;
    private static final String SUBTITLE = " ";
    private static final String VICTORY = "victory";
    private static final String DEFEAT = "defeat";
    private static final Text VICTORY_WORD = Text.of("VITÓRIA", "VICTORY");
    private static final Text DEFEAT_WORD = Text.of("DERROTA", "DEFEAT");

    private final Plugin plugin;
    private final Localized<TitleSweep> victory;
    private final Localized<TitleSweep> defeat;
    private final Optional<SoundCue> victorySound;
    private final Optional<SoundCue> defeatSound;
    private final LanguageLookup languages;
    private final TitleRuns runs = new TitleRuns();

    public EndTitles(Plugin plugin, TitleSweep victory, TitleSweep defeat, Optional<SoundCue> victorySound, Optional<SoundCue> defeatSound) {
        this(plugin, Localized.same(victory), Localized.same(defeat), victorySound, defeatSound, LanguageLookup.DEFAULT);
    }

    public EndTitles(Plugin plugin, Localized<TitleSweep> victory, Localized<TitleSweep> defeat, Optional<SoundCue> victorySound, Optional<SoundCue> defeatSound,
                     LanguageLookup languages) {
        this.plugin = plugin;
        this.victory = victory;
        this.defeat = defeat;
        this.victorySound = victorySound;
        this.defeatSound = defeatSound;
        this.languages = languages;
    }

    public static EndTitles from(Plugin plugin, ConfigurationSection section) {
        return from(plugin, Localized.same(Optional.ofNullable(section)), LanguageLookup.DEFAULT);
    }

    public static EndTitles from(Plugin plugin, Localized<Optional<ConfigurationSection>> sections, LanguageLookup languages) {
        Localized<TitleSweep> victory = Localized.of(language -> sweep(sections.in(language), VICTORY, language, "&6"));
        Localized<TitleSweep> defeat = Localized.of(language -> sweep(sections.in(language), DEFEAT, language, "&c"));
        Optional<ConfigurationSection> standard = sections.standard();
        return new EndTitles(plugin, victory, defeat, sound(standard, VICTORY), sound(standard, DEFEAT), languages);
    }

    public TitleSweep sweep(boolean won, Language language) {
        return (won ? victory : defeat).in(language);
    }

    public void victory(Player player) {
        show(player, victory.in(languages.of(player.getUniqueId())), victorySound);
    }

    public void defeat(Player player) {
        show(player, defeat.in(languages.of(player.getUniqueId())), defeatSound);
    }

    private static TitleSweep sweep(Optional<ConfigurationSection> section, String kind, Language language, String accent) {
        Text word = kind.equals(VICTORY) ? VICTORY_WORD : DEFEAT_WORD;
        return TitleSweep.from(section.map(found -> found.getConfigurationSection(kind)).orElse(null), word.in(language), accent);
    }

    private static Optional<SoundCue> sound(Optional<ConfigurationSection> section, String kind) {
        return SoundCue.from(section.map(found -> found.getConfigurationSection(kind + ".sound")).orElse(null));
    }

    public void stop(Player player) {
        runs.stop(player.getUniqueId());
    }

    private void show(Player player, TitleSweep sweep, Optional<SoundCue> sound) {
        Title.clear(player);
        sound.ifPresent(cue -> cue.play(player));
        Sweep run = new Sweep(player, sweep);
        run.runTaskTimer(plugin, 0L, 1L);
        runs.start(player.getUniqueId(), run.stop);
    }

    private final class Sweep extends BukkitRunnable {

        private final Player player;
        private final TitleSweep sweep;
        private final Runnable stop = this::cancel;
        private int tick;

        Sweep(Player player, TitleSweep sweep) {
            this.player = player;
            this.sweep = sweep;
        }

        @Override
        public void run() {
            Optional<String> frame = sweep.frame(tick++);
            if (frame.isEmpty() || !player.isOnline()) {
                cancel();
                runs.done(player.getUniqueId(), stop);
                return;
            }
            Title.builder().title(Colors.translate(frame.get())).subtitle(SUBTITLE).timings(0, STAY_TICKS, 0).build().send(player);
        }
    }
}
