package com.ae2smartpatternsystem;

final class Ae2RuntimeCompat {
    private static final String LEGACY_PATTERN_DETAILS =
        "appeng.api.networking.crafting.ICraftingPatternDetails";
    private static final String AE2S_PATTERN_DETAILS =
        "appeng.api.crafting.IPatternDetails";

    private Ae2RuntimeCompat() {
    }

    static boolean hasLegacyPatternApi() {
        return hasClass(LEGACY_PATTERN_DETAILS);
    }

    static boolean hasAe2sPatternApi() {
        return hasClass(AE2S_PATTERN_DETAILS);
    }

    static String describePatternApi() {
        if (hasLegacyPatternApi()) {
            return "legacy";
        }
        if (hasAe2sPatternApi()) {
            return "ae2s";
        }
        return "missing";
    }

    private static boolean hasClass(String className) {
        try {
            Class.forName(className, false, Ae2RuntimeCompat.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
