package com.ae2smartpatternsystem.registry;

import com.ae2smartpatternsystem.TechStartForge;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class TechStartBlocks {
    public static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, TechStartForge.MODID);

    private TechStartBlocks() {
    }
}
