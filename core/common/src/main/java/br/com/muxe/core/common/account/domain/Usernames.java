package br.com.muxe.core.common.account.domain;

import java.util.regex.Pattern;

public final class Usernames {

    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9_]{3,16}");

    private Usernames() {
    }

    public static boolean isValid(String username) {
        return username != null && VALID.matcher(username).matches();
    }
}
