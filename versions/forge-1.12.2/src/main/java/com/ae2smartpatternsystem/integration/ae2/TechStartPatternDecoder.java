package com.ae2smartpatternsystem.integration.ae2;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.IPatternDetailsDecoder;
import appeng.api.stacks.AEItemKey;
import com.ae2smartpatternsystem.ItemTest;
import com.ae2smartpatternsystem.LegacyPatternNbtKeys;
import com.ae2smartpatternsystem.TechStart;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public final class TechStartPatternDecoder implements IPatternDetailsDecoder {
    private static final String TAG_ENCODED = LegacyPatternNbtKeys.TAG_ENCODED;
    private static final String TAG_ENCODED_ITEM = LegacyPatternNbtKeys.TAG_ENCODED_ITEM;
    private static final String TAG_INPUTS = LegacyPatternNbtKeys.TAG_INPUTS;
    private static final String TAG_OUTPUTS = LegacyPatternNbtKeys.TAG_OUTPUTS;
    private static final String TAG_INPUT_ORE = "InputOreName";
    private static final String TAG_OUTPUT_ORE = "OutputOreName";
    private static final String TAG_INPUT_ORES = "InputOreNames";
    private static final String TAG_OUTPUT_ORES = "OutputOreNames";
    private static final String TAG_INPUT_COUNTS = "InputCounts";
    private static final String TAG_OUTPUT_COUNTS = "OutputCounts";
    private static final String TAG_INPUT_FLUIDS = "InputFluids";
    private static final String TAG_OUTPUT_FLUIDS = "OutputFluids";
    private static final String TAG_INPUT_GASES = "InputGases";
    private static final String TAG_OUTPUT_GASES = "OutputGases";
    private static final String TAG_VIRTUAL_INPUT_ORES = "VirtualInputOreNames";
    private static final String TAG_VIRTUAL_OUTPUT_ORES = "VirtualOutputOreNames";
    private static final String TAG_VIRTUAL_INPUT_STACKS = "VirtualInputStacks";
    private static final String TAG_VIRTUAL_OUTPUT_STACKS = "VirtualOutputStacks";

    @Override
    public boolean isEncodedPattern(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ItemTest)) {
            return false;
        }
        boolean encoded = isEncodedTag(stack.getTagCompound());
        if (encoded) {
            TechStart.LOGGER.info(
                "AE2SPS decoder accepted pattern stack: {}, inputs={}, outputs={}",
                stack,
                ItemTest.getInputOreNamesStatic(stack),
                ItemTest.getOutputOreNamesStatic(stack)
            );
        }
        return encoded;
    }

    @Override
    public IPatternDetails decodePattern(AEItemKey what, World world) {
        if (what == null || what.getItem() == null) {
            return null;
        }
        ItemStack stack = what.toStack();
        if (!isEncodedPattern(stack)) {
            return null;
        }
        try {
            return new TechStartPatternDetails(what, stack.copy());
        } catch (IllegalArgumentException ignored) {
            TechStart.LOGGER.warn("AE2SPS decoder rejected encoded pattern key due to invalid IO payload: {}", stack);
            return null;
        }
    }

    @Override
    public IPatternDetails decodePattern(ItemStack what, World world) {
        if (!isEncodedPattern(what)) {
            return null;
        }
        AEItemKey key = AEItemKey.of(what);
        if (key == null) {
            return null;
        }
        try {
            return new TechStartPatternDetails(key, what.copy());
        } catch (IllegalArgumentException ignored) {
            TechStart.LOGGER.warn("AE2SPS decoder rejected encoded pattern stack due to invalid IO payload: {}", what);
            return null;
        }
    }

    private boolean isEncodedTag(NBTTagCompound tag) {
        return tag != null && (
            tag.getBoolean(TAG_ENCODED)
                || tag.hasKey(TAG_ENCODED_ITEM)
                || tag.hasKey(TAG_INPUTS)
                || tag.hasKey(TAG_OUTPUTS)
                || tag.hasKey(TAG_INPUT_ORE)
                || tag.hasKey(TAG_OUTPUT_ORE)
                || tag.hasKey(TAG_INPUT_ORES)
                || tag.hasKey(TAG_OUTPUT_ORES)
                || tag.hasKey(TAG_INPUT_COUNTS)
                || tag.hasKey(TAG_OUTPUT_COUNTS)
                || tag.hasKey(TAG_INPUT_FLUIDS)
                || tag.hasKey(TAG_OUTPUT_FLUIDS)
                || tag.hasKey(TAG_INPUT_GASES)
                || tag.hasKey(TAG_OUTPUT_GASES)
                || tag.hasKey(TAG_VIRTUAL_INPUT_ORES)
                || tag.hasKey(TAG_VIRTUAL_OUTPUT_ORES)
                || tag.hasKey(TAG_VIRTUAL_INPUT_STACKS)
                || tag.hasKey(TAG_VIRTUAL_OUTPUT_STACKS)
        );
    }
}
