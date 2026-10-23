package br.com.muxe.core.common.account.domain;

import br.com.muxe.core.common.result.Problem;

public final class AccountProblems {

    public static final String INVALID_USERNAME = "account.invalid_username";
    public static final String NAME_CASE_CONFLICT = "account.name_case_conflict";

    private AccountProblems() {
    }

    public static Problem invalidUsername(String username) {
        return Problem.of(INVALID_USERNAME, "Username '" + username + "' is not allowed");
    }

    public static Problem nameCaseConflict(String requested, String registered) {
        return Problem.of(NAME_CASE_CONFLICT, "Username '" + requested + "' is already registered as '" + registered + "'");
    }
}
