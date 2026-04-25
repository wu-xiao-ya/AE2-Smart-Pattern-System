package com.wuxiaoya.techstart.content;

import com.wuxiaoya.techstart.integration.mekanism.MekanismGasHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import com.wuxiaoya.techstart.menu.PatternEditorMenu;

import java.util.ArrayList;
import java.util.List;

public class PatternIntegrationsItem extends Item {
    private static final String TAG_ENCODED = "TechStartEncoded";
    private static final String TAG_INPUTS = "TechStartInputs";
    private static final String TAG_OUTPUTS = "TechStartOutputs";
    private static final String TAG_ENCODED_ITEM = "EncodedItem";
    private static final String TAG_FILTER_MODE = "TechStartFilterMode";
    private static final String TAG_FILTER_MODE_LEGACY = "FilterMode";
    private static final String TAG_FILTER_ENTRIES = "FilterEntries";
    private static final String TAG_INPUT_FLUIDS = "InputFluids";
    private static final String TAG_INPUT_FLUID_AMOUNTS = "InputFluidAmounts";
    private static final String TAG_OUTPUT_FLUIDS = "OutputFluids";
    private static final String TAG_OUTPUT_FLUID_AMOUNTS = "OutputFluidAmounts";
    private static final String TAG_INPUT_GASES = "InputGases";
    private static final String TAG_INPUT_GAS_AMOUNTS = "InputGasAmounts";
    private static final String TAG_OUTPUT_GASES = "OutputGases";
    private static final String TAG_OUTPUT_GAS_AMOUNTS = "OutputGasAmounts";
    private static final String TAG_EXCLUDED_INPUT_MOD_IDS = "ExcludedInputModIds";
    private static final String TAG_EXCLUDED_OUTPUT_MOD_IDS = "ExcludedOutputModIds";
    private static final String TAG_FLUID_MARKER = "TechStartFluidMarker";
    private static final String TAG_FLUID_NAME = "TechStartFluidName";
    private static final String TAG_FLUID_AMOUNT = "TechStartFluidAmount";
    private static final String TAG_GAS_MARKER = "TechStartGasMarker";
    private static final String TAG_GAS_NAME = "TechStartGasName";
    private static final String TAG_GAS_AMOUNT = "TechStartGasAmount";
    private static final String TAG_ITEM_MARKER = "TechStartItemMarker";
    private static final String TAG_ITEM_AMOUNT = "TechStartItemAmount";
    private static final String TAG_SLOT = "Slot";
    private static final String TAG_STACK = "Stack";
    private static final int FILTER_MODE_WHITELIST = 0;
    private static final int FILTER_MODE_BLACKLIST = 1;

    public PatternIntegrationsItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (!level.isClientSide && player.isShiftKeyDown()) {
            clearPatternData(stack);
            player.sendSystemMessage(Component.translatable("message.techstart.pattern_cleared"));
            return InteractionResultHolder.success(stack);
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(
                    new SimpleMenuProvider(
                            (containerId, playerInventory, targetPlayer) -> new PatternEditorMenu(containerId, playerInventory, usedHand),
                            Component.translatable("gui.techstart.pattern_editor")),
                    buf -> buf.writeEnum(usedHand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag isAdvanced) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        boolean encoded = tag.getBoolean(TAG_ENCODED);

        String encodedLabel = Component.translatable(encoded ? "tooltip.techstart.bool_yes" : "tooltip.techstart.bool_no").getString();
        if (tag.contains(TAG_ENCODED_ITEM, Tag.TAG_STRING)) {
            String encodedName = tag.getString(TAG_ENCODED_ITEM).trim();
            if (!encodedName.isEmpty()) {
                encodedLabel = encodedName;
            }
        }

        tooltip.add(Component.translatable("tooltip.techstart.encoded", encodedLabel)
                .withStyle(encoded ? ChatFormatting.GREEN : ChatFormatting.RED));

        int filterMode = readFilterMode(tag);
        Component modeText = Component.translatable(filterMode == FILTER_MODE_WHITELIST
                ? "tooltip.techstart.filter_mode_whitelist"
                : "tooltip.techstart.filter_mode_blacklist");
        tooltip.add(Component.translatable("tooltip.techstart.filter_mode", modeText)
                .withStyle(filterMode == FILTER_MODE_WHITELIST ? ChatFormatting.AQUA : ChatFormatting.GOLD));

        HolderLookup.Provider registries = context.registries();
        if (encoded && registries != null) {
            List<Component> inputLines = buildEntryLines(tag, TAG_INPUTS, true, registries);
            List<Component> outputLines = buildEntryLines(tag, TAG_OUTPUTS, false, registries);
            tooltip.add(Component.translatable("tooltip.techstart.input_count", inputLines.size()).withStyle(ChatFormatting.GRAY));
            tooltip.addAll(inputLines);
            tooltip.add(Component.translatable("tooltip.techstart.output_count", outputLines.size()).withStyle(ChatFormatting.GRAY));
            tooltip.addAll(outputLines);
        }

        tooltip.add(Component.translatable("tooltip.techstart.open_pattern_editor").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.techstart.mark_hint").withStyle(ChatFormatting.GRAY));
    }

    private void clearPatternData(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.isEmpty()) {
            return;
        }
        tag.remove(TAG_ENCODED);
        tag.remove(TAG_INPUTS);
        tag.remove(TAG_OUTPUTS);
        tag.remove(TAG_ENCODED_ITEM);
        tag.remove(TAG_FILTER_MODE);
        tag.remove(TAG_FILTER_MODE_LEGACY);
        tag.remove(TAG_FILTER_ENTRIES);
        tag.remove(TAG_INPUT_FLUIDS);
        tag.remove(TAG_INPUT_FLUID_AMOUNTS);
        tag.remove(TAG_OUTPUT_FLUIDS);
        tag.remove(TAG_OUTPUT_FLUID_AMOUNTS);
        tag.remove(TAG_INPUT_GASES);
        tag.remove(TAG_INPUT_GAS_AMOUNTS);
        tag.remove(TAG_OUTPUT_GASES);
        tag.remove(TAG_OUTPUT_GAS_AMOUNTS);
        tag.remove(TAG_EXCLUDED_INPUT_MOD_IDS);
        tag.remove(TAG_EXCLUDED_OUTPUT_MOD_IDS);
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }

    private int readFilterMode(CompoundTag tag) {
        if (tag.contains(TAG_FILTER_MODE, Tag.TAG_INT)) {
            return tag.getInt(TAG_FILTER_MODE) == FILTER_MODE_WHITELIST ? FILTER_MODE_WHITELIST : FILTER_MODE_BLACKLIST;
        }
        if (tag.contains(TAG_FILTER_MODE_LEGACY, Tag.TAG_INT)) {
            return tag.getInt(TAG_FILTER_MODE_LEGACY) == FILTER_MODE_WHITELIST ? FILTER_MODE_WHITELIST : FILTER_MODE_BLACKLIST;
        }
        return FILTER_MODE_BLACKLIST;
    }

    private List<Component> buildEntryLines(CompoundTag tag, String listKey, boolean input, HolderLookup.Provider registries) {
        if (!tag.contains(listKey, Tag.TAG_LIST)) {
            return List.of();
        }

        ListTag rawEntries = tag.getList(listKey, Tag.TAG_COMPOUND);
        List<SlotEntry> entries = new ArrayList<>(rawEntries.size());
        for (Tag raw : rawEntries) {
            if (!(raw instanceof CompoundTag entry)) {
                continue;
            }
            if (!entry.contains(TAG_SLOT, Tag.TAG_INT) || !entry.contains(TAG_STACK, Tag.TAG_COMPOUND)) {
                continue;
            }
            ItemStack entryStack = ItemStack.parseOptional(registries, entry.getCompound(TAG_STACK));
            if (entryStack.isEmpty()) {
                continue;
            }
            entries.add(new SlotEntry(entry.getInt(TAG_SLOT), entryStack));
        }

        entries.sort((left, right) -> Integer.compare(left.slot, right.slot));
        if (entries.isEmpty()) {
            return List.of();
        }

        List<Component> lines = new ArrayList<>(entries.size());
        for (int index = 0; index < entries.size(); index++) {
            ItemStack entryStack = entries.get(index).stack;
            lines.add(Component.translatable(resolveTooltipKey(entryStack, input), index + 1, resolveDisplayName(entryStack), getLogicalStackAmount(entryStack))
                    .withStyle(resolveTooltipStyle(entryStack)));
        }
        return lines;
    }

    private String resolveTooltipKey(ItemStack stack, boolean input) {
        if (isFluidMarkerStack(stack)) {
            return input ? "tooltip.techstart.input_fluid" : "tooltip.techstart.output_fluid";
        }
        if (isGasMarkerStack(stack)) {
            return input ? "tooltip.techstart.input_gas" : "tooltip.techstart.output_gas";
        }
        return input ? "tooltip.techstart.input_item" : "tooltip.techstart.output_item";
    }

    private ChatFormatting resolveTooltipStyle(ItemStack stack) {
        if (isFluidMarkerStack(stack) || isGasMarkerStack(stack)) {
            return ChatFormatting.DARK_AQUA;
        }
        return ChatFormatting.DARK_GRAY;
    }

    private Component resolveDisplayName(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.getBoolean(TAG_FLUID_MARKER)) {
            return getFluidDisplayName(tag.getString(TAG_FLUID_NAME), Math.max(1, tag.getInt(TAG_FLUID_AMOUNT)));
        }
        if (tag.getBoolean(TAG_GAS_MARKER)) {
            return MekanismGasHelper.getDisplayName(tag.getString(TAG_GAS_NAME));
        }
        return stack.getHoverName();
    }

    private Component getFluidDisplayName(String fluidId, int amount) {
        if (fluidId == null || fluidId.isBlank()) {
            return Component.literal("unknown");
        }
        ResourceLocation key = ResourceLocation.tryParse(fluidId.trim());
        if (key == null) {
            return Component.literal(fluidId);
        }
        return BuiltInRegistries.FLUID.getOptional(key)
                .filter(fluid -> fluid != Fluids.EMPTY)
                .map(fluid -> new FluidStack(fluid, Math.max(1, amount)).getHoverName())
                .orElse(Component.literal(fluidId));
    }

    private int getLogicalStackAmount(ItemStack stack) {
        if (stack.isEmpty()) {
            return 1;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.getBoolean(TAG_FLUID_MARKER)) {
            return Math.max(1, tag.getInt(TAG_FLUID_AMOUNT));
        }
        if (tag.getBoolean(TAG_GAS_MARKER)) {
            return Math.max(1, tag.getInt(TAG_GAS_AMOUNT));
        }
        if (tag.getBoolean(TAG_ITEM_MARKER)) {
            return Math.max(1, tag.getInt(TAG_ITEM_AMOUNT));
        }
        return Math.max(1, stack.getCount());
    }

    private boolean isFluidMarkerStack(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getBoolean(TAG_FLUID_MARKER) && tag.contains(TAG_FLUID_NAME, Tag.TAG_STRING);
    }

    private boolean isGasMarkerStack(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getBoolean(TAG_GAS_MARKER) && tag.contains(TAG_GAS_NAME, Tag.TAG_STRING);
    }

    private static final class SlotEntry {
        private final int slot;
        private final ItemStack stack;

        private SlotEntry(int slot, ItemStack stack) {
            this.slot = slot;
            this.stack = stack;
        }
    }
}
