package com.lwx1145.sampleintegration;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Mod.EventBusSubscriber(modid = TechStart.MODID)
public class ModConfig {
    private static final String CATEGORY_PATTERN_EXPANDER = "pattern_expander";
    private static final String COMMENT_REQUIRED_MOD_IDS =
        "Allowed mod id list. Empty means allow all mods (except excluded input/output lists).";
    private static final String COMMENT_EXCLUDED_INPUT_MOD_IDS =
        "Blocked input mod id list. Blocked mods won't appear as INPUT in filter GUI and won't be used as pattern input.";
    private static final String COMMENT_EXCLUDED_OUTPUT_MOD_IDS =
        "Blocked output mod id list. Blocked mods won't appear as OUTPUT in filter GUI and won't be used as pattern output.";
    private static final String COMMENT_EXCLUDED_MOD_IDS_LEGACY =
        "Legacy blocked mod id list. If new input/output lists are empty, this list is applied to both.";

    private static Configuration config;

    public static int patternExpanderScanIntervalTicks = 300;
    public static String[] customOrePrefixes = new String[]{"gem", "cluster"};
    public static String[] customOreNames = new String[]{"ingotWeirdCopper", "mymod_special_plate"};
    public static String[] customRecipePairs = new String[]{"ingotCopper -> plateCopper"};

    public static String[] requiredModIds = new String[]{};
    public static String[] excludedInputModIds = new String[]{};
    public static String[] excludedOutputModIds = new String[]{};
    // Compatibility only: old config key.
    public static String[] excludedModIds = new String[]{};

    private static final Set<String> requiredModIdSet = new LinkedHashSet<>();
    private static final Set<String> excludedInputModIdSet = new LinkedHashSet<>();
    private static final Set<String> excludedOutputModIdSet = new LinkedHashSet<>();

    public static void init(File configFile) {
        config = new Configuration(configFile);
        sync();
    }

    private static void sync() {
        String category = CATEGORY_PATTERN_EXPANDER;
        config.addCustomCategoryComment(category, "Pattern Expander settings");

        patternExpanderScanIntervalTicks = config.getInt(
            "scanIntervalTicks",
            category,
            patternExpanderScanIntervalTicks,
            20,
            1200,
            "Scan interval in ticks (20 ticks = 1 second)."
        );
        customOrePrefixes = config.getStringList(
            "customOrePrefixes",
            category,
            customOrePrefixes,
            "Additional ore name prefixes to support (e.g., ingot, plate, ore)."
        );
        customOreNames = config.getStringList(
            "customOreNames",
            category,
            customOreNames,
            "Ore dictionary names to seed before scanning (e.g., ingotCopper, plateCopper)."
        );
        customRecipePairs = config.getStringList(
            "customRecipePairs",
            category,
            customRecipePairs,
            "Explicit ore pairs (e.g., 'ingotCopper -> plateCopper'). Applied before OreDictionary scan."
        );

        requiredModIds = config.getStringList(
            "requiredModIds",
            category,
            requiredModIds,
            COMMENT_REQUIRED_MOD_IDS
        );
        excludedModIds = config.getStringList(
            "excludedModIds",
            category,
            excludedModIds,
            COMMENT_EXCLUDED_MOD_IDS_LEGACY
        );
        excludedInputModIds = config.getStringList(
            "excludedInputModIds",
            category,
            excludedInputModIds,
            COMMENT_EXCLUDED_INPUT_MOD_IDS
        );
        excludedOutputModIds = config.getStringList(
            "excludedOutputModIds",
            category,
            excludedOutputModIds,
            COMMENT_EXCLUDED_OUTPUT_MOD_IDS
        );

        // Backward compatibility: if new lists are empty, inherit legacy list.
        if ((excludedInputModIds == null || excludedInputModIds.length == 0)
            && excludedModIds != null && excludedModIds.length > 0) {
            excludedInputModIds = excludedModIds.clone();
        }
        if ((excludedOutputModIds == null || excludedOutputModIds.length == 0)
            && excludedModIds != null && excludedModIds.length > 0) {
            excludedOutputModIds = excludedModIds.clone();
        }

        rebuildModFilterSets();
        if (config.hasChanged()) {
            config.save();
        }
    }

    public static boolean isInputStackAllowed(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() == null) {
            return true;
        }
        ResourceLocation registryName = stack.getItem().getRegistryName();
        if (registryName == null) {
            return true;
        }
        return isInputModAllowed(registryName.getNamespace());
    }

    public static boolean isOutputStackAllowed(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() == null) {
            return true;
        }
        ResourceLocation registryName = stack.getItem().getRegistryName();
        if (registryName == null) {
            return true;
        }
        return isOutputModAllowed(registryName.getNamespace());
    }

    public static boolean isInputModAllowed(String modId) {
        String normalized = normalizeModId(modId);
        if (normalized.isEmpty()) {
            return true;
        }
        if (!requiredModIdSet.isEmpty() && !requiredModIdSet.contains(normalized)) {
            return false;
        }
        return !excludedInputModIdSet.contains(normalized);
    }

    public static boolean isOutputModAllowed(String modId) {
        String normalized = normalizeModId(modId);
        if (normalized.isEmpty()) {
            return true;
        }
        if (!requiredModIdSet.isEmpty() && !requiredModIdSet.contains(normalized)) {
            return false;
        }
        return !excludedOutputModIdSet.contains(normalized);
    }

    public static String[] getExcludedInputModIds() {
        return excludedInputModIds == null ? new String[]{} : excludedInputModIds.clone();
    }

    public static String[] getExcludedOutputModIds() {
        return excludedOutputModIds == null ? new String[]{} : excludedOutputModIds.clone();
    }

    public static void setExcludedModFilters(String[] inputModIds, String[] outputModIds) {
        excludedInputModIds = inputModIds == null ? new String[]{} : inputModIds.clone();
        excludedOutputModIds = outputModIds == null ? new String[]{} : outputModIds.clone();
        excludedModIds = new String[]{}; // stop using legacy key when new filters are set
        rebuildModFilterSets();

        if (config == null) {
            return;
        }

        config.get(CATEGORY_PATTERN_EXPANDER, "excludedInputModIds", new String[]{}, COMMENT_EXCLUDED_INPUT_MOD_IDS)
            .set(excludedInputModIds);
        config.get(CATEGORY_PATTERN_EXPANDER, "excludedOutputModIds", new String[]{}, COMMENT_EXCLUDED_OUTPUT_MOD_IDS)
            .set(excludedOutputModIds);
        config.get(CATEGORY_PATTERN_EXPANDER, "excludedModIds", new String[]{}, COMMENT_EXCLUDED_MOD_IDS_LEGACY)
            .set(excludedModIds);
        if (config.hasChanged()) {
            config.save();
        }
    }

    // Backward-compatible setter used by existing call sites.
    public static void setExcludedModIds(String[] modIds) {
        setExcludedModFilters(modIds, modIds);
    }

    private static void rebuildModFilterSets() {
        requiredModIdSet.clear();
        excludedInputModIdSet.clear();
        excludedOutputModIdSet.clear();

        for (String modId : requiredModIds) {
            String normalized = normalizeModId(modId);
            if (!normalized.isEmpty()) {
                requiredModIdSet.add(normalized);
            }
        }
        for (String modId : excludedInputModIds) {
            String normalized = normalizeModId(modId);
            if (!normalized.isEmpty()) {
                excludedInputModIdSet.add(normalized);
            }
        }
        for (String modId : excludedOutputModIds) {
            String normalized = normalizeModId(modId);
            if (!normalized.isEmpty()) {
                excludedOutputModIdSet.add(normalized);
            }
        }
    }

    private static String normalizeModId(String modId) {
        if (modId == null) {
            return "";
        }
        return modId.trim().toLowerCase(Locale.ROOT);
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(TechStart.MODID)) {
            sync();
        }
    }
}
