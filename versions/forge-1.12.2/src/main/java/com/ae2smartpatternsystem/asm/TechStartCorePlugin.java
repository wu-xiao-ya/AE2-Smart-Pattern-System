package com.ae2smartpatternsystem.asm;


import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import zone.rong.mixinbooter.IEarlyMixinLoader;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 */
@IFMLLoadingPlugin.Name("sampleintegrationCore")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.SortingIndex(1001)
public class TechStartCorePlugin implements IFMLLoadingPlugin {
    private static final String LEGACY_PATTERN_DETAILS =
        "appeng.api.networking.crafting.ICraftingPatternDetails";

    private static boolean hasLegacyPatternApi() {
        try {
            Class.forName(LEGACY_PATTERN_DETAILS, false, TechStartCorePlugin.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
    
    @Override
    public String[] getASMTransformerClass() {
        if (!hasLegacyPatternApi()) {
            return new String[0];
        }
        return new String[] {
            "com.ae2smartpatternsystem.asm.DualityInterfaceTransformer",
            "com.ae2smartpatternsystem.asm.DSurroundItemClassTransformer",
            "com.ae2smartpatternsystem.asm.MMCEItemStackEmptyCompatTransformer",
            "com.ae2smartpatternsystem.asm.MMCEJsonUtilsCompatTransformer",
            "com.ae2smartpatternsystem.asm.MMCEPatternProviderGuiCompatTransformer",
            "com.ae2smartpatternsystem.asm.MMCEPatternFilterTransformer",
            "com.ae2smartpatternsystem.asm.MMCEPatternProviderTransformer"
        };
    }
    
    @Override
    public String getModContainerClass() {
        return null;
    }
    
    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }
    
    @Override
    public void injectData(Map<String, Object> data) {
    }
    
    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}

