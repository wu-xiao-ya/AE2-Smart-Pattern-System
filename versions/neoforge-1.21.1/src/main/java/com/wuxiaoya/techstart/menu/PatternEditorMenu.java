package com.wuxiaoya.techstart.menu;

import com.wuxiaoya.techstart.config.TechStartConfig;
import com.wuxiaoya.techstart.TechStartNeoForge;
import com.wuxiaoya.techstart.integration.ae2.TechStartPatternExpansion;
import com.wuxiaoya.techstart.integration.mekanism.MekanismGasHelper;
import com.wuxiaoya.techstart.registry.TechStartItems;
import com.wuxiaoya.techstart.registry.TechStartMenus;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternEditorMenu extends AbstractContainerMenu {
    private static final Pattern RESOURCE_LOCATION_PATTERN =
            Pattern.compile("([a-z0-9_.-]+:[a-z0-9_./-]+)");
    private static final String TAG_ENCODED = "TechStartEncoded";
    private static final String TAG_INPUTS = "TechStartInputs";
    private static final String TAG_OUTPUTS = "TechStartOutputs";
    private static final String TAG_ENCODED_ITEM = "EncodedItem";
    private static final String TAG_SLOT = "Slot";
    private static final String TAG_STACK = "Stack";
    private static final String TAG_FLUID_MARKER = "TechStartFluidMarker";
    private static final String TAG_FLUID_NAME = "TechStartFluidName";
    private static final String TAG_FLUID_AMOUNT = "TechStartFluidAmount";
    private static final String TAG_INPUT_FLUIDS = "InputFluids";
    private static final String TAG_INPUT_FLUID_AMOUNTS = "InputFluidAmounts";
    private static final String TAG_OUTPUT_FLUIDS = "OutputFluids";
    private static final String TAG_OUTPUT_FLUID_AMOUNTS = "OutputFluidAmounts";
    private static final String TAG_GAS_MARKER = "TechStartGasMarker";
    private static final String TAG_GAS_NAME = "TechStartGasName";
    private static final String TAG_GAS_AMOUNT = "TechStartGasAmount";
    private static final String TAG_INPUT_GASES = "InputGases";
    private static final String TAG_INPUT_GAS_AMOUNTS = "InputGasAmounts";
    private static final String TAG_OUTPUT_GASES = "OutputGases";
    private static final String TAG_OUTPUT_GAS_AMOUNTS = "OutputGasAmounts";
    private static final String TAG_EXCLUDED_INPUT_MOD_IDS = "ExcludedInputModIds";
    private static final String TAG_EXCLUDED_OUTPUT_MOD_IDS = "ExcludedOutputModIds";
    private static final String TAG_ITEM_MARKER = "TechStartItemMarker";
    private static final String TAG_ITEM_AMOUNT = "TechStartItemAmount";
    private static final String TAG_FILTER_MODE = "TechStartFilterMode";
    private static final String TAG_FILTER_MODE_LEGACY = "FilterMode";
    private static final String TAG_FILTER_ENTRIES = "FilterEntries";
    private static final String TAG_INPUT_ORES = "InputOreNames";
    private static final String TAG_OUTPUT_ORES = "OutputOreNames";
    private static final String TAG_INPUT_COUNTS = "InputCounts";
    private static final String TAG_OUTPUT_COUNTS = "OutputCounts";
    private static final String TAG_VIRTUAL_FILTER_ENTRY_ID = "VirtualFilterEntryId";
    private static final int MAX_ENCODED_SLOT_COUNT = Integer.MAX_VALUE;
    private static final int INPUT_SLOTS = 9;
    private static final int OUTPUT_SLOTS = 9;
    private static final int TOTAL_PATTERN_SLOTS = INPUT_SLOTS + OUTPUT_SLOTS;
    private static final int BUTTON_FILTER_ENTRY_BASE = 1000;

    public static final int FILTER_MODE_WHITELIST = 0;
    public static final int FILTER_MODE_BLACKLIST = 1;

    private final ItemStackHandler itemHandler;
    private final ContainerLevelAccess access;
    private final Player player;
    private final @Nullable InteractionHand boundPatternHand;
    private final ItemStack boundPatternStack;
    private final ContainerData data = new SimpleContainerData(1);
    private final LinkedHashSet<String> filterEntries = new LinkedHashSet<>();
    private final LinkedHashSet<String> excludedInputModIds = new LinkedHashSet<>();
    private final LinkedHashSet<String> excludedOutputModIds = new LinkedHashSet<>();

    public static class PatternSlot extends SlotItemHandler {
        private boolean active = true;

        public PatternSlot(ItemStackHandler itemHandler, int index, int x, int y) {
            super(itemHandler, index, x, y);
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        @Override
        public boolean isActive() {
            return this.active;
        }
    }

    public static PatternEditorMenu createItemMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf data) {
        return new PatternEditorMenu(containerId, playerInventory, data.readEnum(InteractionHand.class));
    }

    public PatternEditorMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
        super(TechStartMenus.PATTERN_EDITOR_ITEM.get(), containerId);
        this.itemHandler = new ItemStackHandler(TOTAL_PATTERN_SLOTS);
        this.player = playerInventory.player;
        this.access = ContainerLevelAccess.NULL;
        this.boundPatternHand = hand;
        this.boundPatternStack = locateBoundPatternStack(hand);
        this.data.set(0, FILTER_MODE_BLACKLIST);
        this.addDataSlots(this.data);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slot = row * 3 + col;
                this.addSlot(createPatternSlot(this.itemHandler, slot, 24 + col * 18, 35 + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slot = INPUT_SLOTS + row * 3 + col;
                this.addSlot(createPatternSlot(this.itemHandler, slot, 96 + col * 18, 35 + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = col + row * 9 + 9;
                this.addSlot(new Slot(playerInventory, slot, 6 + col * 18, 125 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 6 + col * 18, 185));
        }

        loadFromPatternItem();
    }

    private Slot createPatternSlot(ItemStackHandler itemHandler, int slot, int x, int y) {
        return new PatternSlot(itemHandler, slot, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !stack.is(TechStartItems.PATTERN_INTEGRATIONS.get()) && super.mayPlace(stack);
            }
        };
    }

    @Override
    public void clicked(int slotId, int dragType, @NotNull ClickType clickType, @NotNull Player player) {
        if (isPatternSlot(slotId)) {
            if (clickType != ClickType.PICKUP || (dragType != 0 && dragType != 1)) {
                return;
            }

            Slot slot = this.slots.get(slotId);
            ItemStack current = slot.getItem();
            if (!current.isEmpty()) {
                slot.set(ItemStack.EMPTY);
                onPatternSlotsMutated(player);
                return;
            }

            ItemStack carried = this.getCarried();
            if (!carried.isEmpty() && !carried.is(TechStartItems.PATTERN_INTEGRATIONS.get())) {
                ItemStack placed = normalizePatternSlotStack(carried);
                if (!placed.isEmpty()) {
                    slot.set(placed);
                    onPatternSlotsMutated(player);
                }
            }
            return;
        }
        super.clicked(slotId, dragType, clickType, player);
    }

    @Override
    public boolean canDragTo(@NotNull Slot slot) {
        int slotId = this.slots.indexOf(slot);
        if (isPatternSlot(slotId)) {
            return false;
        }
        return super.canDragTo(slot);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        if (id == FILTER_MODE_WHITELIST || id == FILTER_MODE_BLACKLIST) {
            setFilterMode(id);
            saveToPatternItem();
            return true;
        }
        if (id >= BUTTON_FILTER_ENTRY_BASE) {
            int entryIndex = id - BUTTON_FILTER_ENTRY_BASE;
            toggleFilterEntryByIndex(entryIndex);
            saveToPatternItem();
            return true;
        }
        return super.clickMenuButton(player, id);
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return !resolvePatternStack().isEmpty();
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            saveToPatternItem();
        }
    }

    public int getFilterMode() {
        return normalizeFilterMode(this.data.get(0));
    }

    public static int encodeFilterEntryButtonId(int entryIndex) {
        return BUTTON_FILTER_ENTRY_BASE + Math.max(0, entryIndex);
    }

    public List<String> getFilterEntriesSnapshot() {
        return List.copyOf(this.filterEntries);
    }

    public List<String> getExcludedInputModIdsSnapshot() {
        return List.copyOf(this.excludedInputModIds);
    }

    public List<String> getExcludedOutputModIdsSnapshot() {
        return List.copyOf(this.excludedOutputModIds);
    }

    public ItemStack getPatternStackSnapshot() {
        return resolvePatternStack().copy();
    }

    public void refreshPatternStackSnapshot() {
        saveToPatternItem();
    }

    public int getAmountLimitForStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return 1;
        }
        return extractFluidMarker(stack) != null || extractGasMarker(stack) != null
                ? TechStartConfig.getFluidGasMarkerMaxAmount()
                : TechStartConfig.getItemMarkerMaxAmount();
    }

    public void applyPatternSlotAmountFromClient(int slotId, int amount, Player player) {
        if (!isPatternSlot(slotId) || player.level().isClientSide) {
            return;
        }
        applyPatternSlotCount(slotId, amount);
        onPatternSlotsMutated(player);
    }

    public void applyExcludedModFilters(String[] inputValues, String[] outputValues) {
        this.excludedInputModIds.clear();
        this.excludedInputModIds.addAll(normalizeModIds(inputValues));
        this.excludedOutputModIds.clear();
        this.excludedOutputModIds.addAll(normalizeModIds(outputValues));
        saveToPatternItem();
    }

    public void applyExcludedModFiltersFromClient(String[] inputValues, String[] outputValues, Player actor) {
        applyExcludedModFilters(inputValues, outputValues);
        if (!actor.level().isClientSide) {
            this.broadcastChanges();
            if (actor instanceof ServerPlayer serverPlayer) {
                serverPlayer.inventoryMenu.broadcastChanges();
            }
        }
    }

    public ItemStack createMarkerFromIngredient(Object ingredient) {
        return createMarkerFromIngredient(ingredient, -1);
    }

    public ItemStack createMarkerFromIngredient(Object ingredient, long preferredAmount) {
        if (ingredient instanceof ItemStack itemStack) {
            if (itemStack.isEmpty() || itemStack.is(TechStartItems.PATTERN_INTEGRATIONS.get())) {
                return ItemStack.EMPTY;
            }
            return normalizePatternSlotStack(itemStack);
        }
        if (ingredient instanceof Fluid fluid && fluid != Fluids.EMPTY) {
            int amount = preferredAmount > 0 ? sanitizeMarkerAmount((int) Math.min(Integer.MAX_VALUE, preferredAmount)) : 1000;
            ItemStack source = fluid.getBucket() != Items.AIR ? new ItemStack(fluid.getBucket()) : new ItemStack(Items.BUCKET);
            String fluidId = BuiltInRegistries.FLUID.getKey(fluid).toString();
            return createFluidMarkerStack(source, new FluidMarker(fluidId, amount));
        }
        if (ingredient instanceof FluidStack fluidStack && !fluidStack.isEmpty() && fluidStack.getFluid() != null) {
            ItemStack source = fluidStack.getFluid().getBucket() != Items.AIR
                    ? new ItemStack(fluidStack.getFluid().getBucket())
                    : new ItemStack(Items.BUCKET);
            String fluidId = BuiltInRegistries.FLUID.getKey(fluidStack.getFluid()).toString();
            int amount = preferredAmount > 0
                    ? sanitizeMarkerAmount((int) Math.min(Integer.MAX_VALUE, preferredAmount))
                    : Math.max(1, fluidStack.getAmount());
            return createFluidMarkerStack(source, new FluidMarker(fluidId, amount));
        }
        MekanismGasHelper.GasStackView gasStack = preferredAmount > 0
                ? MekanismGasHelper.extractGas(ingredient, preferredAmount)
                : MekanismGasHelper.extractGas(ingredient);
        if (gasStack != null && gasStack.gasId() != null && !gasStack.gasId().isBlank()) {
            return createGasMarkerStack(new ItemStack(Items.GLASS_BOTTLE), new GasMarker(gasStack.gasId(), Math.max(1, gasStack.amount())));
        }
        return ItemStack.EMPTY;
    }

    public void applyMarkerStackFromClient(int slotId, ItemStack marker, Player actor) {
        if (!isPatternSlot(slotId) || marker.isEmpty() || marker.is(TechStartItems.PATTERN_INTEGRATIONS.get())) {
            return;
        }
        Slot slot = this.slots.get(slotId);
        ItemStack normalized = normalizePatternSlotStack(marker);
        if (normalized.isEmpty()) {
            return;
        }
        slot.set(normalized);
        onPatternSlotsMutated(actor);
    }

    private boolean isPatternSlot(int slotId) {
        return slotId >= 0 && slotId < TOTAL_PATTERN_SLOTS;
    }

    private ItemStack locateBoundPatternStack() {
        return locateBoundPatternStack(this.boundPatternHand);
    }

    private ItemStack locateBoundPatternStack(@Nullable InteractionHand preferredHand) {
        if (preferredHand != null) {
            ItemStack preferred = this.player.getItemInHand(preferredHand);
            if (preferred.is(TechStartItems.PATTERN_INTEGRATIONS.get())) {
                return preferred;
            }
        }
        ItemStack mainHand = this.player.getMainHandItem();
        if (mainHand.is(TechStartItems.PATTERN_INTEGRATIONS.get())) {
            return mainHand;
        }
        ItemStack offHand = this.player.getOffhandItem();
        if (offHand.is(TechStartItems.PATTERN_INTEGRATIONS.get())) {
            return offHand;
        }
        return ItemStack.EMPTY;
    }

    private ItemStack resolvePatternStack() {
        if (this.boundPatternHand != null) {
            ItemStack preferred = this.player.getItemInHand(this.boundPatternHand);
            if (preferred.is(TechStartItems.PATTERN_INTEGRATIONS.get())) {
                return preferred;
            }
        }
        if (!this.boundPatternStack.isEmpty() && this.boundPatternStack.is(TechStartItems.PATTERN_INTEGRATIONS.get())) {
            return this.boundPatternStack;
        }
        return locateBoundPatternStack();
    }

    private void clearPatternSlots() {
        ItemStackHandler handler = this.itemHandler;
        for (int index = 0; index < TOTAL_PATTERN_SLOTS; index++) {
            handler.setStackInSlot(index, ItemStack.EMPTY);
        }
    }

    private void loadFromPatternItem() {
        clearPatternSlots();
        loadFilterStateFromPatternItem();
        ItemStack patternStack = resolvePatternStack();
        if (patternStack.isEmpty()) {
            return;
        }

        CompoundTag tag = patternStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.isEmpty()) {
            return;
        }

        readSlotList(tag.getList(TAG_INPUTS, Tag.TAG_COMPOUND), 0, INPUT_SLOTS);
        readSlotList(tag.getList(TAG_OUTPUTS, Tag.TAG_COMPOUND), INPUT_SLOTS, OUTPUT_SLOTS);
    }

    private void loadFilterStateFromPatternItem() {
        this.filterEntries.clear();
        this.excludedInputModIds.clear();
        this.excludedOutputModIds.clear();
        setFilterMode(FILTER_MODE_BLACKLIST);
        ItemStack patternStack = resolvePatternStack();
        if (patternStack.isEmpty()) {
            return;
        }

        CompoundTag tag = patternStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.isEmpty()) {
            return;
        }
        setFilterMode(readFilterMode(tag));
        readFilterEntries(tag);
        readExcludedModIds(tag, true);
        readExcludedModIds(tag, false);
    }

    private void readSlotList(ListTag listTag, int baseSlot, int maxSlots) {
        ItemStackHandler handler = this.itemHandler;
        for (Tag rawTag : listTag) {
            if (!(rawTag instanceof CompoundTag entry)) {
                continue;
            }
            if (!entry.contains(TAG_SLOT, Tag.TAG_INT) || !entry.contains(TAG_STACK, Tag.TAG_COMPOUND)) {
                continue;
            }
            int relativeSlot = entry.getInt(TAG_SLOT);
            if (relativeSlot < 0 || relativeSlot >= maxSlots) {
                continue;
            }
            ItemStack stack = ItemStack.parseOptional(this.player.level().registryAccess(), entry.getCompound(TAG_STACK));
            handler.setStackInSlot(baseSlot + relativeSlot, normalizeLoadedPatternSlotStack(stack));
        }
    }

    private ListTag writeSlotList(int baseSlot, int maxSlots) {
        ListTag list = new ListTag();
        ItemStackHandler handler = this.itemHandler;
        for (int index = 0; index < maxSlots; index++) {
            ItemStack stack = handler.getStackInSlot(baseSlot + index);
            if (stack.isEmpty()) {
                continue;
            }
            CompoundTag entry = new CompoundTag();
            entry.putInt(TAG_SLOT, index);
            entry.put(TAG_STACK, stack.save(this.player.level().registryAccess(), new CompoundTag()));
            list.add(entry);
        }
        return list;
    }

    private void saveToPatternItem() {
        ItemStack patternStack = resolvePatternStack();
        if (patternStack.isEmpty()) {
            return;
        }

        ListTag inputs = writeSlotList(0, INPUT_SLOTS);
        ListTag outputs = writeSlotList(INPUT_SLOTS, OUTPUT_SLOTS);
        boolean encoded = !inputs.isEmpty() || !outputs.isEmpty();
        boolean hasFilterableRecipe = !inputs.isEmpty() && !outputs.isEmpty();

        CompoundTag tag = patternStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(TAG_FILTER_MODE, getFilterMode());
        tag.putBoolean(TAG_ENCODED, encoded);
        writeExcludedModIds(tag, true);
        writeExcludedModIds(tag, false);
        if (encoded) {
            tag.put(TAG_INPUTS, inputs);
            tag.put(TAG_OUTPUTS, outputs);
            writeFluidLists(tag);
            writeGasLists(tag);
            writeLegacyCategoryLists(tag);
            if (hasFilterableRecipe) {
                writeFilterEntries(tag);
            } else {
                this.filterEntries.clear();
                tag.remove(TAG_FILTER_ENTRIES);
            }
            String encodedName = buildEncodedDisplayName();
            if (!encodedName.isEmpty()) {
                tag.putString(TAG_ENCODED_ITEM, encodedName);
            } else {
                tag.remove(TAG_ENCODED_ITEM);
            }
        } else {
            this.filterEntries.clear();
            tag.remove(TAG_INPUTS);
            tag.remove(TAG_OUTPUTS);
            tag.remove(TAG_FILTER_ENTRIES);
            removeFluidLists(tag);
            removeGasLists(tag);
            removeLegacyCategoryLists(tag);
            tag.remove(TAG_ENCODED_ITEM);
        }
        CustomData.set(DataComponents.CUSTOM_DATA, patternStack, tag);
        this.player.getInventory().setChanged();
    }

    private void onPatternSlotsMutated(Player player) {
        if (!player.level().isClientSide) {
            saveToPatternItem();
            this.broadcastChanges();
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.inventoryMenu.broadcastChanges();
            }
        }
    }

    private void setFilterMode(int mode) {
        this.data.set(0, normalizeFilterMode(mode));
    }

    private int normalizeFilterMode(int mode) {
        return mode == FILTER_MODE_WHITELIST ? FILTER_MODE_WHITELIST : FILTER_MODE_BLACKLIST;
    }

    private int readFilterMode(CompoundTag tag) {
        if (tag.contains(TAG_FILTER_MODE, Tag.TAG_INT)) {
            return normalizeFilterMode(tag.getInt(TAG_FILTER_MODE));
        }
        if (tag.contains(TAG_FILTER_MODE_LEGACY, Tag.TAG_INT)) {
            return normalizeFilterMode(tag.getInt(TAG_FILTER_MODE_LEGACY));
        }
        return FILTER_MODE_BLACKLIST;
    }

    private void readFilterEntries(CompoundTag tag) {
        if (!tag.contains(TAG_FILTER_ENTRIES, Tag.TAG_LIST)) {
            return;
        }
        ListTag list = tag.getList(TAG_FILTER_ENTRIES, Tag.TAG_STRING);
        for (int index = 0; index < list.size(); index++) {
            String value = list.getString(index);
            if (value != null && !value.isBlank()) {
                this.filterEntries.add(value);
            }
        }
    }

    private void readExcludedModIds(CompoundTag tag, boolean input) {
        String key = input ? TAG_EXCLUDED_INPUT_MOD_IDS : TAG_EXCLUDED_OUTPUT_MOD_IDS;
        if (!tag.contains(key, Tag.TAG_LIST)) {
            return;
        }
        ListTag list = tag.getList(key, Tag.TAG_STRING);
        LinkedHashSet<String> target = input ? this.excludedInputModIds : this.excludedOutputModIds;
        for (int index = 0; index < list.size(); index++) {
            String value = normalizeModId(list.getString(index));
            if (!value.isBlank()) {
                target.add(value);
            }
        }
    }

    private void writeFilterEntries(CompoundTag tag) {
        if (this.filterEntries.isEmpty()) {
            tag.remove(TAG_FILTER_ENTRIES);
            return;
        }
        ListTag list = new ListTag();
        for (String entry : this.filterEntries) {
            if (entry != null && !entry.isBlank()) {
                list.add(StringTag.valueOf(entry));
            }
        }
        if (list.isEmpty()) {
            tag.remove(TAG_FILTER_ENTRIES);
        } else {
            tag.put(TAG_FILTER_ENTRIES, list);
        }
    }

    private void writeExcludedModIds(CompoundTag tag, boolean input) {
        LinkedHashSet<String> source = input ? this.excludedInputModIds : this.excludedOutputModIds;
        String key = input ? TAG_EXCLUDED_INPUT_MOD_IDS : TAG_EXCLUDED_OUTPUT_MOD_IDS;
        if (source.isEmpty()) {
            tag.remove(key);
            return;
        }
        ListTag list = new ListTag();
        for (String modId : source) {
            if (modId != null && !modId.isBlank()) {
                list.add(StringTag.valueOf(modId));
            }
        }
        if (list.isEmpty()) {
            tag.remove(key);
        } else {
            tag.put(key, list);
        }
    }

    private void toggleFilterEntryByIndex(int entryIndex) {
        List<String> ids = buildFilterEntryIds();
        if (entryIndex < 0 || entryIndex >= ids.size()) {
            return;
        }
        String id = ids.get(entryIndex);
        if (!this.filterEntries.add(id)) {
            this.filterEntries.remove(id);
        }
    }

    private List<String> buildFilterEntryIds() {
        ItemStack patternStack = resolvePatternStack();
        Level patternLevel = this.player.level();
        if (!patternStack.isEmpty() && patternLevel != null) {
            LinkedHashSet<String> expandedIds = new LinkedHashSet<>();
            try {
                for (var detail : TechStartPatternExpansion.expandFilterCandidates(patternStack, patternLevel)) {
                    ItemStack variantStack = detail.getDefinition().toStack();
                    if (variantStack.isEmpty()) {
                        continue;
                    }
                    CompoundTag tag = variantStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                    String id = tag.getString(TAG_VIRTUAL_FILTER_ENTRY_ID);
                    if (id != null && !id.isBlank()) {
                        expandedIds.add(id);
                    }
                }
            } catch (Throwable ignored) {
                expandedIds.clear();
            }
            if (!expandedIds.isEmpty()) {
                return new ArrayList<>(expandedIds);
            }
        }

        List<ItemStack> inputs = new ArrayList<>();
        List<ItemStack> outputs = new ArrayList<>();
        ItemStackHandler handler = this.itemHandler;
        for (int index = 0; index < INPUT_SLOTS; index++) {
            ItemStack stack = handler.getStackInSlot(index);
            if (!stack.isEmpty()) {
                inputs.add(stack.copy());
            }
        }
        for (int index = 0; index < OUTPUT_SLOTS; index++) {
            ItemStack stack = handler.getStackInSlot(INPUT_SLOTS + index);
            if (!stack.isEmpty()) {
                outputs.add(stack.copy());
            }
        }

        List<String> ids = new ArrayList<>();
        if (inputs.isEmpty() || outputs.isEmpty()) {
            return ids;
        }
        for (ItemStack input : inputs) {
            for (ItemStack output : outputs) {
                ids.add(buildFilterEntryId(input, output));
            }
        }
        return ids;
    }

    private String buildEncodedDisplayName() {
        ItemStackHandler handler = this.itemHandler;
        String inputName = "";
        String outputName = "";
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty() && !isFluidMarkerStack(stack) && !isGasMarkerStack(stack)) {
                inputName = stack.getHoverName().getString();
                break;
            }
        }
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            ItemStack stack = handler.getStackInSlot(INPUT_SLOTS + i);
            if (!stack.isEmpty() && !isFluidMarkerStack(stack) && !isGasMarkerStack(stack)) {
                outputName = stack.getHoverName().getString();
                break;
            }
        }
        if (inputName.isEmpty()) {
            FluidMarker fluidMarker = findFirstFluidMarker(0, INPUT_SLOTS);
            if (fluidMarker != null) {
                inputName = getFluidDisplayName(fluidMarker.fluidName(), fluidMarker.amount());
            }
            if (inputName.isEmpty()) {
                GasMarker gasMarker = findFirstGasMarker(0, INPUT_SLOTS);
                if (gasMarker != null) {
                    inputName = getGasDisplayName(gasMarker.gasName());
                }
            }
        }
        if (outputName.isEmpty()) {
            FluidMarker fluidMarker = findFirstFluidMarker(INPUT_SLOTS, OUTPUT_SLOTS);
            if (fluidMarker != null) {
                outputName = getFluidDisplayName(fluidMarker.fluidName(), fluidMarker.amount());
            }
            if (outputName.isEmpty()) {
                GasMarker gasMarker = findFirstGasMarker(INPUT_SLOTS, OUTPUT_SLOTS);
                if (gasMarker != null) {
                    outputName = getGasDisplayName(gasMarker.gasName());
                }
            }
        }
        if (inputName.isEmpty() && outputName.isEmpty()) {
            return "";
        }
        return inputName + " -> " + outputName;
    }

    private void writeFluidLists(CompoundTag tag) {
        ListTag inputFluidNames = new ListTag();
        ListTag inputFluidAmounts = new ListTag();
        collectFluidMarkers(0, INPUT_SLOTS, inputFluidNames, inputFluidAmounts);

        ListTag outputFluidNames = new ListTag();
        ListTag outputFluidAmounts = new ListTag();
        collectFluidMarkers(INPUT_SLOTS, OUTPUT_SLOTS, outputFluidNames, outputFluidAmounts);

        putOrRemoveList(tag, TAG_INPUT_FLUIDS, inputFluidNames);
        putOrRemoveList(tag, TAG_INPUT_FLUID_AMOUNTS, inputFluidAmounts);
        putOrRemoveList(tag, TAG_OUTPUT_FLUIDS, outputFluidNames);
        putOrRemoveList(tag, TAG_OUTPUT_FLUID_AMOUNTS, outputFluidAmounts);
        if (!inputFluidNames.isEmpty() || !outputFluidNames.isEmpty()) {
            TechStartNeoForge.LOGGER.info("Saved fluid lists: inputs={} / {} outputs={} / {}",
                    inputFluidNames,
                    inputFluidAmounts,
                    outputFluidNames,
                    outputFluidAmounts);
        }
    }

    private void writeGasLists(CompoundTag tag) {
        ListTag inputGasNames = new ListTag();
        ListTag inputGasAmounts = new ListTag();
        collectGasMarkers(0, INPUT_SLOTS, inputGasNames, inputGasAmounts);

        ListTag outputGasNames = new ListTag();
        ListTag outputGasAmounts = new ListTag();
        collectGasMarkers(INPUT_SLOTS, OUTPUT_SLOTS, outputGasNames, outputGasAmounts);

        putOrRemoveList(tag, TAG_INPUT_GASES, inputGasNames);
        putOrRemoveList(tag, TAG_INPUT_GAS_AMOUNTS, inputGasAmounts);
        putOrRemoveList(tag, TAG_OUTPUT_GASES, outputGasNames);
        putOrRemoveList(tag, TAG_OUTPUT_GAS_AMOUNTS, outputGasAmounts);
        if (!inputGasNames.isEmpty() || !outputGasNames.isEmpty()) {
            TechStartNeoForge.LOGGER.info("Saved gas lists: inputs={} / {} outputs={} / {}",
                    inputGasNames,
                    inputGasAmounts,
                    outputGasNames,
                    outputGasAmounts);
        }
    }

    private void removeFluidLists(CompoundTag tag) {
        tag.remove(TAG_INPUT_FLUIDS);
        tag.remove(TAG_INPUT_FLUID_AMOUNTS);
        tag.remove(TAG_OUTPUT_FLUIDS);
        tag.remove(TAG_OUTPUT_FLUID_AMOUNTS);
    }

    private void removeGasLists(CompoundTag tag) {
        tag.remove(TAG_INPUT_GASES);
        tag.remove(TAG_INPUT_GAS_AMOUNTS);
        tag.remove(TAG_OUTPUT_GASES);
        tag.remove(TAG_OUTPUT_GAS_AMOUNTS);
    }

    private void writeLegacyCategoryLists(CompoundTag tag) {
        writeLegacyCategorySide(tag, true);
        writeLegacyCategorySide(tag, false);
    }

    private void writeLegacyCategorySide(CompoundTag tag, boolean input) {
        int baseSlot = input ? 0 : INPUT_SLOTS;
        int maxSlots = input ? INPUT_SLOTS : OUTPUT_SLOTS;
        String listKey = input ? TAG_INPUT_ORES : TAG_OUTPUT_ORES;
        String countsKey = input ? TAG_INPUT_COUNTS : TAG_OUTPUT_COUNTS;
        String singleKey = input ? "InputOreName" : "OutputOreName";
        String singleCountKey = input ? "InputCount" : "OutputCount";

        ListTag names = new ListTag();
        ListTag counts = new ListTag();
        ItemStackHandler handler = this.itemHandler;
        for (int index = 0; index < maxSlots; index++) {
            ItemStack stack = handler.getStackInSlot(baseSlot + index);
            if (stack.isEmpty() || isFluidMarkerStack(stack) || isGasMarkerStack(stack)) {
                continue;
            }
            String legacyKey = inferLegacyCategoryKey(stack);
            if (legacyKey == null || legacyKey.isBlank()) {
                continue;
            }
            names.add(StringTag.valueOf(legacyKey));
            counts.add(IntTag.valueOf(getItemMarkerAmount(stack)));
        }

        if (names.isEmpty()) {
            tag.remove(listKey);
            tag.remove(countsKey);
            tag.remove(singleKey);
            tag.remove(singleCountKey);
            return;
        }

        tag.put(listKey, names);
        tag.put(countsKey, counts);
        tag.putString(singleKey, names.getString(0));
        tag.putInt(singleCountKey, counts.getInt(0));
    }

    private void removeLegacyCategoryLists(CompoundTag tag) {
        tag.remove(TAG_INPUT_ORES);
        tag.remove(TAG_OUTPUT_ORES);
        tag.remove(TAG_INPUT_COUNTS);
        tag.remove(TAG_OUTPUT_COUNTS);
        tag.remove("InputOreName");
        tag.remove("OutputOreName");
        tag.remove("InputCount");
        tag.remove("OutputCount");
    }

    private @Nullable String inferLegacyCategoryKey(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        for (var tagKey : stack.getTags().toList()) {
            ResourceLocation location = tagKey.location();
            if (!"forge".equals(location.getNamespace()) && !"c".equals(location.getNamespace())) {
                continue;
            }
            String[] parts = location.getPath().split("/");
            if (parts.length < 2) {
                continue;
            }
            String prefix = switch (parts[0]) {
                case "ingots" -> "ingot";
                case "plates" -> "plate";
                case "storage_blocks" -> "block";
                case "nuggets" -> "nugget";
                case "rods" -> "rod";
                case "gears" -> "gear";
                case "wires" -> "wire";
                case "dusts" -> "dust";
                case "ores" -> "ore";
                case "gems" -> "gem";
                default -> null;
            };
            if (prefix == null) {
                continue;
            }
            StringBuilder material = new StringBuilder();
            for (int index = 1; index < parts.length; index++) {
                if (parts[index].isBlank()) {
                    continue;
                }
                if (!material.isEmpty()) {
                    material.append('_');
                }
                material.append(parts[index]);
            }
            if (!material.isEmpty()) {
                return prefix + toUpperCamel(material.toString());
            }
        }
        return null;
    }

    private String toUpperCamel(String raw) {
        StringBuilder builder = new StringBuilder();
        boolean upper = true;
        for (int index = 0; index < raw.length(); index++) {
            char current = raw.charAt(index);
            if (current == '_' || current == '-' || current == '/') {
                upper = true;
                continue;
            }
            builder.append(upper ? Character.toUpperCase(current) : current);
            upper = false;
        }
        return builder.toString();
    }

    private void putOrRemoveList(CompoundTag tag, String key, ListTag list) {
        if (list.isEmpty()) {
            tag.remove(key);
        } else {
            tag.put(key, list);
        }
    }

    private void collectFluidMarkers(int baseSlot, int maxSlots, ListTag names, ListTag amounts) {
        ItemStackHandler handler = this.itemHandler;
        for (int index = 0; index < maxSlots; index++) {
            FluidMarker marker = extractFluidMarker(handler.getStackInSlot(baseSlot + index));
            if (marker == null) {
                continue;
            }
            names.add(StringTag.valueOf(marker.fluidName()));
            amounts.add(IntTag.valueOf(marker.amount()));
        }
    }

    private void collectGasMarkers(int baseSlot, int maxSlots, ListTag names, ListTag amounts) {
        ItemStackHandler handler = this.itemHandler;
        for (int index = 0; index < maxSlots; index++) {
            GasMarker marker = extractGasMarker(handler.getStackInSlot(baseSlot + index));
            if (marker == null) {
                continue;
            }
            names.add(StringTag.valueOf(marker.gasName()));
            amounts.add(IntTag.valueOf(marker.amount()));
        }
    }

    private @Nullable FluidMarker findFirstFluidMarker(int baseSlot, int maxSlots) {
        ItemStackHandler handler = this.itemHandler;
        for (int index = 0; index < maxSlots; index++) {
            FluidMarker marker = extractFluidMarker(handler.getStackInSlot(baseSlot + index));
            if (marker != null) {
                return marker;
            }
        }
        return null;
    }

    private @Nullable GasMarker findFirstGasMarker(int baseSlot, int maxSlots) {
        ItemStackHandler handler = this.itemHandler;
        for (int index = 0; index < maxSlots; index++) {
            GasMarker marker = extractGasMarker(handler.getStackInSlot(baseSlot + index));
            if (marker != null) {
                return marker;
            }
        }
        return null;
    }

    private String getFluidDisplayName(String fluidId, int amount) {
        if (fluidId == null || fluidId.isBlank()) {
            return "unknown";
        }
        ResourceLocation key = ResourceLocation.tryParse(fluidId.trim());
        if (key == null) {
            return fluidId;
        }
        return BuiltInRegistries.FLUID.getOptional(key)
                .filter(fluid -> fluid != Fluids.EMPTY)
                .map(fluid -> new FluidStack(fluid, Math.max(1, amount)).getHoverName().getString())
                .orElse(fluidId);
    }

    private String getGasDisplayName(String gasId) {
        return MekanismGasHelper.getDisplayName(gasId).getString();
    }

    public static String buildFilterEntryId(ItemStack input, ItemStack output) {
        return buildStackDescriptor(input) + "->" + buildStackDescriptor(output);
    }

    private static String buildStackDescriptor(ItemStack stack) {
        if (stack.isEmpty()) {
            return "empty";
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.isEmpty()) {
            if (tag.getBoolean(TAG_FLUID_MARKER) && tag.contains(TAG_FLUID_NAME, Tag.TAG_STRING)) {
                return "fluid|" + tag.getString(TAG_FLUID_NAME) + "|" + Math.max(1, tag.getInt(TAG_FLUID_AMOUNT));
            }
            if (tag.getBoolean(TAG_GAS_MARKER) && tag.contains(TAG_GAS_NAME, Tag.TAG_STRING)) {
                return "gas|" + tag.getString(TAG_GAS_NAME) + "|" + Math.max(1, tag.getInt(TAG_GAS_AMOUNT));
            }
            if (tag.getBoolean(TAG_ITEM_MARKER)) {
                CompoundTag cleaned = tag.copy();
                cleaned.remove(TAG_ITEM_MARKER);
                cleaned.remove(TAG_ITEM_AMOUNT);
                String itemId = String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()));
                String nbt = cleaned.isEmpty() ? "" : cleaned.toString();
                return itemId + "|item|" + Math.max(1, tag.getInt(TAG_ITEM_AMOUNT)) + "|" + nbt;
            }
        }
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String id = key == null ? "unknown" : key.toString();
        return id + "|" + stack.getCount() + "|" + stack.getComponentsPatch();
    }

    private static List<String> normalizeModIds(String[] values) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (values == null) {
            return List.of();
        }
        for (String value : values) {
            String modId = normalizeModId(value);
            if (!modId.isBlank()) {
                normalized.add(modId);
            }
        }
        return new ArrayList<>(normalized);
    }

    private static String normalizeModId(String modId) {
        return modId == null ? "" : modId.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isItemMarkerStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getBoolean(TAG_ITEM_MARKER);
    }

    private int getItemMarkerAmount(ItemStack stack) {
        if (stack.isEmpty()) {
            return 1;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.getBoolean(TAG_ITEM_MARKER)) {
            return Math.max(1, tag.getInt(TAG_ITEM_AMOUNT));
        }
        return Math.max(1, stack.getCount());
    }

    private void applyItemMarkerAmount(ItemStack stack, int amount) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean(TAG_ITEM_MARKER, true);
        tag.putInt(TAG_ITEM_AMOUNT, Math.max(1, Math.min(amount, TechStartConfig.getItemMarkerMaxAmount())));
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
        stack.setCount(1);
    }

    private void applyPatternSlotCount(int slotId, int count) {
        ItemStackHandler handler = this.itemHandler;
        ItemStack stack = handler.getStackInSlot(slotId);
        if (stack.isEmpty()) {
            return;
        }

        int amountLimit = Math.min(MAX_ENCODED_SLOT_COUNT, getAmountLimitForStack(stack));

        GasMarker gasMarker = extractGasMarker(stack);
        if (gasMarker != null) {
            int target = Mth.clamp(count, 1, amountLimit);
            TechStartNeoForge.LOGGER.info("applyPatternSlotCount gas slot={} current={} target={}", slotId, gasMarker.amount(), target);
            ItemStack updated = stack.copy();
            CompoundTag tag = updated.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            tag.putBoolean(TAG_GAS_MARKER, true);
            tag.putString(TAG_GAS_NAME, gasMarker.gasName());
            tag.putInt(TAG_GAS_AMOUNT, target);
            tag.remove(TAG_FLUID_MARKER);
            tag.remove(TAG_FLUID_NAME);
            tag.remove(TAG_FLUID_AMOUNT);
            CustomData.set(DataComponents.CUSTOM_DATA, updated, tag);
            updated.setCount(1);
            handler.setStackInSlot(slotId, updated);
            return;
        }

        FluidMarker fluidMarker = extractFluidMarker(stack);
        if (fluidMarker != null) {
            int target = Mth.clamp(count, 1, amountLimit);
            TechStartNeoForge.LOGGER.info("applyPatternSlotCount fluid slot={} current={} target={}", slotId, fluidMarker.amount(), target);
            ItemStack updated = stack.copy();
            CompoundTag tag = updated.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            tag.putBoolean(TAG_FLUID_MARKER, true);
            tag.putString(TAG_FLUID_NAME, fluidMarker.fluidName());
            tag.putInt(TAG_FLUID_AMOUNT, target);
            tag.remove(TAG_GAS_MARKER);
            tag.remove(TAG_GAS_NAME);
            tag.remove(TAG_GAS_AMOUNT);
            CustomData.set(DataComponents.CUSTOM_DATA, updated, tag);
            updated.setCount(1);
            handler.setStackInSlot(slotId, updated);
            return;
        }

        int target = Mth.clamp(count, 1, amountLimit);
        ItemStack updated = createItemMarkerStack(stack, target);
        if (!updated.isEmpty()) {
            handler.setStackInSlot(slotId, updated);
        }
    }

    private ItemStack normalizePatternSlotStack(ItemStack incoming) {
        if (incoming.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (isFluidMarkerStack(incoming)) {
            FluidMarker marker = extractFluidMarker(incoming);
            return marker == null ? ItemStack.EMPTY : createFluidMarkerStack(incoming, marker);
        }
        if (isGasMarkerStack(incoming)) {
            GasMarker marker = extractGasMarker(incoming);
            return marker == null ? ItemStack.EMPTY : createGasMarkerStack(incoming, marker);
        }
        if (isItemMarkerStack(incoming)) {
            return createItemMarkerStack(incoming, getItemMarkerAmount(incoming));
        }
        GasMarker gasMarker = extractGasFromContainer(incoming);
        if (gasMarker != null) {
            return createGasMarkerStack(incoming, gasMarker);
        }
        FluidMarker fluidMarker = extractFluidFromContainer(incoming);
        if (fluidMarker != null) {
            return createFluidMarkerStack(incoming, fluidMarker);
        }
        return createItemMarkerStack(incoming, 1);
    }

    private ItemStack normalizeLoadedPatternSlotStack(ItemStack loaded) {
        if (loaded.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (isFluidMarkerStack(loaded) || isGasMarkerStack(loaded) || isItemMarkerStack(loaded)) {
            return normalizePatternSlotStack(loaded);
        }
        GasMarker gasMarker = extractGasFromContainer(loaded);
        if (gasMarker != null) {
            return createGasMarkerStack(loaded, gasMarker);
        }
        FluidMarker fluidMarker = extractFluidFromContainer(loaded);
        if (fluidMarker != null) {
            return createFluidMarkerStack(loaded, fluidMarker);
        }
        return createItemMarkerStack(loaded, Math.max(1, loaded.getCount()));
    }

    private boolean isFluidMarkerStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getBoolean(TAG_FLUID_MARKER) && tag.contains(TAG_FLUID_NAME, Tag.TAG_STRING);
    }

    private boolean isGasMarkerStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getBoolean(TAG_GAS_MARKER) && tag.contains(TAG_GAS_NAME, Tag.TAG_STRING);
    }

    private ItemStack createItemMarkerStack(ItemStack source, int amount) {
        if (source.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack marker = source.copy();
        marker.setCount(1);
        applyItemMarkerAmount(marker, amount);
        return marker;
    }

    private ItemStack createFluidMarkerStack(ItemStack source, FluidMarker marker) {
        if (marker == null || marker.fluidName() == null || marker.fluidName().isBlank()) {
            return ItemStack.EMPTY;
        }
        ItemStack markerStack = createFluidDisplayStack(source, marker.fluidName());
        CompoundTag tag = markerStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean(TAG_FLUID_MARKER, true);
        tag.putString(TAG_FLUID_NAME, marker.fluidName());
        tag.putInt(TAG_FLUID_AMOUNT, sanitizeMarkerAmount(marker.amount()));
        CustomData.set(DataComponents.CUSTOM_DATA, markerStack, tag);
        String displayName = getFluidDisplayName(marker.fluidName(), marker.amount());
        if (displayName != null && !displayName.isBlank()) {
            markerStack.set(DataComponents.CUSTOM_NAME, Component.literal(displayName));
        }
        markerStack.setCount(1);
        return markerStack;
    }

    private ItemStack createGasMarkerStack(ItemStack source, GasMarker marker) {
        if (marker == null || marker.gasName() == null || marker.gasName().isBlank()) {
            return ItemStack.EMPTY;
        }
        ItemStack markerStack = new ItemStack(Items.GLASS_BOTTLE);
        CompoundTag tag = markerStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean(TAG_GAS_MARKER, true);
        tag.putString(TAG_GAS_NAME, marker.gasName());
        tag.putInt(TAG_GAS_AMOUNT, sanitizeMarkerAmount(marker.amount()));
        CustomData.set(DataComponents.CUSTOM_DATA, markerStack, tag);
        String displayName = getGasDisplayName(marker.gasName());
        if (displayName != null && !displayName.isBlank()) {
            markerStack.set(DataComponents.CUSTOM_NAME, Component.literal(displayName));
        }
        markerStack.setCount(1);
        return markerStack;
    }

    private ItemStack createFluidDisplayStack(ItemStack source, String fluidName) {
        ResourceLocation key = ResourceLocation.tryParse(fluidName == null ? "" : fluidName.trim());
        if (key != null) {
            ItemStack bucketStack = BuiltInRegistries.FLUID.getOptional(key)
                    .filter(fluid -> fluid != Fluids.EMPTY)
                    .map(fluid -> fluid.getBucket())
                    .filter(bucket -> bucket != Items.AIR)
                    .map(ItemStack::new)
                    .orElse(ItemStack.EMPTY);
            if (!bucketStack.isEmpty()) {
                return bucketStack;
            }
        }
        return new ItemStack(Items.BUCKET);
    }

    private @Nullable FluidMarker extractFluidMarker(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.getBoolean(TAG_GAS_MARKER) && tag.contains(TAG_GAS_NAME, Tag.TAG_STRING)) {
            return null;
        }
        if (tag.getBoolean(TAG_FLUID_MARKER) && tag.contains(TAG_FLUID_NAME, Tag.TAG_STRING)) {
            String name = tag.getString(TAG_FLUID_NAME);
            if (!name.isBlank()) {
                int amount = tag.contains(TAG_FLUID_AMOUNT, Tag.TAG_INT) ? tag.getInt(TAG_FLUID_AMOUNT) : 1000;
                return new FluidMarker(name, sanitizeMarkerAmount(amount));
            }
        }
        return extractFluidFromContainer(stack);
    }

    private @Nullable GasMarker extractGasMarker(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.getBoolean(TAG_GAS_MARKER) && tag.contains(TAG_GAS_NAME, Tag.TAG_STRING)) {
            String name = tag.getString(TAG_GAS_NAME);
            if (!name.isBlank()) {
                int amount = tag.contains(TAG_GAS_AMOUNT, Tag.TAG_INT) ? tag.getInt(TAG_GAS_AMOUNT) : 1;
                return new GasMarker(name, sanitizeMarkerAmount(amount));
            }
        }
        return extractGasFromContainer(stack);
    }

    private @Nullable FluidMarker extractFluidFromContainer(ItemStack stack) {
        if (isGasMarkerStack(stack)) {
            return null;
        }
        FluidStack contained = FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
        if (!contained.isEmpty() && contained.getFluid() != null) {
            String key = BuiltInRegistries.FLUID.getKey(contained.getFluid()).toString();
            return new FluidMarker(key, sanitizeMarkerAmount(contained.getAmount()));
        }
        if (stack.getItem() instanceof BucketItem bucketItem && bucketItem.content != Fluids.EMPTY) {
            String key = BuiltInRegistries.FLUID.getKey(bucketItem.content).toString();
            return new FluidMarker(key, sanitizeMarkerAmount(1000));
        }
        IFluidHandlerItem handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (handler != null) {
            FluidMarker marker = extractFirstFluid(handler);
            if (marker != null) {
                return marker;
            }
        }
        return extractFluidFromTag(stack);
    }

    private @Nullable FluidMarker extractFirstFluid(IFluidHandlerItem handler) {
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack fluidStack = handler.getFluidInTank(tank);
            if (fluidStack.isEmpty() || fluidStack.getFluid() == null) {
                continue;
            }
            String key = BuiltInRegistries.FLUID.getKey(fluidStack.getFluid()).toString();
            return new FluidMarker(key, sanitizeMarkerAmount(fluidStack.getAmount()));
        }
        return null;
    }

    private @Nullable GasMarker extractGasFromContainer(ItemStack stack) {
        MekanismGasHelper.GasStackView gas = MekanismGasHelper.extractGasOrTag(stack);
        if (gas == null || gas.gasId() == null || gas.gasId().isBlank()) {
            return null;
        }
        return new GasMarker(gas.gasId(), sanitizeMarkerAmount(gas.amount()));
    }

    private @Nullable FluidMarker extractFluidFromTag(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.isEmpty() || (tag.getBoolean(TAG_GAS_MARKER) && tag.contains(TAG_GAS_NAME, Tag.TAG_STRING))) {
            return null;
        }
        Matcher matcher = RESOURCE_LOCATION_PATTERN.matcher(tag.toString().toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String candidate = matcher.group(1);
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(candidate);
            if (id == null) {
                continue;
            }
            if (BuiltInRegistries.FLUID.getOptional(id).orElse(Fluids.EMPTY) == Fluids.EMPTY) {
                continue;
            }
            return new FluidMarker(id.toString(), sanitizeMarkerAmount(1000));
        }
        return null;
    }

    private int sanitizeMarkerAmount(int amount) {
        return Mth.clamp(amount, 1, TechStartConfig.getFluidGasMarkerMaxAmount());
    }

    private record FluidMarker(String fluidName, int amount) {
    }

    private record GasMarker(String gasName, int amount) {
    }
}
