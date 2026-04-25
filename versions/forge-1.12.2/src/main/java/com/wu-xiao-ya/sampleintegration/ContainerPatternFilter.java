package com.lwx1145.sampleintegration;


import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ContainerPatternFilter extends Container {
    private static final String TAG_VIRTUAL_FILTER_ENTRY_ID = "VirtualFilterEntryId";

    private final EntityPlayer player;
    private final ItemStack patternStack;

    public ContainerPatternFilter(EntityPlayer player) {
        this.player = player;
        ItemStack mainHandStack = player.getHeldItemMainhand();
        if (!mainHandStack.isEmpty() && mainHandStack.getItem() instanceof ItemTest) {
            this.patternStack = mainHandStack;
        } else {
            ItemStack offHandStack = player.getHeldItemOffhand();
            if (!offHandStack.isEmpty() && offHandStack.getItem() instanceof ItemTest) {
                this.patternStack = offHandStack;
            } else {
                this.patternStack = findPatternInInventory(player);
            }
        }

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlotToContainer(new Slot(player.inventory, j + i * 9 + 9, 8 + j * 18, 125 + i * 18));
            }
        }

        for (int k = 0; k < 9; ++k) {
            this.addSlotToContainer(new Slot(player.inventory, k, 8 + k * 18, 185));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }

    public int getFilterMode() {
        return ItemTest.getFilterModeStatic(patternStack);
    }

    public ItemStack getPatternStack() {
        return patternStack;
    }

    public List<String> getFilterEntries() {
        return ItemTest.getFilterEntriesStatic(patternStack);
    }

    public void applyFilterMode(int mode) {
        ItemTest.setFilterModeStatic(patternStack, mode);
        markDirty();
    }

    public void toggleFilterEntry(String entry) {
        ItemTest.toggleFilterEntryStatic(patternStack, entry);
        markDirty();
    }

    public void clearFilterEntries() {
        ItemTest.clearFilterEntriesStatic(patternStack);
        markDirty();
    }

    private void markDirty() {
        this.player.inventory.markDirty();
    }

    public List<String[]> getAvailableRecipeTypes() {
        List<String[]> recipeTypes = new ArrayList<>();
        if (patternStack == null || patternStack.isEmpty() || !(patternStack.getItem() instanceof ItemTest)) {
            return recipeTypes;
        }

        java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>();
        SmartPatternDetails mainPattern = new SmartPatternDetails(patternStack);
        List<SmartPatternDetails> details = mainPattern.expandToVirtualPatterns();

        for (SmartPatternDetails detail : details) {
            if (detail == null) {
                continue;
            }
            ItemStack detailPattern = detail.getPattern();
            if (detailPattern == null || detailPattern.isEmpty()) {
                continue;
            }

            String input = ItemTest.getInputOreNameStatic(detailPattern);
            String output = ItemTest.getOutputOreNameStatic(detailPattern);
            if (input == null || input.isEmpty() || output == null || output.isEmpty()) {
                continue;
            }

            String displayName = ItemTest.getEncodedItemNameStatic(detailPattern);
            if (displayName == null || displayName.isEmpty()) {
                displayName = input + " -> " + output;
            }

            String entryId = resolveFilterEntryId(detailPattern, input, output);
            String deDupKey = entryId + "#" + displayName;
            if (seen.add(deDupKey)) {
                recipeTypes.add(new String[]{input, output, displayName, entryId});
            }
        }

        if (!recipeTypes.isEmpty()) {
            return recipeTypes;
        }
        if (mainPattern.isWildcardPattern()) {
            return recipeTypes;
        }

        String inputOreName = ItemTest.getInputOreNameStatic(patternStack);
        String outputOreName = ItemTest.getOutputOreNameStatic(patternStack);
        if (inputOreName == null || inputOreName.isEmpty() || outputOreName == null || outputOreName.isEmpty()) {
            return recipeTypes;
        }
        if (!hasAllowedSideStack(inputOreName, true) || !hasAllowedSideStack(outputOreName, false)) {
            return recipeTypes;
        }

        // Fallback for unexpected old pattern data.
        String legacyDisplay = inputOreName + " -> " + outputOreName;
        recipeTypes.add(new String[]{inputOreName, outputOreName, legacyDisplay, buildLegacyId(inputOreName, outputOreName)});
        return recipeTypes;
    }

    private String resolveFilterEntryId(ItemStack detailPattern, String inputOre, String outputOre) {
        String legacy = buildLegacyId(inputOre, outputOre);
        if (detailPattern == null || detailPattern.isEmpty() || !detailPattern.hasTagCompound()) {
            return legacy;
        }
        if (detailPattern.getTagCompound().hasKey(TAG_VIRTUAL_FILTER_ENTRY_ID)) {
            String id = detailPattern.getTagCompound().getString(TAG_VIRTUAL_FILTER_ENTRY_ID);
            if (id != null && !id.isEmpty()) {
                return id;
            }
        }
        return legacy;
    }

    private List<String[]> deriveRecipes(String inputOreName, String outputOreName) {
        List<String[]> derived = new ArrayList<>();
        String inputBaseType = normalizeBaseType(inputOreName);
        String outputBaseType = normalizeBaseType(outputOreName);
        if (inputBaseType == null || outputBaseType == null) {
            return derived;
        }

        Set<String> inputMaterials = new LinkedHashSet<>();
        Set<String> outputMaterials = new LinkedHashSet<>();
        for (String oreName : OreDictionary.getOreNames()) {
            if (oreName.startsWith(inputBaseType)) {
                inputMaterials.add(oreName.substring(inputBaseType.length()));
            }
            if (oreName.startsWith(outputBaseType)) {
                outputMaterials.add(oreName.substring(outputBaseType.length()));
            }
        }
        inputMaterials.retainAll(outputMaterials);
        Set<String> seenVariantIds = new LinkedHashSet<>();

        for (String material : inputMaterials) {
            if (material == null || material.isEmpty()) {
                continue;
            }
            String derivedInputOre = inputBaseType + material;
            String derivedOutputOre = outputBaseType + material;
            if (!OreDictionary.doesOreNameExist(derivedInputOre) || !OreDictionary.doesOreNameExist(derivedOutputOre)) {
                continue;
            }
            List<ItemStack> inputItems = OreDictionary.getOres(derivedInputOre);
            List<ItemStack> outputItems = OreDictionary.getOres(derivedOutputOre);
            if (!inputItems.isEmpty() && !outputItems.isEmpty()) {
                boolean addedAny = false;
                for (ItemStack inputItem : inputItems) {
                    if (inputItem == null || inputItem.isEmpty()) {
                        continue;
                    }
                    if (!ItemTest.isInputStackAllowedForPattern(patternStack, inputItem)) {
                        continue;
                    }
                    ItemStack outputItem = selectOutputForInput(inputItem, outputItems);
                    if (outputItem == null || outputItem.isEmpty()) {
                        continue;
                    }
                    String entryId = buildVariantId(derivedInputOre, derivedOutputOre, inputItem, outputItem);
                    if (!seenVariantIds.add(entryId)) {
                        continue;
                    }
                    String displayName = inputItem.getDisplayName() + " -> " + outputItem.getDisplayName();
                    derived.add(new String[]{derivedInputOre, derivedOutputOre, displayName, entryId});
                    addedAny = true;
                }
                if (!addedAny) {
                    String displayName = inputItems.get(0).getDisplayName() + " -> " + outputItems.get(0).getDisplayName();
                    String legacyId = buildLegacyId(derivedInputOre, derivedOutputOre);
                    if (seenVariantIds.add(legacyId)) {
                        derived.add(new String[]{derivedInputOre, derivedOutputOre, displayName, legacyId});
                    }
                }
            }
        }
        return derived;
    }

    private static String buildLegacyId(String inputOre, String outputOre) {
        return inputOre + "->" + outputOre;
    }

    private static String buildItemKey(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() == null) {
            return "empty";
        }
        String registry = stack.getItem().getRegistryName() == null
            ? "unknown:unknown"
            : stack.getItem().getRegistryName().toString();
        return registry + "@" + stack.getMetadata();
    }

    private static String buildVariantId(String inputOre, String outputOre, ItemStack inputItem, ItemStack outputItem) {
        return buildLegacyId(inputOre, outputOre) + "|" + buildItemKey(inputItem) + "=>" + buildItemKey(outputItem);
    }

    private ItemStack selectOutputForInput(ItemStack inputItem, List<ItemStack> outputItems) {
        if (inputItem == null || inputItem.isEmpty() || outputItems == null || outputItems.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack firstValid = ItemStack.EMPTY;
        ItemStack bestSameNamespace = ItemStack.EMPTY;
        String inputNamespace = getRegistryNamespace(inputItem);
        String inputPath = getRegistryPath(inputItem);
        int bestScore = -1;

        for (ItemStack outputItem : outputItems) {
            if (outputItem == null || outputItem.isEmpty()) {
                continue;
            }
            if (!ItemTest.isOutputStackAllowedForPattern(patternStack, outputItem)) {
                continue;
            }
            if (firstValid.isEmpty()) {
                firstValid = outputItem;
            }

            if (inputNamespace.isEmpty()) {
                continue;
            }
            String outputNamespace = getRegistryNamespace(outputItem);
            if (!inputNamespace.equals(outputNamespace)) {
                continue;
            }

            int score = commonPrefixLength(inputPath, getRegistryPath(outputItem));
            if (score > bestScore) {
                bestScore = score;
                bestSameNamespace = outputItem;
            }
        }

        if (!bestSameNamespace.isEmpty()) {
            return bestSameNamespace;
        }
        return firstValid;
    }

    private String getRegistryNamespace(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() == null || stack.getItem().getRegistryName() == null) {
            return "";
        }
        ResourceLocation rl = stack.getItem().getRegistryName();
        return rl == null ? "" : rl.getNamespace();
    }

    private String getRegistryPath(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() == null || stack.getItem().getRegistryName() == null) {
            return "";
        }
        ResourceLocation rl = stack.getItem().getRegistryName();
        return rl == null ? "" : rl.getPath();
    }

    private int commonPrefixLength(String a, String b) {
        if (a == null || b == null) {
            return 0;
        }
        int len = Math.min(a.length(), b.length());
        int i = 0;
        while (i < len && a.charAt(i) == b.charAt(i)) {
            i++;
        }
        return i;
    }

    private String normalizeBaseType(String oreName) {
        if (oreName == null || oreName.isEmpty()) {
            return null;
        }
        String name = oreName.endsWith("*") ? oreName.substring(0, oreName.length() - 1) : oreName;
        return extractBaseType(name);
    }

    private String extractBaseType(String oreName) {
        if (oreName == null) return null;
        return OreDictRecipeCache.findPrefix(oreName);
    }

    private ItemStack findPatternInInventory(EntityPlayer player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemTest) {
                return stack;
            }
        }
        for (int i = 9; i < 36; i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemTest) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private boolean hasAllowedSideStack(String oreName, boolean inputSide) {
        if (oreName == null || oreName.isEmpty()) {
            return false;
        }
        List<ItemStack> stacks = OreDictionary.getOres(oreName);
        if (stacks == null || stacks.isEmpty()) {
            return false;
        }
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            boolean allowed = inputSide
                ? ItemTest.isInputStackAllowedForPattern(patternStack, stack)
                : ItemTest.isOutputStackAllowedForPattern(patternStack, stack);
            if (allowed) {
                return true;
            }
        }
        return false;
    }
}

