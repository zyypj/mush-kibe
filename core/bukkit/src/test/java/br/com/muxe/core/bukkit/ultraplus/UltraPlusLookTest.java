package br.com.muxe.core.bukkit.ultraplus;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.bukkit.permission.display.NameTag;
import br.com.muxe.core.bukkit.permission.display.RankDisplay;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.permission.domain.Tag;
import br.com.muxe.core.common.ultraplus.domain.UltraPlusStyle;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UltraPlusLookTest {

    private static final UltraPlusSettings SETTINGS = UltraPlusSettings.from(LocalizedConfig.bundled(UltraPlusLookTest.class, "config.yml").in(Language.PORTUGUESE));
    private static final Tag ULTRA_PLUS = new Tag("ultra+", "Ultra&6+", "&5", 50, Optional.of("&5&lULTRA&6+ "), false);
    private static final Tag NATAL = new Tag("natal", "Natal", "&c", 46, Optional.empty(), true);
    private static final String CHAT = "{medal}{prefix}{color}{player}: &f{message}";

    @Test
    void anUnlockedLevelAndPlusColorRestyleTheUltraPlusTagAndTheEmblemFollowsTheNick() {
        UltraPlusStyle style = new UltraPlusStyle(Optional.of("coracao"), Optional.of("amarelo"), 2);

        RankDisplay display = UltraPlusLook.of(SETTINGS, style, 6).apply(SETTINGS, new RankDisplay(Optional.of(ULTRA_PLUS)));

        assertThat(display.prefix()).isEqualTo("&b&lULTRA&e+ ");
        assertThat(display.color()).isEqualTo("&b");
        assertThat(display.emblem()).isEqualTo("❤");
        assertThat(display.preview(CHAT, "tadeu", "oi")).isEqualTo("&b&lULTRA&e+ &btadeu❤: &foi");
        assertThat(display.chatFormat(CHAT)).isEqualTo("§b§lULTRA§e+ §b%1$s❤: §f%2$s");
    }

    @Test
    void choicesTheMemberHasNotReachedFallBackToTheIncludedOnes() {
        UltraPlusStyle style = new UltraPlusStyle(Optional.empty(), Optional.of("preto"), 4);

        RankDisplay display = UltraPlusLook.of(SETTINGS, style, 3).apply(SETTINGS, new RankDisplay(Optional.of(ULTRA_PLUS)));

        assertThat(display.prefix()).isEqualTo("&5&lULTRA&6+ ");
        assertThat(display.emblem()).isEmpty();
    }

    @Test
    void anotherTagKeepsItsLookButStillShowsTheEmblem() {
        UltraPlusStyle style = UltraPlusStyle.DEFAULT.withEmblem(Optional.of("mais"));

        RankDisplay display = UltraPlusLook.of(SETTINGS, style, 1).apply(SETTINGS, new RankDisplay(Optional.of(NATAL)));

        assertThat(display.prefix()).isEqualTo("&c&lNATAL ");
        assertThat(display.emblem()).isEqualTo("&6+");
    }

    @Test
    void theEmblemGoesInTheNameTagSuffixAndEachLookGetsItsOwnTeam() {
        RankDisplay plain = new RankDisplay(Optional.of(ULTRA_PLUS));
        RankDisplay styled = UltraPlusLook.of(SETTINGS, new UltraPlusStyle(Optional.of("coracao"), Optional.empty(), 2), 6).apply(SETTINGS, plain);

        NameTag before = NameTag.of(Optional.empty(), plain);
        NameTag after = NameTag.of(Optional.empty(), styled);

        assertThat(after.prefix()).isEqualTo("§b§lULTRA§6+ §b");
        assertThat(after.suffix()).isEqualTo("❤");
        assertThat(after.team()).hasSize(NameTag.MAX_TEAM).isNotEqualTo(before.team());
        assertThat(after.team().substring(0, 6)).isEqualTo(before.team().substring(0, 6));
    }
}
