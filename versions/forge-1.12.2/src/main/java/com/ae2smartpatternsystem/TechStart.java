package com.ae2smartpatternsystem;

import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraft.item.Item;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.ae2smartpatternsystem.integration.ae2.Ae2Compat;

@Mod(modid = TechStart.MODID, name = TechStart.NAME, version = TechStart.VERSION)
@Mod.EventBusSubscriber(modid = TechStart.MODID)
public class TechStart {
    public static final String MODID = "sampleintegration";
    public static final String NAME = "AE2SPS Smart Pattern System";
    public static final String VERSION = "1.0.9-beta-AE2S";
    public static final boolean AE2S_PATTERN_API = Ae2RuntimeCompat.hasAe2sPatternApi();
    
    public static final Logger LOGGER = LogManager.getLogger(NAME);
    public static TechStart INSTANCE;
    public static ItemTest ITEM_TEST;
    public static BlockPatternExpander PATTERN_EXPANDER;
    public static GuiHandler GUI_HANDLER;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        INSTANCE = this;
        LOGGER.info("Hello From {}!", NAME);
        ITEM_TEST = new ItemTest();
        GUI_HANDLER = new GuiHandler();
        PATTERN_EXPANDER = null;
        PacketHandler.register();
        ModConfig.init(event.getSuggestedConfigurationFile());
        NetworkRegistry.INSTANCE.registerGuiHandler(this, GUI_HANDLER);
    }

    @SubscribeEvent
    public static void onRegisterItems(RegistryEvent.Register<Item> event) {
        if (ITEM_TEST == null) {
            return;
        }
        event.getRegistry().register(ITEM_TEST);
        if (PATTERN_EXPANDER != null) {
            event.getRegistry().register(new net.minecraft.item.ItemBlock(PATTERN_EXPANDER)
                .setRegistryName(PATTERN_EXPANDER.getRegistryName()));
        }
    }

    @SubscribeEvent
    public static void onRegisterBlocks(RegistryEvent.Register<Block> event) {
        if (PATTERN_EXPANDER == null) {
            return;
        }
        event.getRegistry().register(PATTERN_EXPANDER);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (PATTERN_EXPANDER != null) {
            GameRegistry.registerTileEntity(TileEntityPatternExpander.class,
                new net.minecraft.util.ResourceLocation(MODID, "pattern_expander"));
        }
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        if (AE2S_PATTERN_API) {
            LOGGER.info("AE2S pattern API detected in postInit, enabling AE2S decoder + pattern provider integration.");
            Ae2Compat.init();
        } else {
            LOGGER.warn("AE2S pattern API not detected in postInit.");
            LOGGER.warn("1.0.9 targets AE2S only, so pattern-provider integration remains unavailable.");
        }
        OreDictRecipeCache.init();
    }
    
    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new com.ae2smartpatternsystem.command.CommandExpandPattern());
    }
}
