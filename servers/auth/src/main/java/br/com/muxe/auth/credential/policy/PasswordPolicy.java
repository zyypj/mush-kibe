package br.com.muxe.auth.credential.policy;

import br.com.muxe.auth.credential.domain.AuthProblems;
import br.com.muxe.core.common.result.Problem;
import java.util.Optional;

public record PasswordPolicy(int minLength, int maxLength) {

    public PasswordPolicy {
        if (minLength < 1 || maxLength < minLength) {
            throw new IllegalArgumentException("Invalid password length limits");
        }
    }

    public Optional<Problem> validate(String username, String password, String confirmation) {
        if (!password.equals(confirmation)) {
            return Optional.of(Problem.of(AuthProblems.PASSWORDS_DO_NOT_MATCH, "Passwords do not match"));
        }
        if (password.length() < minLength) {
            return Optional.of(Problem.of(AuthProblems.PASSWORD_TOO_SHORT, "Password shorter than " + minLength));
        }
        if (password.length() > maxLength) {
            return Optional.of(Problem.of(AuthProblems.PASSWORD_TOO_LONG, "Password longer than " + maxLength));
        }
        if (password.equalsIgnoreCase(username)) {
            return Optional.of(Problem.of(AuthProblems.PASSWORD_EQUALS_USERNAME, "Password equals the username"));
        }
        return Optional.empty();
    }
}
