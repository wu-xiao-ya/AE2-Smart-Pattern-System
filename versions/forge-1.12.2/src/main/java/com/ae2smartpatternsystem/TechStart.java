package com.ae2smartpatternsystem;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraft.item.Item;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.ae2smartpatternsystem.integration.ae2.Ae2Compat;

@Mod(
    modid = TechStart.MODID,
    name = TechStart.NAME,
    version = TechStart.VERSION,
    dependencies = TechStart.AE2S_DEPENDENCY
)
@Mod.EventBusSubscriber(modid = TechStart.MODID)
public class TechStart {
    public static final String MODID = "sampleintegration";
    public static final String NAME = "AE2SPS Smart Pattern System";
    public static final String VERSION = "1.0.9-beta-AE2S";
    public static final String AE2S_DEPENDENCY = "required-after:ae2@[1.0.0,)";
    public static final boolean AE2S_PATTERN_API = Ae2RuntimeCompat.hasAe2sPatternApi();
    
    public static final Logger LOGGER = LogManager.getLogger(NAME);
    public static TechStart INSTANCE;
    public static ItemTest ITEM_TEST;
    public static GuiHandler GUI_HANDLER;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        if (!AE2S_PATTERN_API) {
            throw new IllegalStateException(
                NAME + " " + VERSION + " requires Applied Energistics 2 - Supergiant (AE2S) 1.0.0+ "
                    + "with appeng.api.crafting.IPatternDetails."
            );
        }
        INSTANCE = this;
        LOGGER.info("Starting {}.", NAME);
        ITEM_TEST = new ItemTest();
        GUI_HANDLER = new GuiHandler();
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
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        LOGGER.info("AE2S pattern API detected in postInit, enabling AE2SPS decoder and pattern provider integration.");
        Ae2Compat.init();
        OreDictRecipeCache.init();
    }
    
}
