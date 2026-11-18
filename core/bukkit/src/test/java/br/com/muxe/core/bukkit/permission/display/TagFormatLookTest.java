package br.com.muxe.core.bukkit.permission.display;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.permission.domain.Tag;
import br.com.muxe.core.common.preference.domain.TagFormat;
import br.com.muxe.core.common.testing.PermissionFixtures;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TagFormatLookTest {

    private static final Tag ULTRA_PLUS = new Tag("ultra+", "Ultra&6+", "&5", 50, Optional.of("&5&lULTRA&6+ "), false);
    private static final RankDisplay TADEU = new RankDisplay(Optional.of(ULTRA_PLUS));
    private static final String EXAMPLE = "{prefix}{color}{player}";
    private static final String CHAT = "{medal}{prefix}{color}{player}: &f{message}";

    @Test
    void eachFormatShowsTheUltraPlusTagLikeTheMenuExamples() {
        assertThat(look(TagFormat.DEFAULT)).isEqualTo("&5&lULTRA&6+ &5tadeu");
        assertThat(look(TagFormat.COLOR)).isEqualTo("&5tadeu&6+");
        assertThat(look(TagFormat.BRACKETS_UPPER)).isEqualTo("&5[ULTRA&6+&5] &5tadeu");
        assertThat(look(TagFormat.BRACKETS)).isEqualTo("&5[Ultra&6+&5] &5tadeu");
        assertThat(look(TagFormat.DEFAULT_WHITE)).isEqualTo("&5&lULTRA&6+ &ftadeu");
        assertThat(look(TagFormat.DEFAULT_GRAY)).isEqualTo("&5&lULTRA&6+ &7tadeu");
    }

    @Test
    void aTagWithoutPrefixKeepsItsLookAndATagWithoutCustomPrefixGetsBrackets() {
        RankDisplay member = new RankDisplay(PermissionFixtures.DATA.tag("membro"));
        RankDisplay vip = new RankDisplay(PermissionFixtures.DATA.tag("vip"));

        for (TagFormat format : TagFormat.values()) {
            assertThat(TagFormatLook.apply(format, member)).isEqualTo(member);
            assertThat(TagFormatLook.apply(format, RankDisplay.NONE)).isEqualTo(RankDisplay.NONE);
        }
        assertThat(TagFormatLook.apply(TagFormat.BRACKETS_UPPER, vip).preview(EXAMPLE, "tadeu", "")).isEqualTo("&a[VIP&a] &atadeu");
        assertThat(TagFormatLook.apply(TagFormat.COLOR, vip).preview(EXAMPLE, "tadeu", "")).isEqualTo("&atadeu");
    }

    @Test
    void theMedalAndTheEmblemStayAroundTheFormattedName() {
        RankDisplay styled = TADEU.withMedal(PermissionFixtures.DATA.medal("cafe")).withEmblem("❤");

        assertThat(TagFormatLook.apply(TagFormat.COLOR, styled).preview(CHAT, "tadeu", "oi")).isEqualTo("&e☕ &5tadeu&6+&5❤: &foi");
        assertThat(TagFormatLook.apply(TagFormat.BRACKETS, styled).preview(CHAT, "tadeu", "oi")).isEqualTo("&e☕ &5[Ultra&6+&5] &5tadeu❤: &foi");
    }

    @Test
    void theNameTagKeepsTheBracketsWithinSixteenCharactersAndTheColorTailInTheSuffix() {
        NameTag brackets = NameTag.of(Optional.empty(), TagFormatLook.apply(TagFormat.BRACKETS_UPPER, TADEU));
        NameTag color = NameTag.of(Optional.empty(), TagFormatLook.apply(TagFormat.COLOR, TADEU));
        NameTag standard = NameTag.of(Optional.empty(), TADEU);

        assertThat(brackets.prefix()).isEqualTo("§5[ULTRA§6+§5] ");
        assertThat(color.prefix()).isEqualTo("§5");
        assertThat(color.suffix()).isEqualTo("§6+");
        assertThat(standard.prefix()).isEqualTo("§5§lULTRA§6+ §5");
        assertThat(brackets.team()).isNotEqualTo(standard.team());
    }

    @Test
    void aBoardSharedByViewersWithDifferentFormatsFallsBackToTheDefault() {
        assertThat(NameTagTeams.shared(TagFormat.BRACKETS, TagFormat.BRACKETS)).isEqualTo(TagFormat.BRACKETS);
        assertThat(NameTagTeams.shared(TagFormat.BRACKETS, TagFormat.COLOR)).isEqualTo(TagFormat.DEFAULT);
        assertThat(NameTagTeams.shared(TagFormat.DEFAULT, TagFormat.COLOR)).isEqualTo(TagFormat.DEFAULT);
    }

    @Test
    void theChatIsRewrittenOnlyForAViewerWithAnotherFormat() {
        String format = "§7[12✫] " + TADEU.chatFormat(CHAT);

        assertThat(ChatFormatListener.formatFor(format, TADEU, TagFormat.DEFAULT)).isEmpty();
        assertThat(ChatFormatListener.formatFor(format, TADEU, TagFormat.BRACKETS_UPPER).map(own -> String.format(own, "tadeu", "oi")))
            .contains("§7[12✫] §5[ULTRA§6+§5] §5tadeu: §foi");
        assertThat(ChatFormatListener.formatFor(format, new RankDisplay(PermissionFixtures.DATA.tag("membro")), TagFormat.BRACKETS)).isEmpty();
    }

    private static String look(TagFormat format) {
        return TagFormatLook.apply(format, TADEU).preview(EXAMPLE, "tadeu", "");
    }
}
