package br.com.muxe.core.proxy.editor.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.core.common.permission.domain.Contexts;
import br.com.muxe.core.common.permission.domain.PermissionChange;
import br.com.muxe.core.common.permission.domain.PermissionNode;
import br.com.muxe.core.common.permission.domain.Tag;
import br.com.muxe.core.common.permission.domain.Track;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ChangeReaderTest {

    private static final Instant NOW = Instant.parse("2026-10-28T15:00:00Z");

    @Test
    void everyKindOfChangeIsRead() {
        String body = """
            {"changes":[
              {"type":"put-tag","tag":{"id":"halloween","name":"Halloween","color":"&6","weight":95,"prefix":"&6&lBOO ","decorative":true}},
              {"type":"put-group","group":{"id":"helper","displayName":"Helper","weight":5,"tag":null,"nodes":[]}},
              {"type":"put-track","track":{"id":"staff","groups":["helper","mod"]}},
              {"type":"delete-track","id":"old"},
              {"type":"delete-group","id":"old"},
              {"type":"delete-tag","id":"old"}
            ]}
            """;

        List<PermissionChange> changes = ChangeReader.read(body, NOW);

        assertThat(changes).containsExactly(
            new PermissionChange.PutTag(new Tag("halloween", "Halloween", "&6", 95, Optional.of("&6&lBOO "), true)),
            changes.get(1),
            new PermissionChange.PutTrack(new Track("staff", List.of("helper", "mod"))),
            new PermissionChange.DeleteTrack("old"),
            new PermissionChange.DeleteGroup("old"),
            new PermissionChange.DeleteTag("old"));
        assertThat(((PermissionChange.PutGroup) changes.get(1)).group().tag()).isEmpty();
    }

    @Test
    void nodesKeepTheirValueContextsAndExpiry() {
        String body = """
            {"changes":[{"type":"put-group","group":{"id":"vip","displayName":"VIP","weight":10,"tag":"vip","nodes":[
              {"key":"Muxe.Fly","value":false,"contexts":{"server":"lobby","world":"lobby"},"expiresAt":1893456000000,"addedAt":1700000000000,"updatedAt":1700000500000}
            ]}}]}
            """;

        PermissionNode node = ((PermissionChange.PutGroup) ChangeReader.read(body, NOW).getFirst()).group().nodes().getFirst();

        assertThat(node.key()).isEqualTo("muxe.fly");
        assertThat(node.value()).isFalse();
        assertThat(node.contexts()).isEqualTo(new Contexts(Map.of("server", "lobby", "world", "lobby")));
        assertThat(node.expiresAt()).contains(Instant.ofEpochMilli(1893456000000L));
        assertThat(node.addedAt()).isEqualTo(Instant.ofEpochMilli(1700000000000L));
        assertThat(node.updatedAt()).isEqualTo(Instant.ofEpochMilli(1700000500000L));
    }

    @Test
    void newOrFutureTimestampsBecomeNow() {
        String body = """
            {"changes":[{"type":"put-user","user":{"id":"00000000-0000-0000-0000-00000000000b","nodes":[
              {"key":"group.vip"},
              {"key":"muxe.fly","addedAt":4102444800000}
            ]}}]}
            """;

        List<PermissionNode> nodes = ((PermissionChange.PutUser) ChangeReader.read(body, NOW).getFirst()).user().nodes();

        assertThat(nodes).allSatisfy(node -> {
            assertThat(node.value()).isTrue();
            assertThat(node.addedAt()).isEqualTo(NOW);
            assertThat(node.updatedAt()).isEqualTo(NOW);
        });
    }

    @Test
    void malformedBodiesAreRejected() {
        assertInvalid("[]");
        assertInvalid("{\"changes\":[]}");
        assertInvalid("{\"changes\":[{\"type\":\"drop-database\"}]}");
        assertInvalid("{\"changes\":[{\"type\":\"put-user\",\"user\":{\"id\":\"not-a-uuid\"}}]}");
        assertInvalid("{\"changes\":[{\"type\":\"put-tag\",\"tag\":{\"id\":\"x\",\"name\":\"X\",\"color\":\"&a\",\"weight\":1.5}}]}");
        assertInvalid("{\"changes\":[{\"type\":\"put-tag\",\"tag\":{\"id\":\"x\",\"name\":{\"a\":1},\"color\":\"&a\",\"weight\":1}}]}");
        assertInvalid("{\"changes\":[{\"type\":\"put-group\",\"group\":{\"id\":\"x\",\"displayName\":\"X\",\"weight\":1,\"nodes\":[{\"key\":\"a\"},{\"key\":\"a\"}]}}]}");
        assertInvalid("{\"changes\":[{\"type\":\"put-group\",\"group\":{\"id\":\"x\",\"displayName\":\"X\",\"weight\":1,\"nodes\":[{\"key\":\"bad key\"}]}}]}");
        assertInvalid("{\"changes\":[{\"type\":\"put-group\",\"group\":{\"id\":\"x\",\"displayName\":\"X\",\"weight\":1,\"nodes\":[{\"key\":\"a\",\"contexts\":{\"server\":\"a=b\"}}]}}]}");
    }

    @Test
    void aSaveHasAtMostFiveHundredChanges() {
        String change = "{\"type\":\"delete-tag\",\"id\":\"x\"}";
        String body = "{\"changes\":[" + String.join(",", Collections.nCopies(ChangeReader.MAX_CHANGES + 1, change)) + "]}";

        assertInvalid(body);
    }

    private static void assertInvalid(String body) {
        assertThatThrownBy(() -> ChangeReader.read(body, NOW)).isInstanceOf(IllegalArgumentException.class);
    }
}
