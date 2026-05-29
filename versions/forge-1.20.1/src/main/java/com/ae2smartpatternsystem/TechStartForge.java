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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(TechStartForge.MODID)
public class TechStartForge {
    public static final String MODID = "sampleintegration";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TechStartForge() {
        FMLJavaModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, TechStartConfig.SPEC);
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::onCommonSetup);
        TechStartBlocks.BLOCKS.register(modEventBus);
        TechStartItems.ITEMS.register(modEventBus);
        TechStartBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        TechStartMenus.MENUS.register(modEventBus);
        TechStartTabs.TABS.register(modEventBus);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(ClientModEvents::onClientSetup);
        }
        MinecraftForge.EVENT_BUS.register(this);
        TechStartNetwork.register();
        LOGGER.info("AE2SPS Forge 1.20.1 bootstrap complete.");
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        if (ModList.get().isLoaded("ae2")) {
            event.enqueueWork(Ae2Compat::init);
        }
    }
}
