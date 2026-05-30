package com.ae2smartpatternsystem.integration.ae2;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import com.ae2smartpatternsystem.ItemTest;
import com.ae2smartpatternsystem.LegacyPatternNbtKeys;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class TechStartPatternDetails implements IPatternDetails {
    private static final String TAG_INPUTS = LegacyPatternNbtKeys.TAG_INPUTS;
    private static final String TAG_OUTPUTS = LegacyPatternNbtKeys.TAG_OUTPUTS;
    private static final String TAG_VIRTUAL_INPUT_STACKS = "VirtualInputStacks";
    private static final String TAG_VIRTUAL_OUTPUT_STACKS = "VirtualOutputStacks";
    private static final String TAG_VIRTUAL_INPUT_ORES = "VirtualInputOreNames";
    private static final String TAG_VIRTUAL_OUTPUT_ORES = "VirtualOutputOreNames";
    private static final String TAG_VIRTUAL_INPUT_ORE = "VirtualInputOreName";
    private static final String TAG_VIRTUAL_OUTPUT_ORE = "VirtualOutputOreName";
    private static final String TAG_EDITOR_INPUT_SLOTS = "EditorInputSlots";
    private static final String TAG_EDITOR_OUTPUT_SLOTS = "EditorOutputSlots";
    private static final String TAG_EDITOR_SLOT = "Slot";
    private static final String TAG_EDITOR_STACK = "Stack";
    private static final String TAG_ITEM_MARKER = LegacyPatternNbtKeys.TAG_ITEM_MARKER;
    private static final String TAG_ITEM_AMOUNT = LegacyPatternNbtKeys.TAG_ITEM_AMOUNT;

    private final AEItemKey definition;
    private final IInput[] inputs;
    private final GenericStack[] outputs;

    public TechStartPatternDetails(AEItemKey definition, ItemStack encodedStack) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.inputs = buildInputs(encodedStack);
        this.outputs = buildOutputs(encodedStack);
        if (this.inputs.length == 0 || this.outputs.length == 0) {
            throw new IllegalArgumentException("Encoded stack has no valid inputs or outputs.");
        }
    }

    @Override
    public AEItemKey getDefinition() {
        return definition;
    }

    @Override
    public IInput[] getInputs() {
        return inputs;
    }

    @Override
    public List<GenericStack> getOutputs() {
        List<GenericStack> result = new ArrayList<>(outputs.length);
        for (GenericStack output : outputs) {
            result.add(output);
        }
        return result;
    }

    private static IInput[] buildInputs(ItemStack encodedStack) {
        List<GenericStack> stacks = new ArrayList<>();
        stacks.addAll(resolveConcreteItemInputs(encodedStack));
        stacks.addAll(resolveFluidStacks(encodedStack, true));
        return stacks.stream()
            .filter(Objects::nonNull)
            .map(SimpleInput::new)
            .toArray(IInput[]::new);
    }

    private static GenericStack[] buildOutputs(ItemStack encodedStack) {
        Map<AEKey, Long> merged = new LinkedHashMap<>();
        for (GenericStack stack : resolveConcreteItemOutputs(encodedStack)) {
            if (stack == null || stack.what() == null || stack.amount() <= 0) {
                continue;
            }
            merged.merge(stack.what(), stack.amount(), Long::sum);
        }
        for (GenericStack stack : resolveFluidStacks(encodedStack, false)) {
            if (stack == null || stack.what() == null || stack.amount() <= 0) {
                continue;
            }
            merged.merge(stack.what(), stack.amount(), Long::sum);
        }
        GenericStack[] result = new GenericStack[merged.size()];
        int index = 0;
        for (Map.Entry<AEKey, Long> entry : merged.entrySet()) {
            result[index++] = new GenericStack(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private static List<GenericStack> resolveConcreteItemInputs(ItemStack encodedStack) {
        List<ItemStack> concrete = resolveConcreteStacks(
            encodedStack,
            ItemTest.getInputOreNamesStatic(encodedStack),
            ItemTest.getInputCountsStatic(encodedStack),
            TAG_VIRTUAL_INPUT_STACKS,
            true
        );
        return toGenericStacks(concrete);
    }

    private static List<GenericStack> resolveConcreteItemOutputs(ItemStack encodedStack) {
        List<ItemStack> concrete = resolveConcreteStacks(
            encodedStack,
            ItemTest.getOutputOreNamesStatic(encodedStack),
            ItemTest.getOutputCountsStatic(encodedStack),
            TAG_VIRTUAL_OUTPUT_STACKS,
            false
        );
        return toGenericStacks(concrete);
    }

    private static List<ItemStack> resolveConcreteStacks(
        ItemStack encodedStack,
        List<String> oreNames,
        List<Integer> counts,
        String virtualListKey,
        boolean inputSide
    ) {
        List<ItemStack> resolved = readPreviewStacks(encodedStack, inputSide ? TAG_INPUTS : TAG_OUTPUTS);
        if (!resolved.isEmpty()) {
            return resolved;
        }

        resolved = hasExplicitVirtualOres(encodedStack, inputSide)
            ? readVirtualStacks(encodedStack, virtualListKey)
            : new ArrayList<>();
        if (!resolved.isEmpty()) {
            return resolved;
        }

        resolved = readPreviewStacks(encodedStack, inputSide ? TAG_EDITOR_INPUT_SLOTS : TAG_EDITOR_OUTPUT_SLOTS);
        if (!resolved.isEmpty()) {
            return resolved;
        }

        List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < oreNames.size(); i++) {
            String oreName = oreNames.get(i);
            if (oreName == null || oreName.isEmpty()) {
                continue;
            }
            List<ItemStack> candidates = OreDictionary.getOres(oreName);
            ItemStack selected = selectFirstAllowed(encodedStack, candidates, inputSide);
            if (selected.isEmpty()) {
                continue;
            }
            ItemStack copy = selected.copy();
            int count = i < counts.size() ? counts.get(i) : copy.getCount();
            copy.setCount(Math.max(1, count));
            result.add(copy);
        }
        return result;
    }

    private static boolean hasExplicitVirtualOres(ItemStack encodedStack, boolean inputSide) {
        if (encodedStack == null || encodedStack.isEmpty() || !encodedStack.hasTagCompound()) {
            return false;
        }
        NBTTagCompound tag = encodedStack.getTagCompound();
        if (tag == null) {
            return false;
        }
        return inputSide
            ? tag.hasKey(TAG_VIRTUAL_INPUT_ORES, 9) || tag.hasKey(TAG_VIRTUAL_INPUT_ORE, 8)
            : tag.hasKey(TAG_VIRTUAL_OUTPUT_ORES, 9) || tag.hasKey(TAG_VIRTUAL_OUTPUT_ORE, 8);
    }

    private static List<ItemStack> readVirtualStacks(ItemStack encodedStack, String listKey) {
        List<ItemStack> result = new ArrayList<>();
        if (encodedStack == null || encodedStack.isEmpty() || !encodedStack.hasTagCompound()) {
            return result;
        }
        NBTTagCompound tag = encodedStack.getTagCompound();
        if (tag == null || !tag.hasKey(listKey, 9)) {
            return result;
        }
        NBTTagList list = tag.getTagList(listKey, 10);
        for (int i = 0; i < list.tagCount(); i++) {
            ItemStack stack = new ItemStack(list.getCompoundTagAt(i));
            if (!stack.isEmpty()) {
                result.add(stack);
            }
        }
        return result;
    }

    private static List<ItemStack> readPreviewStacks(ItemStack encodedStack, String listKey) {
        List<ItemStack> result = new ArrayList<>();
        if (encodedStack == null || encodedStack.isEmpty() || !encodedStack.hasTagCompound()) {
            return result;
        }
        NBTTagCompound tag = encodedStack.getTagCompound();
        if (tag == null || !tag.hasKey(listKey, 9)) {
            return result;
        }
        NBTTagList list = tag.getTagList(listKey, 10);
        List<PreviewSlot> ordered = new ArrayList<>();
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            if (!entry.hasKey(TAG_EDITOR_SLOT) || !entry.hasKey(TAG_EDITOR_STACK, 10)) {
                continue;
            }
            ItemStack raw = new ItemStack(entry.getCompoundTag(TAG_EDITOR_STACK));
            if (raw.isEmpty()) {
                continue;
            }
            ordered.add(new PreviewSlot(entry.getInteger(TAG_EDITOR_SLOT), stripItemMarkerTags(raw)));
        }
        ordered.sort((a, b) -> Integer.compare(a.slot, b.slot));
        for (PreviewSlot slot : ordered) {
            if (!slot.stack.isEmpty()) {
                result.add(slot.stack);
            }
        }
        return result;
    }

    private static ItemStack selectFirstAllowed(ItemStack encodedStack, List<ItemStack> candidates, boolean inputSide) {
        if (candidates == null) {
            return ItemStack.EMPTY;
        }
        for (ItemStack candidate : candidates) {
            if (candidate == null || candidate.isEmpty()) {
                continue;
            }
            boolean allowed = inputSide
                ? ItemTest.isInputStackAllowedForPattern(encodedStack, candidate)
                : ItemTest.isOutputStackAllowedForPattern(encodedStack, candidate);
            if (allowed) {
                return candidate;
            }
        }
        return ItemStack.EMPTY;
    }

    private static List<GenericStack> toGenericStacks(List<ItemStack> stacks) {
        List<GenericStack> result = new ArrayList<>();
        for (ItemStack stack : stacks) {
            GenericStack generic = GenericStack.fromItemStack(stripItemMarkerTags(stack));
            if (generic != null && generic.amount() > 0) {
                result.add(generic);
            }
        }
        return result;
    }

    private static ItemStack stripItemMarkerTags(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) {
            return stack;
        }
        ItemStack cleaned = stack.copy();
        NBTTagCompound tag = cleaned.getTagCompound();
        if (tag == null) {
            return cleaned;
        }
        if (tag.hasKey(TAG_ITEM_MARKER)) {
            tag.removeTag(TAG_ITEM_MARKER);
        }
        if (tag.hasKey(TAG_ITEM_AMOUNT)) {
            tag.removeTag(TAG_ITEM_AMOUNT);
        }
        if (tag.getKeySet().isEmpty()) {
            cleaned.setTagCompound(null);
        }
        return cleaned;
    }

    private static List<GenericStack> resolveFluidStacks(ItemStack encodedStack, boolean input) {
        List<String> fluids = input ? ItemTest.getInputFluidsStatic(encodedStack) : ItemTest.getOutputFluidsStatic(encodedStack);
        List<Integer> amounts = input ? ItemTest.getInputFluidAmountsStatic(encodedStack) : ItemTest.getOutputFluidAmountsStatic(encodedStack);
        List<GenericStack> result = new ArrayList<>();
        for (int i = 0; i < fluids.size(); i++) {
            String fluidName = fluids.get(i);
            int amount = i < amounts.size() ? amounts.get(i) : 0;
            if (fluidName == null || fluidName.isEmpty() || amount <= 0) {
                continue;
            }
            FluidStack fluidStack = FluidRegistry.getFluidStack(fluidName, amount);
            if (fluidStack == null) {
                continue;
            }
            GenericStack generic = GenericStack.fromFluidStack(fluidStack);
            if (generic != null && generic.amount() > 0) {
                result.add(generic);
            }
        }
        return result;
    }

    private static final class SimpleInput implements IInput {
        private final GenericStack[] possibleInputs;
        private final long multiplier;

        private SimpleInput(GenericStack stack) {
            this.possibleInputs = new GenericStack[] {
                new GenericStack(stack.what(), 1)
            };
            this.multiplier = stack.amount();
        }

        @Override
        public GenericStack[] possibleInputs() {
            return possibleInputs;
        }

        @Override
        public long getMultiplier() {
            return multiplier;
        }

        @Override
        public boolean isValid(AEKey key, World world) {
            if (key == null) {
                return false;
            }
            AEKey incoming = key.dropSecondary();
            for (GenericStack possibleInput : possibleInputs) {
                if (Objects.equals(possibleInput.what().dropSecondary(), incoming)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public AEKey getRemainingKey(AEKey template) {
            return null;
        }
    }

    private static final class PreviewSlot {
        private final int slot;
        private final ItemStack stack;

        private PreviewSlot(int slot, ItemStack stack) {
            this.slot = slot;
            this.stack = stack;
        }
    }
}
