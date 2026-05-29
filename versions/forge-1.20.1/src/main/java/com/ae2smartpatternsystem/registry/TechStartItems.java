package com.ae2smartpatternsystem.registry;

import com.ae2smartpatternsystem.TechStartForge;
import com.ae2smartpatternsystem.content.PatternIntegrationsItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class TechStartItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TechStartForge.MODID);

    public static final RegistryObject<Item> PATTERN_INTEGRATIONS = ITEMS.register("pattern_integrations",
            () -> new PatternIntegrationsItem(new Item.Properties().stacksTo(1)));

    private TechStartItems() {
    }
}
