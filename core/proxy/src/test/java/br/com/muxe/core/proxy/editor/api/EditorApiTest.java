package br.com.muxe.core.proxy.editor.api;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.permission.domain.PermissionNode;
import br.com.muxe.core.common.permission.domain.PermissionProblems;
import br.com.muxe.core.common.permission.domain.PermissionUser;
import br.com.muxe.core.proxy.editor.EditorFixture;
import br.com.muxe.core.proxy.editor.security.EditorAuthenticator;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EditorApiTest {

    private final EditorFixture fixture = new EditorFixture();
    private final String token = fixture.adminToken();

    @AfterEach
    void close() {
        fixture.close();
    }

    @Test
    void theSessionShowsWhoIsEditingAndWhenItEnds() {
        ApiResponse response = fixture.get("/api/session", token);

        assertThat(response.status()).isEqualTo(200);
        JsonObject body = response.body().getAsJsonObject();
        assertThat(body.get("actor").getAsString()).isEqualTo("tadeu");
        assertThat(body.get("expiresAt").getAsLong()).isEqualTo(EditorFixture.NOW.plus(Duration.ofMinutes(30)).toEpochMilli());
        assertThat(body.toString()).doesNotContain(token);
    }

    @Test
    void theDataListsGroupsByWeightWithTagsAndTracks() {
        JsonObject body = fixture.get("/api/data", token).body().getAsJsonObject();

        JsonArray groups = body.getAsJsonArray("groups");
        assertThat(groups.get(0).getAsJsonObject().get("id").getAsString()).isEqualTo("admin");
        assertThat(ids(groups)).hasSize(10).contains("membro", "vip", "mod+");
        JsonObject vip = find(groups, "vip");
        assertThat(vip.get("tag").getAsString()).isEqualTo("vip");
        assertThat(vip.getAsJsonArray("nodes").get(0).getAsJsonObject().get("key").getAsString()).isEqualTo("group.membro");
        assertThat(find(body.getAsJsonArray("tags"), "vip").get("chatPrefix").getAsString()).isEqualTo("&a&lVIP ");
        assertThat(ids(body.getAsJsonArray("tracks"))).containsExactly("doacao", "staff");
        assertThat(body.get("defaultGroup").getAsString()).isEqualTo("membro");
    }

    @Test
    void theDataListsMedalsByWeightWithTheirDisplay() {
        JsonArray medals = fixture.get("/api/data", token).body().getAsJsonObject().getAsJsonArray("medals");

        assertThat(medals).hasSize(31);
        assertThat(ids(medals).subList(0, 3)).containsExactly("toxico", "yin-yang", "musico");
        JsonObject cafe = find(medals, "cafe");
        assertThat(cafe.get("name").getAsString()).isEqualTo("Café");
        assertThat(cafe.get("symbol").getAsString()).isEqualTo("☕");
        assertThat(cafe.get("color").getAsString()).isEqualTo("&e");
        assertThat(cafe.get("weight").getAsInt()).isEqualTo(70);
        assertThat(cafe.get("display").getAsString()).isEqualTo("&e☕");
        assertThat(cafe.get("chatPrefix").getAsString()).isEqualTo("&e☕ ");
        assertThat(cafe.get("permission").getAsString()).isEqualTo("muxe.medal.cafe");
    }

    @Test
    void aPlayerShowsTheChosenMedal() {
        JsonObject user = fixture.get("/api/users/" + EditorFixture.VIP, token).body().getAsJsonObject();

        assertThat(user.get("selectedMedal").getAsString()).isEqualTo("carinha");
        assertThat(fixture.get("/api/users/" + EditorFixture.ADMIN, token).body().getAsJsonObject().get("selectedMedal").isJsonNull()).isTrue();
    }

    @Test
    void savingAPlayerAsLoadedKeepsTheChosenMedal() {
        JsonObject user = fixture.get("/api/users/" + EditorFixture.VIP, token).body().getAsJsonObject();
        user.getAsJsonArray("nodes").add(JsonParser.parseString("{\"key\":\"muxe.medal.cafe\",\"value\":true}"));
        JsonObject change = new JsonObject();
        change.addProperty("type", "put-user");
        change.add("user", user);
        JsonArray changes = new JsonArray();
        changes.add(change);
        JsonObject body = new JsonObject();
        body.add("changes", changes);

        assertThat(fixture.post("/api/changes", token, body.toString()).status()).isEqualTo(200);

        PermissionUser saved = fixture.permissions.user(EditorFixture.VIP);
        assertThat(saved.selectedMedal()).contains("carinha");
        assertThat(saved.selectedTag()).contains("vip");
        assertThat(saved.nodes()).extracting(PermissionNode::key).contains("muxe.medal.cafe");
    }

    @Test
    void theChosenMedalOfAPlayerIsChangedAndCleared() {
        String chosen = "{\"changes\":[{\"type\":\"put-user\",\"user\":{\"id\":\"" + EditorFixture.VIP + "\",\"selectedMedal\":\"cafe\",\"nodes\":[{\"key\":\"group.vip\"}]}}]}";
        String cleared = "{\"changes\":[{\"type\":\"put-user\",\"user\":{\"id\":\"" + EditorFixture.VIP + "\",\"selectedMedal\":null,\"nodes\":[{\"key\":\"group.vip\"}]}}]}";

        assertThat(fixture.post("/api/changes", token, chosen).status()).isEqualTo(200);
        assertThat(fixture.permissions.user(EditorFixture.VIP).selectedMedal()).contains("cafe");
        assertThat(fixture.post("/api/changes", token, cleared).status()).isEqualTo(200);
        assertThat(fixture.permissions.user(EditorFixture.VIP).selectedMedal()).isEmpty();
    }

    @Test
    void anUnknownChosenMedalIsRefused() {
        String body = "{\"changes\":[{\"type\":\"put-user\",\"user\":{\"id\":\"" + EditorFixture.VIP + "\",\"selectedMedal\":\"inexistente\",\"nodes\":[]}}]}";

        ApiResponse response = fixture.post("/api/changes", token, body);

        assertThat(response.status()).isEqualTo(400);
        assertThat(code(response)).isEqualTo(PermissionProblems.UNKNOWN_MEDAL);
        assertThat(fixture.permissions.user(EditorFixture.VIP).selectedMedal()).contains("carinha");
    }

    @Test
    void aMedalIsCreatedGrantedToARankAndDeleted() {
        JsonObject vip = find(fixture.get("/api/data", token).body().getAsJsonObject().getAsJsonArray("groups"), "vip");
        vip.getAsJsonArray("nodes").add(JsonParser.parseString("{\"key\":\"muxe.medal.coroa\",\"value\":true}"));
        String create = "{\"changes\":[{\"type\":\"put-medal\",\"medal\":{\"id\":\"coroa\",\"name\":\"Coroa\",\"symbol\":\"⊰⊱\",\"color\":\"&e\",\"weight\":15}},"
            + "{\"type\":\"put-group\",\"group\":" + vip + "}]}";

        ApiResponse created = fixture.post("/api/changes", token, create);

        assertThat(created.status()).isEqualTo(200);
        assertThat(find(created.body().getAsJsonObject().getAsJsonObject("data").getAsJsonArray("medals"), "coroa").get("display").getAsString()).isEqualTo("&e⊰⊱");
        assertThat(fixture.sync.data().group("vip").orElseThrow().nodes()).extracting(PermissionNode::key).contains("muxe.medal.coroa");
        assertThat(fixture.permissions.recentActions(500)).extracting(action -> action.description()).contains("saved medal coroa (&e⊰⊱)");

        ApiResponse deleted = fixture.post("/api/changes", token, "{\"changes\":[{\"type\":\"delete-medal\",\"id\":\"coroa\"}]}");

        assertThat(deleted.status()).isEqualTo(200);
        assertThat(fixture.sync.data().medal("coroa")).isEmpty();
        assertThat(fixture.permissions.recentActions(500)).extracting(action -> action.description()).contains("deleted medal coroa");
    }

    @Test
    void anInvalidMedalIsABadRequest() {
        String body = "{\"changes\":[{\"type\":\"put-medal\",\"medal\":{\"id\":\"coroa\",\"name\":\"Coroa\",\"symbol\":\"coroas\",\"color\":\"&e\",\"weight\":15}}]}";

        assertThat(code(fixture.post("/api/changes", token, body))).isEqualTo(ApiResponse.INVALID_REQUEST);
        assertThat(fixture.sync.data().medal("coroa")).isEmpty();
        assertThat(fixture.permissions.recentActions(500)).isEmpty();
    }

    @Test
    void playersAreFoundByNameOrId() {
        ApiResponse byName = fixture.get("/api/users/search?name=JOANA", token);
        ApiResponse byId = fixture.get("/api/users/" + EditorFixture.VIP, token);

        assertThat(byName.status()).isEqualTo(200);
        assertThat(byName.body()).isEqualTo(byId.body());
        JsonObject user = byName.body().getAsJsonObject();
        assertThat(user.get("username").getAsString()).isEqualTo("joana");
        assertThat(user.get("selectedTag").getAsString()).isEqualTo("vip");
        assertThat(user.getAsJsonArray("nodes").get(0).getAsJsonObject().get("key").getAsString()).isEqualTo("group.vip");
    }

    @Test
    void unknownOrInvalidPlayersAreReported() {
        assertThat(fixture.get("/api/users/search?name=ninguem", token).status()).isEqualTo(404);
        assertThat(fixture.get("/api/users/search?name=a%20b", token).status()).isEqualTo(400);
    }

    @Test
    void groupMembersAreListedWithTheirNames() {
        JsonObject body = fixture.get("/api/groups/vip/members", token).body().getAsJsonObject();

        assertThat(body.get("total").getAsInt()).isEqualTo(1);
        assertThat(body.getAsJsonArray("members").get(0).getAsJsonObject().get("username").getAsString()).isEqualTo("joana");
        assertThat(fixture.get("/api/groups/inexistente/members", token).status()).isEqualTo(404);
    }

    @Test
    void aBatchCreatesATagAndARankThatInheritsAndCopiesPermissions() {
        String body = """
            {"changes":[
              {"type":"put-tag","tag":{"id":"helper","name":"Helper","color":"&e","weight":125,"prefix":null,"decorative":false}},
              {"type":"put-group","group":{"id":"helper","displayName":"Helper","weight":125,"tag":"helper","nodes":[
                {"key":"group.partner+","value":true,"contexts":{},"expiresAt":null},
                {"key":"muxe.chat.slow","value":false,"contexts":{"server":"lobby"},"expiresAt":null},
                {"key":"muxe.fly","value":true,"contexts":{},"expiresAt":1893456000000}
              ]}},
              {"type":"put-track","track":{"id":"staff","groups":["helper","mod","mod+","admin"]}}
            ]}
            """;

        ApiResponse response = fixture.post("/api/changes", token, body);

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.body().getAsJsonObject().get("saved").getAsInt()).isEqualTo(3);
        assertThat(fixture.sync.data().group("helper")).hasValueSatisfying(group -> {
            assertThat(group.parents()).containsExactly("partner+");
            assertThat(group.nodes()).hasSize(3);
        });
        assertThat(fixture.sync.data().track("staff").orElseThrow().groups()).startsWith("helper");
        assertThat(fixture.permissions.recentActions(500)).extracting(action -> action.actor()).containsOnly("tadeu");
    }

    @Test
    void aTemporaryRankIsGivenToAPlayer() {
        long expiry = EditorFixture.NOW.plus(Duration.ofDays(30)).toEpochMilli();
        String body = "{\"changes\":[{\"type\":\"put-user\",\"user\":{\"id\":\"" + EditorFixture.VIP + "\",\"selectedTag\":\"natal\",\"nodes\":["
            + "{\"key\":\"group.vip\",\"value\":true},"
            + "{\"key\":\"group.mvp\",\"value\":true,\"expiresAt\":" + expiry + "},"
            + "{\"key\":\"muxe.tag.natal\",\"value\":true}]}}]}";

        assertThat(fixture.post("/api/changes", token, body).status()).isEqualTo(200);

        PermissionUser user = fixture.permissions.user(EditorFixture.VIP);
        assertThat(user.selectedTag()).contains("natal");
        assertThat(user.nodes()).filteredOn(node -> node.key().equals("group.mvp"))
            .extracting(PermissionNode::expiresAt)
            .containsExactly(Optional.of(EditorFixture.NOW.plus(Duration.ofDays(30))));
    }

    @Test
    void anInheritanceCycleIsRefusedWithItsProblemCode() {
        String body = """
            {"changes":[{"type":"put-group","group":{"id":"membro","displayName":"Membro","weight":0,"tag":"membro","nodes":[
              {"key":"group.admin","value":true}
            ]}}]}
            """;

        ApiResponse response = fixture.post("/api/changes", token, body);

        assertThat(response.status()).isEqualTo(400);
        assertThat(code(response)).isEqualTo(PermissionProblems.INHERITANCE_CYCLE);
        assertThat(fixture.sync.data().group("membro").orElseThrow().nodes()).isEmpty();
    }

    @Test
    void theDefaultRankCannotBeDeleted() {
        ApiResponse response = fixture.post("/api/changes", token, "{\"changes\":[{\"type\":\"delete-group\",\"id\":\"membro\"}]}");

        assertThat(code(response)).isEqualTo(PermissionProblems.DEFAULT_GROUP_REQUIRED);
    }

    @Test
    void invalidInputIsABadRequest() {
        assertThat(code(fixture.post("/api/changes", token, "not json"))).isEqualTo(ApiResponse.INVALID_REQUEST);
        assertThat(code(fixture.post("/api/changes", token, "{\"changes\":[{\"type\":\"put-tag\",\"tag\":{\"id\":\"x\",\"name\":\"X\",\"color\":\"red\",\"weight\":1}}]}")))
            .isEqualTo(ApiResponse.INVALID_REQUEST);
        assertThat(code(fixture.post("/api/changes", token, "{\"changes\":[{\"type\":\"put-group\",\"group\":{\"id\":\"Bad Id\",\"displayName\":\"x\",\"weight\":1}}]}")))
            .isEqualTo(ApiResponse.INVALID_REQUEST);
        assertThat(fixture.permissions.recentActions(500)).isEmpty();
    }

    @Test
    void requestsWithoutAValidSessionAreRefused() {
        assertThat(code(fixture.get("/api/data", "wrong-token"))).isEqualTo(EditorAuthenticator.SESSION_EXPIRED);
        assertThat(fixture.api.handle(ApiRequest.get("/api/data", Optional.empty())).status()).isEqualTo(401);
    }

    @Test
    void logoutClosesTheSession() {
        assertThat(fixture.post("/api/logout", token, "{}").status()).isEqualTo(200);

        assertThat(fixture.get("/api/session", token).status()).isEqualTo(401);
    }

    @Test
    void unknownEndpointsAndMethodsAreRefused() {
        assertThat(fixture.get("/api/nothing", token).status()).isEqualTo(404);
        assertThat(fixture.get("/api/changes", token).status()).isEqualTo(405);
        assertThat(fixture.post("/api/data", token, "{}").status()).isEqualTo(405);
    }

    @Test
    void theActionLogListsTheLatestSaves() {
        fixture.post("/api/changes", token, "{\"changes\":[{\"type\":\"delete-track\",\"id\":\"staff\"}]}");

        JsonArray actions = fixture.get("/api/actions?limit=10", token).body().getAsJsonArray();

        assertThat(actions).hasSize(1);
        assertThat(actions.get(0).getAsJsonObject().get("description").getAsString()).isEqualTo("deleted track staff");
        assertThat(actions.get(0).getAsJsonObject().get("actor").getAsString()).isEqualTo("tadeu");
    }

    private static String code(ApiResponse response) {
        return response.body().getAsJsonObject().getAsJsonObject("error").get("code").getAsString();
    }

    private static List<String> ids(JsonArray array) {
        return StreamSupport.stream(array.spliterator(), false).map(element -> element.getAsJsonObject().get("id").getAsString()).toList();
    }

    private static JsonObject find(JsonArray array, String id) {
        for (JsonElement element : array) {
            if (element.getAsJsonObject().get("id").getAsString().equals(id)) {
                return element.getAsJsonObject();
            }
        }
        throw new AssertionError("Missing " + id);
    }
}
