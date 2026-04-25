package com.wuxiaoya.techstart.registry;

import com.wuxiaoya.techstart.TechStartNeoForge;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TechStartBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TechStartNeoForge.MODID);

    private TechStartBlockEntities() {
    }
}
