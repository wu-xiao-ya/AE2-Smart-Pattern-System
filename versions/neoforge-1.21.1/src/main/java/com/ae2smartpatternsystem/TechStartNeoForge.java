package com.ae2smartpatternsystem;

import com.mojang.logging.LogUtils;
import com.ae2smartpatternsystem.client.ClientModEvents;
import com.ae2smartpatternsystem.config.TechStartConfig;
import com.ae2smartpatternsystem.integration.ae2.Ae2Compat;
import com.ae2smartpatternsystem.network.TechStartNetwork;
import com.ae2smartpatternsystem.registry.TechStartBlockEntities;
import com.ae2smartpatternsystem.registry.TechStartBlocks;
import com.ae2smartpatternsystem.registry.TechStartItems;
import com.ae2smartpatternsystem.registry.TechStartMenus;
import com.ae2smartpatternsystem.registry.TechStartTabs;
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
        LOGGER.info("Bootstrapping AE2SPS on NeoForge 1.21.1 (modid={})", MODID);
    }
}
