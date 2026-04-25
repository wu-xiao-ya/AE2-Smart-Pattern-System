package com.wuxiaoya.techstart;

import com.mojang.logging.LogUtils;
import com.wuxiaoya.techstart.client.ClientModEvents;
import com.wuxiaoya.techstart.config.TechStartConfig;
import com.wuxiaoya.techstart.integration.ae2.Ae2Compat;
import com.wuxiaoya.techstart.network.TechStartNetwork;
import com.wuxiaoya.techstart.registry.TechStartBlockEntities;
import com.wuxiaoya.techstart.registry.TechStartBlocks;
import com.wuxiaoya.techstart.registry.TechStartItems;
import com.wuxiaoya.techstart.registry.TechStartMenus;
import com.wuxiaoya.techstart.registry.TechStartTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(TechStartNeoForge.MODID)
public final class TechStartNeoForge {
    public static final String MODID = "sampleintegration";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TechStartNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(ClientModEvents::onRegisterMenuScreens);
        }
        modEventBus.addListener(TechStartNetwork::registerPayloads);
        TechStartItems.ITEMS.register(modEventBus);
        TechStartMenus.MENUS.register(modEventBus);
        TechStartTabs.TABS.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, TechStartConfig.SPEC);
        if (ModList.get().isLoaded("ae2")) {
            Ae2Compat.init();
        }
        LOGGER.info("Bootstrapping {} on NeoForge 1.21.1", MODID);
    }
}
