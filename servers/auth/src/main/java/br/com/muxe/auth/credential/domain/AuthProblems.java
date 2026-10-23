package br.com.muxe.auth.credential.domain;

public final class AuthProblems {

    public static final String ALREADY_REGISTERED = "auth.already_registered";
    public static final String NOT_REGISTERED = "auth.not_registered";
    public static final String WRONG_PASSWORD = "auth.wrong_password";
    public static final String PASSWORDS_DO_NOT_MATCH = "auth.passwords_do_not_match";
    public static final String PASSWORD_TOO_SHORT = "auth.password_too_short";
    public static final String PASSWORD_TOO_LONG = "auth.password_too_long";
    public static final String PASSWORD_EQUALS_USERNAME = "auth.password_equals_username";

    private AuthProblems() {
    }
}
