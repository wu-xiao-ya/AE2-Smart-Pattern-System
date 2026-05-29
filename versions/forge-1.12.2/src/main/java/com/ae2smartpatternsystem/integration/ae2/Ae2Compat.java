package com.ae2smartpatternsystem.integration.ae2;

import appeng.api.crafting.PatternDetailsHelper;
import com.ae2smartpatternsystem.TechStart;

public final class Ae2Compat {
    private static boolean initialized = false;

    private Ae2Compat() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        PatternDetailsHelper.registerDecoder(new TechStartPatternDecoder());
        TechStart.LOGGER.info("AE2S compat initialized: registered AE2S pattern decoder.");
    }
}
