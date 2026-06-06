package com.ae2smartpatternsystem.client;


import com.ae2smartpatternsystem.TechStart;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = TechStart.MODID, value = Side.CLIENT)
public final class ModelRegistryHandler {

    private ModelRegistryHandler() {}

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        if (TechStart.ITEM_TEST != null) {
            registerItemModel(TechStart.ITEM_TEST);
        }
    }

    private static void registerItemModel(Item item) {
        if (item == null || item.getRegistryName() == null) {
            return;
        }
        ModelLoader.setCustomModelResourceLocation(item, 0,
            new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }
}
