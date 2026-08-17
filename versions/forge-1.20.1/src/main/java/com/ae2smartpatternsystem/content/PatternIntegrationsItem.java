package com.ae2smartpatternsystem.content;

import com.ae2smartpatternsystem.core.PatternDefinitionBridge;
import com.ae2smartpatternsystem.core.codec.PatternNbtKeys;
import com.ae2smartpatternsystem.core.model.FilterMode;
import com.ae2smartpatternsystem.core.model.ModFilterRule;
import com.ae2smartpatternsystem.core.model.PatternDefinition;
import com.ae2smartpatternsystem.integration.mekanism.MekanismGasHelper;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.fluids.FluidStack;
import com.ae2smartpatternsystem.menu.PatternEditorMenu;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PatternIntegrationsItem extends Item {
    private static final String TAG_ENCODED = "TechStartEncoded";
    private static final String TAG_INPUTS = "TechStartInputs";
    private static final String TAG_OUTPUTS = "TechStartOutputs";
    private static final String TAG_ENCODED_ITEM = "EncodedItem";
    private static final String TAG_FILTER_MODE = PatternNbtKeys.TAG_FILTER_MODE;
    private static final String TAG_FILTER_MODE_LEGACY = PatternNbtKeys.TAG_FILTER_MODE_LEGACY;
    private static final String TAG_INPUT_FLUIDS = "InputFluids";
    private static final String TAG_INPUT_FLUID_AMOUNTS = "InputFluidAmounts";
    private static final String TAG_OUTPUT_FLUIDS = "OutputFluids";
    private static final String TAG_OUTPUT_FLUID_AMOUNTS = "OutputFluidAmounts";
    private static final String TAG_FLUID_MARKER = "TechStartFluidMarker";
    private static final String TAG_INPUT_GASES = "InputGases";
    private static final String TAG_INPUT_GAS_AMOUNTS = "InputGasAmounts";
    private static final String TAG_OUTPUT_GASES = "OutputGases";
    private static final String TAG_OUTPUT_GAS_AMOUNTS = "OutputGasAmounts";
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

    public static PatternDefinition readDefinition(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return PatternDefinitionBridge.read(tag == null ? new CompoundTag() : tag);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (!level.isClientSide && player.isShiftKeyDown()) {
            clearPatternData(stack);
            player.sendSystemMessage(Component.translatable("message.ae2sps.pattern_cleared"));
            return InteractionResultHolder.success(stack);
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(
                    serverPlayer,
                    new SimpleMenuProvider(
                            (containerId, playerInventory, targetPlayer) -> new PatternEditorMenu(containerId, playerInventory, usedHand),
                            Component.translatable("gui.ae2sps.pattern_editor")),
                    buf -> buf.writeEnum(usedHand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag isAdvanced) {
        CompoundTag tag = stack.getTag();
        boolean encoded = tag != null && tag.getBoolean(TAG_ENCODED);

        String encodedLabel = Component.translatable(encoded ? "tooltip.ae2sps.bool_yes" : "tooltip.ae2sps.bool_no").getString();
        if (tag != null && tag.contains(TAG_ENCODED_ITEM, Tag.TAG_STRING)) {
            String encodedName = tag.getString(TAG_ENCODED_ITEM).trim();
            if (!encodedName.isEmpty()) {
                encodedLabel = encodedName;
            }
        }

        tooltip.add(Component.translatable("tooltip.ae2sps.encoded", encodedLabel)
                .withStyle(encoded ? ChatFormatting.GREEN : ChatFormatting.RED));

        int filterMode = readFilterMode(tag);
        FilterMode inputModFilterMode = readModFilterMode(tag, true);
        FilterMode outputModFilterMode = readModFilterMode(tag, false);
        Component modeText = Component.translatable(filterMode == 0
                ? "tooltip.ae2sps.filter_mode_whitelist"
                : "tooltip.ae2sps.filter_mode_blacklist");
        tooltip.add(Component.translatable("tooltip.ae2sps.filter_mode", modeText)
                .withStyle(filterMode == 0 ? ChatFormatting.AQUA : ChatFormatting.GOLD));
        tooltip.add(Component.translatable(
                "tooltip.ae2sps.input_mod_filter",
                Component.translatable(inputModFilterMode == FilterMode.WHITELIST
                        ? "tooltip.ae2sps.filter_mode_whitelist"
                        : "tooltip.ae2sps.filter_mode_blacklist"),
                readModFilterIds(tag, true).size()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "tooltip.ae2sps.output_mod_filter",
                Component.translatable(outputModFilterMode == FilterMode.WHITELIST
                        ? "tooltip.ae2sps.filter_mode_whitelist"
                        : "tooltip.ae2sps.filter_mode_blacklist"),
                readModFilterIds(tag, false).size()).withStyle(ChatFormatting.GRAY));

        if (tag != null && encoded) {
            List<Component> inputLines = buildEntryLines(tag, TAG_INPUTS, true);
            List<Component> outputLines = buildEntryLines(tag, TAG_OUTPUTS, false);
            List<Component> inputFluidLines = buildFluidLines(tag, TAG_INPUT_FLUIDS, TAG_INPUT_FLUID_AMOUNTS, true);
            List<Component> outputFluidLines = buildFluidLines(tag, TAG_OUTPUT_FLUIDS, TAG_OUTPUT_FLUID_AMOUNTS, false);
            List<Component> inputGasLines = buildGasLines(tag, TAG_INPUT_GASES, TAG_INPUT_GAS_AMOUNTS, true);
            List<Component> outputGasLines = buildGasLines(tag, TAG_OUTPUT_GASES, TAG_OUTPUT_GAS_AMOUNTS, false);

            tooltip.add(Component.translatable("tooltip.ae2sps.input_count", inputLines.size() + inputFluidLines.size() + inputGasLines.size()).withStyle(ChatFormatting.GRAY));
            tooltip.addAll(inputLines);
            tooltip.addAll(inputFluidLines);
            tooltip.addAll(inputGasLines);
            tooltip.add(Component.translatable("tooltip.ae2sps.output_count", outputLines.size() + outputFluidLines.size() + outputGasLines.size()).withStyle(ChatFormatting.GRAY));
            tooltip.addAll(outputLines);
            tooltip.addAll(outputFluidLines);
            tooltip.addAll(outputGasLines);
        }

        tooltip.add(Component.translatable("tooltip.ae2sps.open_pattern_editor").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.ae2sps.mark_hint").withStyle(ChatFormatting.GRAY));
    }

    private void clearPatternData(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return;
        }
        tag.remove(TAG_ENCODED);
        tag.remove(TAG_INPUTS);
        tag.remove(TAG_OUTPUTS);
        tag.remove(TAG_ENCODED_ITEM);
        tag.remove(TAG_FILTER_MODE);
        tag.remove(TAG_FILTER_MODE_LEGACY);
        tag.remove(TAG_INPUT_FLUIDS);
        tag.remove(TAG_INPUT_FLUID_AMOUNTS);
        tag.remove(TAG_OUTPUT_FLUIDS);
        tag.remove(TAG_OUTPUT_FLUID_AMOUNTS);
        tag.remove(TAG_INPUT_GASES);
        tag.remove(TAG_INPUT_GAS_AMOUNTS);
        tag.remove(TAG_OUTPUT_GASES);
        tag.remove(TAG_OUTPUT_GAS_AMOUNTS);
        tag.remove(PatternNbtKeys.TAG_INPUT_MOD_FILTER_MODE);
        tag.remove(PatternNbtKeys.TAG_OUTPUT_MOD_FILTER_MODE);
        tag.remove(PatternNbtKeys.TAG_INPUT_MOD_FILTER_IDS);
        tag.remove(PatternNbtKeys.TAG_OUTPUT_MOD_FILTER_IDS);
        tag.remove(PatternNbtKeys.TAG_EXCLUDED_INPUT_MOD_IDS);
        tag.remove(PatternNbtKeys.TAG_EXCLUDED_OUTPUT_MOD_IDS);
        if (tag.isEmpty()) {
            stack.setTag(null);
        }
    }

    private int readFilterMode(@Nullable CompoundTag tag) {
        if (tag != null && tag.contains(TAG_FILTER_MODE, Tag.TAG_INT)) {
            return tag.getInt(TAG_FILTER_MODE) == FILTER_MODE_WHITELIST ? FILTER_MODE_WHITELIST : FILTER_MODE_BLACKLIST;
        }
        if (tag != null && tag.contains(TAG_FILTER_MODE_LEGACY, Tag.TAG_INT)) {
            return tag.getInt(TAG_FILTER_MODE_LEGACY) == FILTER_MODE_WHITELIST ? FILTER_MODE_WHITELIST : FILTER_MODE_BLACKLIST;
        }
        return FILTER_MODE_BLACKLIST;
    }

    private FilterMode readModFilterMode(@Nullable CompoundTag tag, boolean input) {
        if (tag == null) {
            return FilterMode.BLACKLIST;
        }
        String modeKey = input
                ? PatternNbtKeys.TAG_INPUT_MOD_FILTER_MODE
                : PatternNbtKeys.TAG_OUTPUT_MOD_FILTER_MODE;
        String idsKey = input
                ? PatternNbtKeys.TAG_INPUT_MOD_FILTER_IDS
                : PatternNbtKeys.TAG_OUTPUT_MOD_FILTER_IDS;
        String legacyKey = input
                ? PatternNbtKeys.TAG_EXCLUDED_INPUT_MOD_IDS
                : PatternNbtKeys.TAG_EXCLUDED_OUTPUT_MOD_IDS;
        if (tag.contains(modeKey, Tag.TAG_INT)) {
            return FilterMode.fromSerializedValue(tag.getInt(modeKey));
        }
        if (tag.contains(idsKey, Tag.TAG_LIST) || tag.contains(legacyKey, Tag.TAG_LIST)) {
            return FilterMode.BLACKLIST;
        }
        return FilterMode.BLACKLIST;
    }

    private List<String> readModFilterIds(@Nullable CompoundTag tag, boolean input) {
        if (tag == null) {
            return List.of();
        }
        String idsKey = input
                ? PatternNbtKeys.TAG_INPUT_MOD_FILTER_IDS
                : PatternNbtKeys.TAG_OUTPUT_MOD_FILTER_IDS;
        String legacyKey = input
                ? PatternNbtKeys.TAG_EXCLUDED_INPUT_MOD_IDS
                : PatternNbtKeys.TAG_EXCLUDED_OUTPUT_MOD_IDS;
        String sourceKey = tag.contains(idsKey, Tag.TAG_LIST) ? idsKey : legacyKey;
        if (!tag.contains(sourceKey, Tag.TAG_LIST)) {
            return List.of();
        }
        ListTag raw = tag.getList(sourceKey, Tag.TAG_STRING);
        List<String> ids = new ArrayList<>(Math.min(raw.size(), 512));
        for (int i = 0; i < raw.size() && ids.size() < 512; i++) {
            String id = ModFilterRule.normalizeModId(raw.getString(i));
            if (!id.isBlank() && id.length() <= 64 && !ids.contains(id)) {
                ids.add(id);
            }
        }
        return ids;
    }

    private List<Component> buildEntryLines(CompoundTag tag, String listKey, boolean input) {
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
            ItemStack entryStack = ItemStack.of(entry.getCompound(TAG_STACK));
            if (entryStack.isEmpty() || isFluidMarkerStack(entryStack) || isGasMarkerStack(entryStack)) {
                continue;
            }
            entries.add(new SlotEntry(entry.getInt(TAG_SLOT), entryStack));
        }

        entries.sort((left, right) -> Integer.compare(left.slot, right.slot));
        if (entries.isEmpty()) {
            return List.of();
        }

        String lineKey = input ? "tooltip.ae2sps.input_item" : "tooltip.ae2sps.output_item";
        List<Component> lines = new ArrayList<>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            SlotEntry current = entries.get(i);
            lines.add(Component.translatable(lineKey, i + 1, current.stack.getHoverName(), getLogicalStackAmount(current.stack))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
    }

    private List<Component> buildFluidLines(CompoundTag tag, String nameListKey, String amountListKey, boolean input) {
        if (!tag.contains(nameListKey, Tag.TAG_LIST)) {
            return List.of();
        }
        ListTag names = tag.getList(nameListKey, Tag.TAG_STRING);
        if (names.isEmpty()) {
            return List.of();
        }
        ListTag amounts = tag.contains(amountListKey, Tag.TAG_LIST)
                ? tag.getList(amountListKey, Tag.TAG_INT)
                : new ListTag();
        String lineKey = input ? "tooltip.ae2sps.input_fluid" : "tooltip.ae2sps.output_fluid";
        List<Component> lines = new ArrayList<>(names.size());
        for (int i = 0; i < names.size(); i++) {
            String fluidName = names.getString(i);
            if (fluidName == null || fluidName.isBlank()) {
                continue;
            }
            int amount = i < amounts.size() ? amounts.getInt(i) : 1;
            lines.add(Component.translatable(lineKey, i + 1, getFluidDisplayName(fluidName, amount), Math.max(1, amount))
                    .withStyle(ChatFormatting.DARK_AQUA));
        }
        return lines;
    }

    private List<Component> buildGasLines(CompoundTag tag, String nameListKey, String amountListKey, boolean input) {
        if (!tag.contains(nameListKey, Tag.TAG_LIST)) {
            return List.of();
        }
        ListTag names = tag.getList(nameListKey, Tag.TAG_STRING);
        if (names.isEmpty()) {
            return List.of();
        }
        ListTag amounts = tag.contains(amountListKey, Tag.TAG_LIST)
                ? tag.getList(amountListKey, Tag.TAG_INT)
                : new ListTag();
        String lineKey = input ? "tooltip.ae2sps.input_gas" : "tooltip.ae2sps.output_gas";
        List<Component> lines = new ArrayList<>(names.size());
        for (int i = 0; i < names.size(); i++) {
            String gasName = names.getString(i);
            if (gasName == null || gasName.isBlank()) {
                continue;
            }
            int amount = i < amounts.size() ? amounts.getInt(i) : 1;
            lines.add(Component.translatable(lineKey, i + 1, MekanismGasHelper.getDisplayName(gasName), Math.max(1, amount))
                    .withStyle(ChatFormatting.DARK_AQUA));
        }
        return lines;
    }

    private static final class SlotEntry {
        private final int slot;
        private final ItemStack stack;

        private SlotEntry(int slot, ItemStack stack) {
            this.slot = slot;
            this.stack = stack;
        }
    }

    private boolean isFluidMarkerStack(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return false;
        }
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(TAG_FLUID_MARKER);
    }

    private boolean isGasMarkerStack(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return false;
        }
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(TAG_GAS_MARKER);
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
                .map(fluid -> new FluidStack(fluid, Math.max(1, amount)).getDisplayName())
                .orElse(Component.literal(fluidId));
    }

    private int getLogicalStackAmount(ItemStack stack) {
        if (stack.isEmpty()) {
            return 1;
        }
        if (!stack.hasTag()) {
            return Math.max(1, stack.getCount());
        }
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.getBoolean(TAG_ITEM_MARKER)) {
            return Math.max(1, tag.getInt(TAG_ITEM_AMOUNT));
        }
        if (tag != null && tag.getBoolean(TAG_GAS_MARKER)) {
            return Math.max(1, tag.getInt(TAG_GAS_AMOUNT));
        }
        return Math.max(1, stack.getCount());
    }
}


