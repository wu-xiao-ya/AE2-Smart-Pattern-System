package com.wuxiaoya.techstart.integration.ae2;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.IPatternDetailsDecoder;
import appeng.api.stacks.AEItemKey;
import com.wuxiaoya.techstart.registry.TechStartItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class TechStartPatternDecoder implements IPatternDetailsDecoder {
    static final String TAG_ENCODED = "TechStartEncoded";
    static final String TAG_ENCODED_ITEM = "EncodedItem";
    static final String TAG_INPUT_ORE = "InputOreName";
    static final String TAG_OUTPUT_ORE = "OutputOreName";
    static final String TAG_INPUT_ORES = "InputOreNames";
    static final String TAG_OUTPUT_ORES = "OutputOreNames";
    static final String TAG_VIRTUAL_INPUT_ORES = "VirtualInputOreNames";
    static final String TAG_VIRTUAL_OUTPUT_ORES = "VirtualOutputOreNames";
    static final String TAG_VIRTUAL_INPUT_STACKS = "VirtualInputStacks";
    static final String TAG_VIRTUAL_OUTPUT_STACKS = "VirtualOutputStacks";

    @Override
    public boolean isEncodedPattern(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(TechStartItems.PATTERN_INTEGRATIONS.get())) {
            return false;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return isEncodedTag(tag);
    }

    private boolean isEncodedTag(@Nullable CompoundTag tag) {
        return tag != null && (
                tag.getBoolean(TAG_ENCODED)
                        || tag.contains(TAG_ENCODED_ITEM)
                        || tag.contains(TAG_INPUT_ORE)
                        || tag.contains(TAG_OUTPUT_ORE)
                        || tag.contains(TAG_INPUT_ORES)
                        || tag.contains(TAG_OUTPUT_ORES)
                        || tag.contains(TAG_VIRTUAL_INPUT_ORES)
                        || tag.contains(TAG_VIRTUAL_OUTPUT_ORES)
                        || tag.contains(TAG_VIRTUAL_INPUT_STACKS)
                        || tag.contains(TAG_VIRTUAL_OUTPUT_STACKS));
    }

    @Override
    public @Nullable IPatternDetails decodePattern(AEItemKey what, Level level) {
        if (what == null || what.getItem() != TechStartItems.PATTERN_INTEGRATIONS.get()) {
            return null;
        }
        ItemStack stack = what.toStack();
        if (!isEncodedPattern(stack)) {
            return null;
        }
        return new TechStartPatternDetails(what, stack, level);
    }

    @Override
    public @Nullable IPatternDetails decodePattern(ItemStack what, Level level) {
        if (!isEncodedPattern(what)) {
            return null;
        }
        AEItemKey key = AEItemKey.of(what);
        if (key == null) {
            return null;
        }
        return new TechStartPatternDetails(key, what.copy(), level);
    }
}
