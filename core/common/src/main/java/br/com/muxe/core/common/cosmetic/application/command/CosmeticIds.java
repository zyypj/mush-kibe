package br.com.muxe.core.common.cosmetic.application.command;

import java.util.regex.Pattern;

public final class CosmeticIds {

    private static final Pattern CATEGORY = Pattern.compile("[a-z0-9-]{1,32}");
    private static final Pattern COSMETIC = Pattern.compile("[a-z0-9-]{1,48}");

    private CosmeticIds() {
    }

    public static void require(String category, String cosmeticId) {
        if (category == null || !CATEGORY.matcher(category).matches()) {
            throw new IllegalArgumentException("Invalid cosmetic category: " + category);
        }
        if (cosmeticId == null || !COSMETIC.matcher(cosmeticId).matches()) {
            throw new IllegalArgumentException("Invalid cosmetic id: " + cosmeticId);
        }
    }
}
