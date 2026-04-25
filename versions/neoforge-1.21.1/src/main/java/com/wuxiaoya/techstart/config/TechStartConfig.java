package com.wuxiaoya.techstart.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class TechStartConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.IntValue ITEM_MARKER_MAX_AMOUNT;
    private static final ModConfigSpec.IntValue FLUID_GAS_MARKER_MAX_AMOUNT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("pattern_editor");
        ITEM_MARKER_MAX_AMOUNT = builder
                .comment("Maximum amount allowed for item pattern markers.")
                .defineInRange("item_marker_max_amount", 10_000, 1, Integer.MAX_VALUE);
        FLUID_GAS_MARKER_MAX_AMOUNT = builder
                .comment("Maximum amount allowed for fluid and gas pattern markers.")
                .defineInRange("fluid_gas_marker_max_amount", Integer.MAX_VALUE, 1, Integer.MAX_VALUE);
        builder.pop();

        SPEC = builder.build();
    }

    private TechStartConfig() {
    }

    public static int getItemMarkerMaxAmount() {
        return Math.max(1, ITEM_MARKER_MAX_AMOUNT.get());
    }

    public static int getFluidGasMarkerMaxAmount() {
        return Math.max(1, FLUID_GAS_MARKER_MAX_AMOUNT.get());
    }
}
