package com.ae2smartpatternsystem;

final class Ae2RuntimeCompat {
    private static final String AE2S_PATTERN_DETAILS =
        "appeng.api.crafting.IPatternDetails";

    private Ae2RuntimeCompat() {
    }

    static boolean hasAe2sPatternApi() {
        return hasClass(AE2S_PATTERN_DETAILS);
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
