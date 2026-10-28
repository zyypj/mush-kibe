package br.com.muxe.core.proxy.editor.security;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.permission.domain.PermissionData;
import br.com.muxe.core.common.permission.domain.PermissionNode;
import br.com.muxe.core.common.permission.domain.PermissionUser;
import br.com.muxe.core.common.permission.editor.EditorSessions;
import br.com.muxe.core.proxy.editor.EditorFixture;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EditorAuthenticatorTest {

    private final EditorFixture fixture = new EditorFixture();
    private final EditorAuthenticator authenticator = new EditorAuthenticator(fixture.sessions, fixture.queries, fixture.sync::data, fixture.clock);

    @AfterEach
    void close() {
        fixture.close();
    }

    @Test
    void anAdminSessionIsGrantedUntilItsEnd() {
        String token = fixture.adminToken();

        Authentication result = authenticator.authenticate(Optional.of("Bearer " + token));

        assertThat(result).isInstanceOfSatisfying(Authentication.Granted.class, granted -> {
            assertThat(granted.session().actorName()).isEqualTo("tadeu");
            assertThat(granted.expiresAt()).isEqualTo(EditorFixture.NOW.plus(Duration.ofMinutes(30)));
            assertThat(granted.toString()).doesNotContain(token);
        });
    }

    @Test
    void aMissingOrMalformedHeaderIsUnauthenticated() {
        assertThat(denied(Optional.empty()).code()).isEqualTo(EditorAuthenticator.UNAUTHENTICATED);
        assertThat(denied(Optional.of("Basic abc")).code()).isEqualTo(EditorAuthenticator.UNAUTHENTICATED);
        assertThat(denied(Optional.of("Bearer ")).code()).isEqualTo(EditorAuthenticator.UNAUTHENTICATED);
    }

    @Test
    void anExpiredSessionIsRefused() {
        String token = fixture.adminToken();
        fixture.clock.advance(Duration.ofMinutes(31));

        assertThat(denied(Optional.of("Bearer " + token)).code()).isEqualTo(EditorAuthenticator.SESSION_EXPIRED);
    }

    @Test
    void aPlayerWhoLostThePermissionIsRefusedAndTheSessionIsClosed() {
        String token = fixture.adminToken();
        fixture.permissions.saveUser(new PermissionUser(EditorFixture.ADMIN, List.of(PermissionNode.group("vip", Optional.empty(), EditorFixture.NOW)), Optional.empty()));

        Authentication.Denied denied = denied(Optional.of("Bearer " + token));

        assertThat(denied.status()).isEqualTo(403);
        assertThat(denied.code()).isEqualTo(EditorAuthenticator.FORBIDDEN);
        assertThat(fixture.sessions.find(token)).isEmpty();
    }

    @Test
    void aLinkOpenedFromTheConsoleIsGranted() {
        String token = fixture.sessions.open(EditorSessions.CONSOLE, "Console");

        assertThat(authenticator.authenticate(Optional.of("Bearer " + token))).isInstanceOfSatisfying(Authentication.Granted.class,
            granted -> assertThat(granted.session().actorName()).isEqualTo("Console"));
    }

    @Test
    void theEditorPermissionAloneIsEnough() {
        fixture.permissions.saveUser(new PermissionUser(EditorFixture.VIP, List.of(PermissionNode.permanent("muxe.permissions.editor", true, EditorFixture.NOW)), Optional.empty()));
        String token = fixture.sessions.open(EditorFixture.VIP, "joana");

        assertThat(authenticator.authenticate(Optional.of("bearer " + token))).isInstanceOf(Authentication.Granted.class);
    }

    @Test
    void nothingIsDecidedBeforeTheGroupsLoad() {
        String token = fixture.adminToken();
        EditorAuthenticator loading = new EditorAuthenticator(fixture.sessions, fixture.queries, () -> PermissionData.EMPTY, fixture.clock);

        Authentication result = loading.authenticate(Optional.of("Bearer " + token));

        assertThat(result).isInstanceOfSatisfying(Authentication.Denied.class, denied -> assertThat(denied.status()).isEqualTo(503));
        assertThat(fixture.sessions.find(token)).isPresent();
    }

    private Authentication.Denied denied(Optional<String> header) {
        return (Authentication.Denied) authenticator.authenticate(header);
    }
}
