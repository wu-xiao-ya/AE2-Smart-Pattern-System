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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class TechStartPatternDetails implements IPatternDetails {
    private static final String TAG_INPUTS = LegacyPatternNbtKeys.TAG_INPUTS;
    private static final String TAG_OUTPUTS = LegacyPatternNbtKeys.TAG_OUTPUTS;
    private static final String TAG_VIRTUAL_INPUT_STACKS = LegacyPatternNbtKeys.TAG_VIRTUAL_INPUT_STACKS;
    private static final String TAG_VIRTUAL_OUTPUT_STACKS = LegacyPatternNbtKeys.TAG_VIRTUAL_OUTPUT_STACKS;
    private static final String TAG_VIRTUAL_INPUT_ORES = LegacyPatternNbtKeys.TAG_VIRTUAL_INPUT_ORES;
    private static final String TAG_VIRTUAL_OUTPUT_ORES = LegacyPatternNbtKeys.TAG_VIRTUAL_OUTPUT_ORES;
    private static final String TAG_VIRTUAL_INPUT_ORE = LegacyPatternNbtKeys.TAG_VIRTUAL_INPUT_ORE_NAME;
    private static final String TAG_VIRTUAL_OUTPUT_ORE = LegacyPatternNbtKeys.TAG_VIRTUAL_OUTPUT_ORE_NAME;
    private static final String TAG_EDITOR_INPUT_SLOTS = LegacyPatternNbtKeys.TAG_EDITOR_INPUT_SLOTS;
    private static final String TAG_EDITOR_OUTPUT_SLOTS = LegacyPatternNbtKeys.TAG_EDITOR_OUTPUT_SLOTS;
    private static final String TAG_EDITOR_SLOT = LegacyPatternNbtKeys.TAG_EDITOR_SLOT;
    private static final String TAG_EDITOR_STACK = LegacyPatternNbtKeys.TAG_EDITOR_STACK;
    private static final String TAG_ITEM_MARKER = LegacyPatternNbtKeys.TAG_ITEM_MARKER;
    private static final String TAG_ITEM_AMOUNT = LegacyPatternNbtKeys.TAG_ITEM_AMOUNT;
    private static final String TAG_FLUID_AMOUNT = LegacyPatternNbtKeys.TAG_FLUID_AMOUNT;
    private static final String TAG_GAS_MARKER = LegacyPatternNbtKeys.TAG_GAS_MARKER;
    private static final String TAG_GAS_NAME = LegacyPatternNbtKeys.TAG_GAS_NAME;
    private static final String TAG_GAS_AMOUNT = LegacyPatternNbtKeys.TAG_GAS_AMOUNT;
    private static final String TAG_DISPLAY_ONLY = "DisplayOnly";
    private static final String TAG_GAS_STACK = "GasStack";
    private static final String TAG_GAS_STACK_AMOUNT = "amount";

    private final AEItemKey definition;
    private final IInput[] inputs;
    private final GenericStack[] outputs;

    private static boolean gasReflectionReady;
    private static Method cachedGasRegistryGetGas;
    private static Constructor<?> cachedGasStackCtor;
    private static Method cachedPackGas2Packet;
    private static Method cachedPackGas2Drops;

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
        stacks.addAll(resolveGasStacks(encodedStack, true));
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
        for (GenericStack stack : resolveGasStacks(encodedStack, false)) {
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
            if (raw.isEmpty() || isNonItemMarkerStack(raw)) {
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

    private static ItemStack stripGasMarkerTags(ItemStack stack) {
        return stripGasMarkerTags(stack, stack == null ? 0 : stack.getCount());
    }

    private static ItemStack stripGasMarkerTags(ItemStack stack, int amount) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) {
            return stack;
        }
        ItemStack cleaned = stack.copy();
        cleaned.setCount(Math.max(1, amount));
        NBTTagCompound tag = cleaned.getTagCompound();
        if (tag == null) {
            return cleaned;
        }
        tag.removeTag(TAG_GAS_MARKER);
        tag.removeTag(TAG_GAS_NAME);
        tag.removeTag(TAG_GAS_AMOUNT);
        tag.removeTag(TAG_DISPLAY_ONLY);
        if (tag.hasKey(TAG_GAS_STACK, 10)) {
            NBTTagCompound gasStackTag = tag.getCompoundTag(TAG_GAS_STACK);
            gasStackTag.setInteger(TAG_GAS_STACK_AMOUNT, Math.max(1, amount));
            tag.setTag(TAG_GAS_STACK, gasStackTag);
        }
        if (tag.getKeySet().isEmpty()) {
            cleaned.setTagCompound(null);
        }
        return cleaned;
    }

    private static boolean isNonItemMarkerStack(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) {
            return false;
        }
        NBTTagCompound tag = stack.getTagCompound();
        return tag.hasKey(TAG_FLUID_AMOUNT) || tag.getBoolean(TAG_GAS_MARKER) || tag.hasKey(TAG_GAS_NAME);
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

    private static List<GenericStack> resolveGasStacks(ItemStack encodedStack, boolean input) {
        List<String> gases = input ? ItemTest.getInputGasesStatic(encodedStack) : ItemTest.getOutputGasesStatic(encodedStack);
        List<Integer> amounts = input ? ItemTest.getInputGasAmountsStatic(encodedStack) : ItemTest.getOutputGasAmountsStatic(encodedStack);
        List<ItemStack> gasItems = input ? ItemTest.getInputGasItemsStatic(encodedStack) : ItemTest.getOutputGasItemsStatic(encodedStack);
        List<GenericStack> result = new ArrayList<>();
        for (int i = 0; i < gases.size(); i++) {
            String gasName = gases.get(i);
            int amount = i < amounts.size() ? amounts.get(i) : 0;
            if (gasName == null || gasName.isEmpty() || amount <= 0) {
                continue;
            }
            ItemStack stack = resolveGasItemStack(gasName, amount, gasItems, i);
            GenericStack generic = GenericStack.fromItemStack(stripGasMarkerTags(stack, amount));
            if (generic != null && generic.amount() > 0) {
                result.add(new GenericStack(generic.what(), amount));
            }
        }
        return result;
    }

    private static ItemStack resolveGasItemStack(String gasName, int amount, List<ItemStack> gasItems, int index) {
        ItemStack packed = createPackedGasStack(gasName, amount, false);
        if (!packed.isEmpty()) {
            return packed;
        }
        if (gasItems != null && index < gasItems.size()) {
            ItemStack saved = gasItems.get(index);
            if (saved != null && !saved.isEmpty()) {
                return stripGasMarkerTags(saved, amount);
            }
        }
        return createPackedGasStack(gasName, amount, true);
    }

    private static ItemStack createPackedGasStack(String gasName, int amount, boolean drops) {
        initGasReflection();
        Method packer = drops ? cachedPackGas2Drops : cachedPackGas2Packet;
        if (cachedGasRegistryGetGas == null || cachedGasStackCtor == null || packer == null) {
            return ItemStack.EMPTY;
        }
        try {
            Object gas = cachedGasRegistryGetGas.invoke(null, gasName);
            if (gas == null) {
                return ItemStack.EMPTY;
            }
            Object gasStack = cachedGasStackCtor.newInstance(gas, Math.max(1, amount));
            Object packed = packer.invoke(null, gasStack);
            if (packed instanceof ItemStack) {
                ItemStack stack = ((ItemStack) packed).copy();
                if (!stack.isEmpty()) {
                    stack.setCount(Math.max(1, amount));
                }
                return stack;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }

    private static void initGasReflection() {
        if (gasReflectionReady) {
            return;
        }
        gasReflectionReady = true;
        try {
            Class<?> gasRegistryClass = Class.forName("mekanism.api.gas.GasRegistry");
            Class<?> gasClass = Class.forName("mekanism.api.gas.Gas");
            Class<?> gasStackClass = Class.forName("mekanism.api.gas.GasStack");
            Class<?> fakeGasesClass = Class.forName("com.glodblock.github.integration.mek.FakeGases");
            cachedGasRegistryGetGas = gasRegistryClass.getMethod("getGas", String.class);
            cachedGasStackCtor = gasStackClass.getConstructor(gasClass, int.class);
            cachedPackGas2Packet = fakeGasesClass.getMethod("packGas2Packet", gasStackClass);
            cachedPackGas2Drops = fakeGasesClass.getMethod("packGas2Drops", gasStackClass);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            cachedGasRegistryGetGas = null;
            cachedGasStackCtor = null;
            cachedPackGas2Packet = null;
            cachedPackGas2Drops = null;
        }
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
