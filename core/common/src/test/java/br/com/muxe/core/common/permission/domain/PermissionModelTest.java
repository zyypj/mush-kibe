package br.com.muxe.core.common.permission.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.core.common.testing.PermissionFixtures;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PermissionModelTest {

    private static final Instant NOW = Instant.parse("2026-10-28T15:00:00Z");

    @Test
    void contextsAreStoredSortedAndMatchWhenEveryPairMatches() {
        Contexts contexts = new Contexts(Map.of("World", "Lobby", "server", "lobby"));

        assertThat(contexts.encode()).isEqualTo("server=lobby;world=lobby");
        assertThat(Contexts.parse("server=lobby;world=lobby")).isEqualTo(contexts);
        assertThat(Contexts.parse("")).isEqualTo(Contexts.NONE);
        assertThat(Contexts.of("server", "lobby").satisfiedBy(contexts)).isTrue();
        assertThat(Contexts.of("server", "bedwars").satisfiedBy(contexts)).isFalse();
        assertThat(Contexts.NONE.satisfiedBy(contexts)).isTrue();
        assertThatThrownBy(() -> Contexts.of("a=b", "c")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tagsShowTheirNameInColorAndABoldPrefix() {
        Tag vip = PermissionFixtures.DATA.tag("vip").orElseThrow();
        Tag membro = PermissionFixtures.DATA.tag("membro").orElseThrow();

        assertThat(vip.display()).isEqualTo("&aVIP");
        assertThat(vip.formatName("tadeu")).isEqualTo("&a&lVIP &atadeu");
        assertThat(PermissionFixtures.DATA.tag("partner+").orElseThrow().chatPrefix()).isEqualTo("&1&lPARTNER+ ");
        assertThat(membro.formatName("tadeu")).isEqualTo("&7tadeu");
        assertThat(vip.permission()).isEqualTo("muxe.tag.vip");
        assertThatThrownBy(() -> new Tag("x", "X", "&z", 0, Optional.empty(), true)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nodesAreLowercaseAndKnowTheirGroup() {
        PermissionNode node = PermissionNode.permanent("Group.VIP", true, NOW);

        assertThat(node.key()).isEqualTo("group.vip");
        assertThat(node.isGroup()).isTrue();
        assertThat(node.groupId()).isEqualTo("vip");
        assertThatThrownBy(() -> PermissionNode.permanent("com espaço", true, NOW)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addingTheSameNodeAgainKeepsWhenItWasFirstAdded() {
        UUID id = UUID.randomUUID();
        PermissionNode first = PermissionNode.group("vip", Optional.empty(), NOW);
        PermissionNode again = PermissionNode.group("vip", Optional.of(NOW.plusSeconds(60)), NOW.plusSeconds(10));

        PermissionUser user = PermissionUser.empty(id).with(first).with(again);

        assertThat(user.nodes()).hasSize(1);
        assertThat(user.nodes().getFirst().addedAt()).isEqualTo(NOW);
        assertThat(user.nodes().getFirst().updatedAt()).isEqualTo(NOW.plusSeconds(10));
        assertThat(user.without("group.vip", Contexts.NONE).nodes()).isEmpty();
    }

    @Test
    void tracksMoveUpAndDown() {
        Track track = PermissionFixtures.DATA.track("doacao").orElseThrow();

        assertThat(track.next("vip")).contains("mvp");
        assertThat(track.previous("vip")).contains("membro");
        assertThat(track.next("ultra")).isEmpty();
        assertThat(track.previous("admin")).isEmpty();
        assertThatThrownBy(() -> new Track("x", List.of("vip", "vip"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theSeededDataFollowsTheHierarchy() {
        assertThat(PermissionRules.check(PermissionFixtures.DATA)).isEmpty();
        assertThat(PermissionFixtures.DATA.tagsByWeight()).extracting(Tag::id).startsWith("admin", "mod+", "mod", "partner+", "partner", "ferias-2", "ferias");
        assertThat(PermissionFixtures.DATA.groupsByWeight()).extracting(Group::id).endsWith("vip", "membro");
    }

    @Test
    void theRulesRefuseBrokenData() {
        PermissionData withoutDefault = PermissionChange.applyAll(PermissionFixtures.DATA, List.of(new PermissionChange.DeleteGroup("membro")));
        Group looping = PermissionFixtures.DATA.group("membro").orElseThrow().withNodes(List.of(PermissionNode.group("admin", Optional.empty(), NOW)));
        PermissionData cycle = PermissionChange.applyAll(PermissionFixtures.DATA, List.of(new PermissionChange.PutGroup(looping)));
        PermissionData missingTag = PermissionChange.applyAll(PermissionFixtures.DATA, List.of(new PermissionChange.DeleteTag("vip")));

        assertThat(PermissionRules.check(withoutDefault)).hasValueSatisfying(problem -> assertThat(problem.code()).isEqualTo(PermissionProblems.DEFAULT_GROUP_REQUIRED));
        assertThat(PermissionRules.check(cycle)).hasValueSatisfying(problem -> assertThat(problem.code()).isEqualTo(PermissionProblems.INHERITANCE_CYCLE));
        assertThat(PermissionRules.check(missingTag)).hasValueSatisfying(problem -> assertThat(problem.code()).isEqualTo(PermissionProblems.UNKNOWN_TAG));
    }
}
