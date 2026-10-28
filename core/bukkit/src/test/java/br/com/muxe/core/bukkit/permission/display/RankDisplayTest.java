package br.com.muxe.core.bukkit.permission.display;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.permission.domain.Contexts;
import br.com.muxe.core.common.permission.domain.Group;
import br.com.muxe.core.common.permission.domain.PermissionData;
import br.com.muxe.core.common.permission.domain.PermissionNode;
import br.com.muxe.core.common.permission.domain.PermissionResolver;
import br.com.muxe.core.common.permission.domain.PermissionUser;
import br.com.muxe.core.common.permission.domain.ResolvedPermissions;
import br.com.muxe.core.common.permission.domain.Tag;
import br.com.muxe.core.common.testing.PermissionFixtures;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RankDisplayTest {

    private static final Instant NOW = Instant.parse("2026-10-28T15:00:00Z");
    private static final PermissionData DATA = PermissionFixtures.DATA;
    private static final String CHAT = "{prefix}{color}{player}&f: {message}";

    @Test
    void theChatShowsTheTagPrefixAndTheNameInTheTagColor() {
        assertThat(String.format(display("vip").chatFormat(CHAT), "tadeu", "Olá!")).isEqualTo("§a§lVIP §atadeu§f: Olá!");
        assertThat(String.format(display("membro").chatFormat(CHAT), "tadeu", "100% certo")).isEqualTo("§7tadeu§f: 100% certo");
        assertThat(String.format(RankDisplay.NONE.chatFormat(CHAT), "tadeu", "oi")).isEqualTo("§7tadeu§f: oi");
    }

    @Test
    void namesAndTagsUseTheTagColors() {
        assertThat(display("natal").formatName("tadeu")).isEqualTo("&c&lNATAL &ctadeu");
        assertThat(display("ultra").coloredName("tadeu")).isEqualTo("&dtadeu");
        assertThat(display("ultra").tagDisplay()).isEqualTo("&dUltra");
        assertThat(RankDisplay.NONE.tagDisplay()).isEqualTo("&7Membro");
        assertThat(display("vip").preview(CHAT, "tadeu", "Olá!")).isEqualTo("&a&lVIP &atadeu&f: Olá!");
    }

    @Test
    void theSelectedTagWinsThenTheRankTag() {
        ResolvedPermissions ultra = resolve(PermissionNode.group("ultra", Optional.empty(), NOW), PermissionNode.permanent("muxe.tag.natal", true, NOW));

        assertThat(RankDisplay.of(DATA, ultra, Optional.empty()).tag()).map(Tag::id).contains("ultra");
        assertThat(RankDisplay.of(DATA, ultra, Optional.of("natal")).tag()).map(Tag::id).contains("natal");
        assertThat(RankDisplay.of(DATA, ultra, Optional.of("mvp")).tag()).map(Tag::id).contains("mvp");
        assertThat(RankDisplay.of(DATA, ultra, Optional.of("admin")).tag()).map(Tag::id).contains("ultra");
        assertThat(RankDisplay.of(DATA, resolve(), Optional.empty()).tag()).map(Tag::id).contains("membro");
    }

    @Test
    void teamsSortByRankThenTagAndKeepThePrefixWithinSixteenCharacters() {
        NameTag admin = NameTag.of(DATA.group("admin"), display("admin"));
        NameTag vip = NameTag.of(DATA.group("vip"), display("vip"));
        NameTag member = NameTag.of(DATA.group("membro"), display("membro"));
        NameTag enderlore = NameTag.of(DATA.group("vip"), display("enderlore"));

        assertThat(List.of(member.team(), vip.team(), admin.team(), enderlore.team()).stream().sorted().toList())
            .containsExactly(admin.team(), enderlore.team(), vip.team(), member.team());
        assertThat(vip.prefix()).isEqualTo("§a§lVIP §a");
        assertThat(member.prefix()).isEqualTo("§7");
        assertThat(enderlore.prefix()).isEqualTo("§5§lENDERLORE §5");
        assertThat(NameTag.of(Optional.<Group>empty(), RankDisplay.NONE).team()).hasSizeLessThanOrEqualTo(16);
    }

    @Test
    void longPrefixesAreCutWithoutBreakingAColorCode() {
        assertThat(NameTag.fit("§1§lABCDEFGHI§aXYZ", "§1")).isEqualTo("§1§lABCDEFGHI§1");
        assertThat(NameTag.fit("§5§lABCDEFGHIJKLMN ", "§5")).hasSize(16).endsWith("§5");
    }

    private static RankDisplay display(String tag) {
        return new RankDisplay(DATA.tag(tag));
    }

    private static ResolvedPermissions resolve(PermissionNode... nodes) {
        return PermissionResolver.resolve(DATA, new PermissionUser(UUID.randomUUID(), List.of(nodes), Optional.empty()), Contexts.NONE, NOW);
    }
}
