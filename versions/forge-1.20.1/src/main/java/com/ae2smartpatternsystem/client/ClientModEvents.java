package com.ae2smartpatternsystem.client;

import com.ae2smartpatternsystem.TechStartForge;
import com.ae2smartpatternsystem.registry.TechStartMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class ClientModEvents {
    private ClientModEvents() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(TechStartMenus.PATTERN_EDITOR_ITEM.get(), PatternEditorScreen::new);
            TechStartForge.LOGGER.info("Registered screen for menu: {}:pattern_editor", TechStartForge.MODID);
        });
    }
}
