package com.wuxiaoya.techstart.client;

import com.wuxiaoya.techstart.TechStartNeoForge;
import com.wuxiaoya.techstart.registry.TechStartMenus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class ClientModEvents {
    private ClientModEvents() {
    }

    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(TechStartMenus.PATTERN_EDITOR_ITEM.get(), PatternEditorScreen::new);
        TechStartNeoForge.LOGGER.info("Registered screen for menu: {}:pattern_editor", TechStartNeoForge.MODID);
    }
}
