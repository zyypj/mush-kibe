package br.com.muxe.core.common.permission.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.testing.PermissionFixtures;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PermissionResolverTest {

    private static final Instant NOW = Instant.parse("2026-10-28T15:00:00Z");
    private static final Contexts LOBBY = Contexts.server("lobby", "lobby");

    private final UUID player = UUID.randomUUID();

    @Test
    void everyoneIsAMemberWithTheMemberTag() {
        ResolvedPermissions resolved = resolve(PermissionFixtures.DATA, user());

        assertThat(resolved.groups()).extracting(Group::id).containsExactly("membro");
        assertThat(resolved.tags()).extracting(Tag::id).containsExactly("membro");
        assertThat(resolved.displayTag(Optional.empty()).map(tag -> tag.formatName("tadeu"))).contains("&7tadeu");
    }

    @Test
    void aRankInheritsEveryRankBelowItAndItsTags() {
        ResolvedPermissions resolved = resolve(PermissionFixtures.DATA, user(group("ultra", Optional.empty())));

        assertThat(resolved.groups()).extracting(Group::id).containsExactly("ultra", "pro", "mvp", "vip", "membro");
        assertThat(resolved.primaryGroup().map(Group::id)).contains("ultra");
        assertThat(resolved.tags()).extracting(Tag::id).containsExactly("ultra", "pro", "mvp", "vip", "membro");
        assertThat(resolved.displayTag(Optional.empty()).map(tag -> tag.formatName("tadeu"))).contains("&d&lULTRA &dtadeu");
    }

    @Test
    void decorativeTagsComeFromTheirPermissionAndGiveNothingElse() {
        ResolvedPermissions resolved = resolve(PermissionFixtures.DATA, user(group("vip", Optional.empty()), node("muxe.tag.natal", true)));

        assertThat(resolved.tags()).extracting(Tag::id).containsExactly("natal", "vip", "membro");
        assertThat(resolved.groups()).extracting(Group::id).containsExactly("vip", "membro");
        assertThat(resolved.has("group.ultra")).isFalse();
        assertThat(resolved.displayTag(Optional.of("vip")).map(Tag::id)).contains("vip");
        assertThat(resolved.displayTag(Optional.of("admin")).map(Tag::id)).contains("natal");
    }

    @Test
    void theAdminWildcardGrantsEverythingButAUserFalseWins() {
        ResolvedPermissions resolved = resolve(PermissionFixtures.DATA, user(group("admin", Optional.empty()), node("muxe.build", false)));

        assertThat(resolved.has("muxe.npc")).isTrue();
        assertThat(resolved.check("muxe.build")).isEqualTo(Tristate.FALSE);
        assertThat(resolved.tags()).hasSize(PermissionFixtures.TAGS.size());
    }

    @Test
    void wildcardsAndNegationsFollowTheMostSpecificNode() {
        ResolvedPermissions resolved = resolve(PermissionFixtures.DATA, user(node("muxe.shop.*", true), node("muxe.shop.manage", false)));

        assertThat(resolved.check("muxe.shop.open")).isEqualTo(Tristate.TRUE);
        assertThat(resolved.check("muxe.shop.manage")).isEqualTo(Tristate.FALSE);
        assertThat(resolved.check("muxe.npc")).isEqualTo(Tristate.UNDEFINED);
    }

    @Test
    void aHeavierGroupOverridesALighterOne() {
        Map<String, Group> groups = new HashMap<>(PermissionFixtures.DATA.groups());
        groups.put("vip", withNode(groups.get("vip"), node("muxe.fly", true)));
        groups.put("pro", withNode(groups.get("pro"), node("muxe.fly", false)));
        PermissionData data = new PermissionData(groups, PermissionFixtures.DATA.tags(), PermissionFixtures.DATA.tracks());

        assertThat(resolve(data, user(group("pro", Optional.empty()))).check("muxe.fly")).isEqualTo(Tristate.FALSE);
        assertThat(resolve(data, user(group("mvp", Optional.empty()))).check("muxe.fly")).isEqualTo(Tristate.TRUE);
    }

    @Test
    void contextNodesOnlyApplyWhereTheyMatch() {
        PermissionNode lobbyOnly = new PermissionNode("muxe.fly", true, Contexts.of(Contexts.SERVER, "lobby"), Optional.empty(), NOW, NOW);
        PermissionUser user = user(lobbyOnly);

        assertThat(resolve(PermissionFixtures.DATA, user).has("muxe.fly")).isTrue();
        assertThat(PermissionResolver.resolve(PermissionFixtures.DATA, user, Contexts.server("bedwars", "mapa"), NOW).has("muxe.fly")).isFalse();
    }

    @Test
    void temporaryRanksExpire() {
        PermissionUser user = user(group("vip", Optional.of(NOW.plus(Duration.ofDays(30)))));

        ResolvedPermissions now = resolve(PermissionFixtures.DATA, user);
        ResolvedPermissions later = PermissionResolver.resolve(PermissionFixtures.DATA, user, LOBBY, NOW.plus(Duration.ofDays(31)));

        assertThat(now.primaryGroup().map(Group::id)).contains("vip");
        assertThat(now.nextExpiry()).contains(NOW.plus(Duration.ofDays(30)));
        assertThat(later.primaryGroup().map(Group::id)).contains("membro");
    }

    @Test
    void aNegatedGroupIsNotInherited() {
        ResolvedPermissions resolved = resolve(PermissionFixtures.DATA, user(group("pro", Optional.empty()), node("group.mvp", false)));

        assertThat(resolved.groups()).extracting(Group::id).containsExactly("pro", "membro");
    }

    @Test
    void metaComesFromMetaNodes() {
        ResolvedPermissions resolved = resolve(PermissionFixtures.DATA, user(node("meta.cor.azul", true)));

        assertThat(resolved.meta("cor")).contains("azul");
        assertThat(resolved.meta("outra")).isEmpty();
    }

    private ResolvedPermissions resolve(PermissionData data, PermissionUser user) {
        return PermissionResolver.resolve(data, user, LOBBY, NOW);
    }

    private PermissionUser user(PermissionNode... nodes) {
        return new PermissionUser(player, List.of(nodes), Optional.empty());
    }

    private static PermissionNode group(String id, Optional<Instant> expiresAt) {
        return PermissionNode.group(id, expiresAt, NOW);
    }

    private static PermissionNode node(String key, boolean value) {
        return PermissionNode.permanent(key, value, NOW);
    }

    private static Group withNode(Group group, PermissionNode node) {
        List<PermissionNode> nodes = new ArrayList<>(group.nodes());
        nodes.add(node);
        return group.withNodes(nodes);
    }
}
