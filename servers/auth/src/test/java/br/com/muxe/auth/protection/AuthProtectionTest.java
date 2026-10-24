package br.com.muxe.auth.protection;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AuthProtectionTest {

    @Test
    void onlyAuthenticatedPlayersInBuildModeMayInteract() {
        assertThat(AuthProtection.mayInteract(false, false)).isFalse();
        assertThat(AuthProtection.mayInteract(false, true)).isFalse();
        assertThat(AuthProtection.mayInteract(true, false)).isFalse();
        assertThat(AuthProtection.mayInteract(true, true)).isTrue();
    }

    @Test
    void unauthenticatedPlayersOnlyRunTheLoginCommands() {
        assertThat(AuthProtection.mayRunCommand(false, "/logar senha123")).isTrue();
        assertThat(AuthProtection.mayRunCommand(false, "/REGISTRAR a b")).isTrue();
        assertThat(AuthProtection.mayRunCommand(false, "/login senha123")).isTrue();
        assertThat(AuthProtection.mayRunCommand(false, "/register a b")).isTrue();
        assertThat(AuthProtection.mayRunCommand(false, "/spawn")).isFalse();
        assertThat(AuthProtection.mayRunCommand(false, "/authsetup spawn")).isFalse();
    }

    @Test
    void namespacedCommandsCannotSneakPastTheFilter() {
        assertThat(AuthProtection.mayRunCommand(false, "/muxe-auth:logar senha")).isTrue();
        assertThat(AuthProtection.mayRunCommand(false, "/bukkit:plugins")).isFalse();
        assertThat(AuthProtection.mayRunCommand(false, "/   ")).isFalse();
    }

    @Test
    void authenticatedPlayersRunAnyCommand() {
        assertThat(AuthProtection.mayRunCommand(true, "/lobby")).isTrue();
    }

    @Test
    void playersBelowTheVoidLevelFellOut() {
        assertThat(AuthProtection.fellOut(-0.5, 0)).isTrue();
        assertThat(AuthProtection.fellOut(0, 0)).isFalse();
        assertThat(AuthProtection.fellOut(64, 0)).isFalse();
    }
}
