package br.com.muxe.core.common.placement.domain;

import java.util.regex.Pattern;

public final class PlacementIds {

    private static final Pattern ID = Pattern.compile("[a-z0-9_-]{1,32}");

    private PlacementIds() {
    }

    public static boolean isValid(String id) {
        return id != null && ID.matcher(id).matches();
    }

    static String require(String id) {
        if (!isValid(id)) {
            throw new IllegalArgumentException("Ids use lowercase letters, digits, '-' and '_', up to 32 characters, got: " + id);
        }
        return id;
    }

    static String requireGroup(String group) {
        if (group == null || group.isBlank()) {
            throw new IllegalArgumentException("A placement needs a server group");
        }
        return group;
    }
}
