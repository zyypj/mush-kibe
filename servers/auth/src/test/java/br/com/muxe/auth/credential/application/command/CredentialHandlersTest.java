package br.com.muxe.auth.credential.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.auth.credential.InMemoryCredentialRepository;
import br.com.muxe.auth.credential.crypto.Pbkdf2PasswordHasher;
import br.com.muxe.auth.credential.domain.AuthProblems;
import br.com.muxe.auth.credential.domain.Credential;
import br.com.muxe.auth.credential.policy.PasswordPolicy;
import br.com.muxe.core.common.result.Result;
import br.com.muxe.core.common.testing.DirectTransactions;
import br.com.muxe.core.common.testing.MutableClock;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CredentialHandlersTest {

    private final MutableClock clock = MutableClock.startingAt("2026-01-01T00:00:00Z");
    private final InMemoryCredentialRepository credentials = new InMemoryCredentialRepository();
    private final DirectTransactions transactions = new DirectTransactions();
    private final Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher(1_000, new SecureRandom());
    private final RegisterCredentialHandler register = new RegisterCredentialHandler(credentials, hasher, new PasswordPolicy(6, 64), transactions, clock);
    private final VerifyCredentialHandler verify = new VerifyCredentialHandler(credentials, hasher, transactions, clock);
    private final UUID player = UUID.randomUUID();

    @Test
    void registersANewPlayer() {
        Result<Void> result = register.handle(new RegisterCredentialCommand(player, "Steve", "minhasenha", "minhasenha"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(credentials.exists(player)).isTrue();
    }

    @Test
    void refusesToRegisterTwice() {
        register.handle(new RegisterCredentialCommand(player, "Steve", "minhasenha", "minhasenha"));

        Result<Void> second = register.handle(new RegisterCredentialCommand(player, "Steve", "outrasenha", "outrasenha"));

        assertProblem(second, AuthProblems.ALREADY_REGISTERED);
    }

    @Test
    void validatesThePasswordBeforeHashing() {
        assertProblem(register.handle(new RegisterCredentialCommand(player, "Steve", "abc", "abc")), AuthProblems.PASSWORD_TOO_SHORT);
        assertProblem(register.handle(new RegisterCredentialCommand(player, "Steve", "minhasenha", "outra")), AuthProblems.PASSWORDS_DO_NOT_MATCH);
        assertProblem(register.handle(new RegisterCredentialCommand(player, "Steve123", "steve123", "steve123")), AuthProblems.PASSWORD_EQUALS_USERNAME);
        assertThat(credentials.exists(player)).isFalse();
    }

    @Test
    void verifiesTheRightPasswordAndRecordsTheLogin() {
        register.handle(new RegisterCredentialCommand(player, "Steve", "minhasenha", "minhasenha"));
        clock.advance(Duration.ofHours(1));

        Result<Void> result = verify.handle(new VerifyCredentialCommand(player, "minhasenha"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(credentials.find(player)).map(Credential::lastLoginAt).contains(clock.instant());
    }

    @Test
    void rejectsTheWrongPassword() {
        register.handle(new RegisterCredentialCommand(player, "Steve", "minhasenha", "minhasenha"));

        assertProblem(verify.handle(new VerifyCredentialCommand(player, "errada")), AuthProblems.WRONG_PASSWORD);
    }

    @Test
    void rejectsUnregisteredPlayers() {
        assertProblem(verify.handle(new VerifyCredentialCommand(player, "minhasenha")), AuthProblems.NOT_REGISTERED);
    }

    @Test
    void upgradesWeakHashesOnLogin() {
        RegisterCredentialHandler weakRegister = new RegisterCredentialHandler(credentials, new Pbkdf2PasswordHasher(500, new SecureRandom()), new PasswordPolicy(6, 64), transactions, clock);
        weakRegister.handle(new RegisterCredentialCommand(player, "Steve", "minhasenha", "minhasenha"));

        verify.handle(new VerifyCredentialCommand(player, "minhasenha"));

        assertThat(credentials.find(player).orElseThrow().passwordHash().iterations()).isEqualTo(1_000);
    }

    private static void assertProblem(Result<Void> result, String code) {
        assertThat(result).isInstanceOfSatisfying(Result.Failure.class, failure -> assertThat(failure.problem().code()).isEqualTo(code));
    }
}
