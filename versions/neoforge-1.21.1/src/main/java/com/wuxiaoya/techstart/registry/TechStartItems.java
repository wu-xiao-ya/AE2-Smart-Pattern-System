package com.wuxiaoya.techstart.registry;

import com.wuxiaoya.techstart.TechStartNeoForge;
import com.wuxiaoya.techstart.content.PatternIntegrationsItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TechStartItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TechStartNeoForge.MODID);

    public static final DeferredItem<Item> PATTERN_INTEGRATIONS = ITEMS.register("pattern_integrations",
            () -> new PatternIntegrationsItem(new Item.Properties().stacksTo(1)));

    private TechStartItems() {
    }
}
