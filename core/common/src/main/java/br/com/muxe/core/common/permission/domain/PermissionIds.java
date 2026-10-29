package br.com.muxe.core.common.permission.domain;

import java.util.regex.Pattern;

public final class PermissionIds {

    private static final Pattern ID = Pattern.compile("[a-z0-9_+-]{1,32}");

    private PermissionIds() {
    }

    public static boolean isValid(String id) {
        return id != null && ID.matcher(id).matches();
    }

    public static String require(String id) {
        if (!isValid(id)) {
            throw new IllegalArgumentException("Ids use lowercase letters, digits, '_', '-' and '+', up to 32 characters, got: " + id);
        }
        return id;
    }
}
