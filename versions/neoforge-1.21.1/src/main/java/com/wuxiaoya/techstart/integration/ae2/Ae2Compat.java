package com.wuxiaoya.techstart.integration.ae2;

import appeng.api.crafting.PatternDetailsHelper;
import com.wuxiaoya.techstart.TechStartNeoForge;

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
        TechStartNeoForge.LOGGER.info("AE2 compat initialized: registered TechStart pattern decoder.");
    }
}
