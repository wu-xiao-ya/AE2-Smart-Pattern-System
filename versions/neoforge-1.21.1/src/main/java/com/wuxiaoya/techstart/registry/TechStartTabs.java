package com.wuxiaoya.techstart.registry;

import com.wuxiaoya.techstart.TechStartNeoForge;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TechStartTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TechStartNeoForge.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.sampleintegration.main"))
                    .icon(() -> new ItemStack(TechStartItems.PATTERN_INTEGRATIONS.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(TechStartItems.PATTERN_INTEGRATIONS.get());
                    })
                    .build());

    private TechStartTabs() {
    }
}
