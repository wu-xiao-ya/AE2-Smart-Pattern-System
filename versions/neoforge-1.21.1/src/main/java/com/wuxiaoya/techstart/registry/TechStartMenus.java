package com.wuxiaoya.techstart.registry;

import com.wuxiaoya.techstart.TechStartNeoForge;
import com.wuxiaoya.techstart.menu.PatternEditorMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TechStartMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, TechStartNeoForge.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<PatternEditorMenu>> PATTERN_EDITOR_ITEM =
            MENUS.register("pattern_editor_item", () -> IMenuTypeExtension.create(PatternEditorMenu::createItemMenu));

    private TechStartMenus() {
    }
}
