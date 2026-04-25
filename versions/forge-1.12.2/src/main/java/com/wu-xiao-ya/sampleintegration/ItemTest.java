

package com.lwx1145.sampleintegration;


import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraft.client.util.ITooltipFlag;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;

/**
 */
public class ItemTest extends Item implements ICraftingPatternItem {
    private static final String TAG_INPUT_ORES = "InputOreNames";
    private static final String TAG_OUTPUT_ORES = "OutputOreNames";
    private static final String TAG_INPUT_COUNTS = "InputCounts";
    private static final String TAG_OUTPUT_COUNTS = "OutputCounts";

    private static final String TAG_INPUT_FLUIDS = "InputFluids";
    private static final String TAG_INPUT_FLUID_AMOUNTS = "InputFluidAmounts";
    private static final String TAG_OUTPUT_FLUIDS = "OutputFluids";
    private static final String TAG_OUTPUT_FLUID_AMOUNTS = "OutputFluidAmounts";

    private static final String TAG_INPUT_GASES = "InputGases";
    private static final String TAG_INPUT_GAS_AMOUNTS = "InputGasAmounts";
    private static final String TAG_OUTPUT_GASES = "OutputGases";
    private static final String TAG_OUTPUT_GAS_AMOUNTS = "OutputGasAmounts";
    private static final String TAG_INPUT_GAS_ITEMS = "InputGasItems";
    private static final String TAG_OUTPUT_GAS_ITEMS = "OutputGasItems";
    private static final String TAG_FILTER_MODE = "FilterMode";
    private static final String TAG_FILTER_ENTRIES = "FilterEntries";
    private static final String TAG_EXCLUDED_INPUT_MOD_IDS = "ExcludedInputModIds";
    private static final String TAG_EXCLUDED_OUTPUT_MOD_IDS = "ExcludedOutputModIds";
    private static final String TAG_ITEM_MARKER = "sampleintegrationItemMarker";
    private static final String TAG_ITEM_AMOUNT = "sampleintegrationItemAmount";
    private static final String TAG_EDITOR_INPUT_SLOTS = "EditorInputSlots";
    private static final String TAG_EDITOR_OUTPUT_SLOTS = "EditorOutputSlots";
    private static final String TAG_EDITOR_SLOT = "Slot";
    private static final String TAG_EDITOR_STACK = "Stack";
    public static final int FILTER_MODE_WHITELIST = 0;
    public static final int FILTER_MODE_BLACKLIST = 1;
    /**
     */
    public ItemTest() {
        setTranslationKey("sampleintegration.pattern_integrations");
        setRegistryName("sampleintegration", "pattern_integrations");
        setCreativeTab(CreativeTabs.TOOLS);
        setMaxStackSize(1);
    }

    /**
     */
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        EnumActionResult result = handlePatternInteraction(world, player, stack);
        return new ActionResult<>(result, stack);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
                                      EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack stack = player.getHeldItem(hand);
        return handlePatternInteraction(world, player, stack);
    }

    private EnumActionResult handlePatternInteraction(World world, EntityPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return EnumActionResult.PASS;
        }

        if (player.isSneaking()) {
            clearEncodedItem(stack);
            if (!world.isRemote) {
                player.sendMessage(new TextComponentTranslation("message.sampleintegration.pattern_cleared"));
            }
            return EnumActionResult.SUCCESS;
        }

        if (!world.isRemote) {
            player.openGui(TechStart.INSTANCE, GuiHandler.PATTERN_EDITOR_GUI, world, 0, 0, 0);
        }

        return EnumActionResult.SUCCESS;
    }

    /**
     */
    public boolean hasEncodedItem(ItemStack stack) {
        if (!stack.hasTagCompound()) return false;
        return stack.getTagCompound().hasKey("EncodedItem");
    }

    /**
     */
    public String getEncodedItemName(ItemStack stack) {
        if (!hasEncodedItem(stack)) return "";
        String raw = stack.getTagCompound().getString("EncodedItem");
        String normalized = normalizeDisplayName(raw);
        if (!raw.equals(normalized)) {
            stack.getTagCompound().setString("EncodedItem", normalized);
        }
        return normalized;
    }

    private static String normalizeDisplayName(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String normalized = value
            .replace("\u2192", "->")
            .replace("\u21D2", "->")
            .replace("\u27F6", "->");
        normalized = normalized.replaceAll("\\s*[^\\p{ASCII}]?\\?\\s*", " -> ");
        normalized = normalized.replaceAll("\\s*->\\s*", " -> ");
        return normalized.trim();
    }

    /**
     */
    public void setEncodedItem(ItemStack stack, String inputOreName, String outputOreName, String displayName) {
        List<String> inputOres = new ArrayList<>();
        List<String> outputOres = new ArrayList<>();
        List<Integer> inputCounts = new ArrayList<>();
        List<Integer> outputCounts = new ArrayList<>();
        inputOres.add(inputOreName);
        outputOres.add(outputOreName);
        inputCounts.add(1);
        outputCounts.add(1);
        setEncodedItem(stack, inputOres, inputCounts, outputOres, outputCounts, displayName);
    }

    public void setEncodedItem(ItemStack stack, String inputOreName, String outputOreName, String displayName, int inputCount, int outputCount) {
        List<String> inputOres = new ArrayList<>();
        List<String> outputOres = new ArrayList<>();
        List<Integer> inputCounts = new ArrayList<>();
        List<Integer> outputCounts = new ArrayList<>();
        inputOres.add(inputOreName);
        outputOres.add(outputOreName);
        inputCounts.add(inputCount);
        outputCounts.add(outputCount);
        setEncodedItem(stack, inputOres, inputCounts, outputOres, outputCounts, displayName);
    }

    public void setEncodedItem(ItemStack stack, List<String> inputOreNames, List<Integer> inputCounts,
                               List<String> outputOreNames, List<Integer> outputCounts, String displayName) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound nbt = stack.getTagCompound();
        nbt.setString("EncodedItem", normalizeDisplayName(displayName));

        NBTTagList inputOreList = new NBTTagList();
        NBTTagList inputCountList = new NBTTagList();
        for (int i = 0; i < inputOreNames.size(); i++) {
            inputOreList.appendTag(new NBTTagString(inputOreNames.get(i)));
            int count = i < inputCounts.size() ? inputCounts.get(i) : 1;
            inputCountList.appendTag(new NBTTagInt(count));
        }

        NBTTagList outputOreList = new NBTTagList();
        NBTTagList outputCountList = new NBTTagList();
        for (int i = 0; i < outputOreNames.size(); i++) {
            outputOreList.appendTag(new NBTTagString(outputOreNames.get(i)));
            int count = i < outputCounts.size() ? outputCounts.get(i) : 1;
            outputCountList.appendTag(new NBTTagInt(count));
        }

        nbt.setTag(TAG_INPUT_ORES, inputOreList);
        nbt.setTag(TAG_INPUT_COUNTS, inputCountList);
        nbt.setTag(TAG_OUTPUT_ORES, outputOreList);
        nbt.setTag(TAG_OUTPUT_COUNTS, outputCountList);

        if (!inputOreNames.isEmpty()) {
            nbt.setString("InputOreName", inputOreNames.get(0));
            nbt.setInteger("InputCount", inputCounts.isEmpty() ? 1 : inputCounts.get(0));
        }
        if (!outputOreNames.isEmpty()) {
            nbt.setString("OutputOreName", outputOreNames.get(0));
            nbt.setInteger("OutputCount", outputCounts.isEmpty() ? 1 : outputCounts.get(0));
        }
    }

    /**
     */
    public void setEncodedItem(ItemStack stack, String oreName, String displayName) {
        setEncodedItem(stack, oreName, oreName, displayName);
    }

    /**
     */
    public void clearEncodedItem(ItemStack stack) {
        if (stack.hasTagCompound()) {
            stack.getTagCompound().removeTag("EncodedItem");
            stack.getTagCompound().removeTag("OreName");
            stack.getTagCompound().removeTag("InputOreName");
            stack.getTagCompound().removeTag("OutputOreName");
            stack.getTagCompound().removeTag("InputCount");
            stack.getTagCompound().removeTag("OutputCount");
            stack.getTagCompound().removeTag(TAG_INPUT_ORES);
            stack.getTagCompound().removeTag(TAG_OUTPUT_ORES);
            stack.getTagCompound().removeTag(TAG_INPUT_COUNTS);
            stack.getTagCompound().removeTag(TAG_OUTPUT_COUNTS);
            stack.getTagCompound().removeTag(TAG_INPUT_FLUIDS);
            stack.getTagCompound().removeTag(TAG_INPUT_FLUID_AMOUNTS);
            stack.getTagCompound().removeTag(TAG_OUTPUT_FLUIDS);
            stack.getTagCompound().removeTag(TAG_OUTPUT_FLUID_AMOUNTS);
            stack.getTagCompound().removeTag(TAG_INPUT_GASES);
            stack.getTagCompound().removeTag(TAG_INPUT_GAS_AMOUNTS);
            stack.getTagCompound().removeTag(TAG_OUTPUT_GASES);
            stack.getTagCompound().removeTag(TAG_OUTPUT_GAS_AMOUNTS);
            stack.getTagCompound().removeTag(TAG_INPUT_GAS_ITEMS);
            stack.getTagCompound().removeTag(TAG_OUTPUT_GAS_ITEMS);
            stack.getTagCompound().removeTag(TAG_FILTER_MODE);
            stack.getTagCompound().removeTag(TAG_FILTER_ENTRIES);
            stack.getTagCompound().removeTag(TAG_EXCLUDED_INPUT_MOD_IDS);
            stack.getTagCompound().removeTag(TAG_EXCLUDED_OUTPUT_MOD_IDS);
            stack.getTagCompound().removeTag("EditorInputSlots");
            stack.getTagCompound().removeTag("EditorOutputSlots");
        }
    }

    public int getFilterMode(ItemStack stack) {
        return getFilterModeStatic(stack);
    }

    public void setFilterMode(ItemStack stack, int mode) {
        setFilterModeStatic(stack, mode);
    }

    public List<String> getFilterEntries(ItemStack stack) {
        return getFilterEntriesStatic(stack);
    }

    public static int getFilterModeStatic(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) {
            return FILTER_MODE_BLACKLIST;
        }
        return stack.getTagCompound().getInteger(TAG_FILTER_MODE);
    }

    public static void setFilterModeStatic(ItemStack stack, int mode) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        int value = (mode == FILTER_MODE_BLACKLIST) ? FILTER_MODE_BLACKLIST : FILTER_MODE_WHITELIST;
        stack.getTagCompound().setInteger(TAG_FILTER_MODE, value);
    }

    public static List<String> getFilterEntriesStatic(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) {
            return new ArrayList<>();
        }
        return readStringList(stack.getTagCompound(), TAG_FILTER_ENTRIES, "");
    }

    public static void toggleFilterEntryStatic(ItemStack stack, String entry) {
        if (stack == null || stack.isEmpty() || entry == null || entry.isEmpty()) {
            return;
        }
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound tag = stack.getTagCompound();
        List<String> entries = readStringList(tag, TAG_FILTER_ENTRIES, "");
        if (entries.contains(entry)) {
            entries.remove(entry);
        } else {
            entries.add(entry);
        }
        NBTTagList list = new NBTTagList();
        for (String value : entries) {
            if (value != null && !value.isEmpty()) {
                list.appendTag(new NBTTagString(value));
            }
        }
        if (list.tagCount() > 0) {
            tag.setTag(TAG_FILTER_ENTRIES, list);
        } else {
            tag.removeTag(TAG_FILTER_ENTRIES);
        }
    }

    public static void clearFilterEntriesStatic(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) {
            return;
        }
        stack.getTagCompound().removeTag(TAG_FILTER_ENTRIES);
    }

    public static List<String> getExcludedInputModIdsStatic(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) {
            return new ArrayList<>();
        }
        return readSimpleStringList(stack.getTagCompound(), TAG_EXCLUDED_INPUT_MOD_IDS);
    }

    public static List<String> getExcludedOutputModIdsStatic(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) {
            return new ArrayList<>();
        }
        return readSimpleStringList(stack.getTagCompound(), TAG_EXCLUDED_OUTPUT_MOD_IDS);
    }

    public static void setExcludedModFiltersStatic(ItemStack stack, String[] inputModIds, String[] outputModIds) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound tag = stack.getTagCompound();

        String[] normalizedInput = normalizeAndSortModIds(inputModIds);
        String[] normalizedOutput = normalizeAndSortModIds(outputModIds);

        if (normalizedInput.length > 0) {
            tag.setTag(TAG_EXCLUDED_INPUT_MOD_IDS, writeStringArray(normalizedInput));
        } else {
            tag.removeTag(TAG_EXCLUDED_INPUT_MOD_IDS);
        }
        if (normalizedOutput.length > 0) {
            tag.setTag(TAG_EXCLUDED_OUTPUT_MOD_IDS, writeStringArray(normalizedOutput));
        } else {
            tag.removeTag(TAG_EXCLUDED_OUTPUT_MOD_IDS);
        }
    }

    public static boolean isInputStackAllowedForPattern(ItemStack patternStack, ItemStack candidate) {
        String modId = getStackModId(candidate);
        if (modId.isEmpty()) {
            return true;
        }
        return isInputModAllowedForPattern(patternStack, modId);
    }

    public static boolean isOutputStackAllowedForPattern(ItemStack patternStack, ItemStack candidate) {
        String modId = getStackModId(candidate);
        if (modId.isEmpty()) {
            return true;
        }
        return isOutputModAllowedForPattern(patternStack, modId);
    }

    public static boolean isInputModAllowedForPattern(ItemStack patternStack, String modId) {
        String normalized = normalizeModId(modId);
        if (normalized.isEmpty()) {
            return true;
        }
        Set<String> excluded = new LinkedHashSet<>();
        for (String id : getExcludedInputModIdsStatic(patternStack)) {
            String value = normalizeModId(id);
            if (!value.isEmpty()) {
                excluded.add(value);
            }
        }
        return !excluded.contains(normalized);
    }

    public static boolean isOutputModAllowedForPattern(ItemStack patternStack, String modId) {
        String normalized = normalizeModId(modId);
        if (normalized.isEmpty()) {
            return true;
        }
        Set<String> excluded = new LinkedHashSet<>();
        for (String id : getExcludedOutputModIdsStatic(patternStack)) {
            String value = normalizeModId(id);
            if (!value.isEmpty()) {
                excluded.add(value);
            }
        }
        return !excluded.contains(normalized);
    }

    private static String[] normalizeAndSortModIds(String[] modIds) {
        if (modIds == null || modIds.length == 0) {
            return new String[0];
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String modId : modIds) {
            String value = normalizeModId(modId);
            if (!value.isEmpty()) {
                normalized.add(value);
            }
        }
        String[] values = normalized.toArray(new String[0]);
        Arrays.sort(values);
        return values;
    }

    private static String getStackModId(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() == null) {
            return "";
        }
        ResourceLocation rl = stack.getItem().getRegistryName();
        if (rl == null) {
            return "";
        }
        return normalizeModId(rl.getNamespace());
    }

    private static String normalizeModId(String modId) {
        if (modId == null) {
            return "";
        }
        return modId.trim().toLowerCase(Locale.ROOT);
    }

    private static NBTTagList writeStringArray(String[] values) {
        NBTTagList list = new NBTTagList();
        if (values == null) {
            return list;
        }
        for (String value : values) {
            if (value != null && !value.isEmpty()) {
                list.appendTag(new NBTTagString(value));
            }
        }
        return list;
    }

    private static List<String> readSimpleStringList(NBTTagCompound tag, String listKey) {
        List<String> result = new ArrayList<>();
        if (tag == null || listKey == null || listKey.isEmpty() || !tag.hasKey(listKey)) {
            return result;
        }
        NBTTagList list = tag.getTagList(listKey, 8);
        for (int i = 0; i < list.tagCount(); i++) {
            String value = list.getStringTagAt(i);
            if (value != null && !value.isEmpty()) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     */
    public List<String> getInputFluids(ItemStack stack) {
        if (!hasEncodedItem(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readStringList(stack.getTagCompound(), TAG_INPUT_FLUIDS, "");
    }

    /**
     */
    public List<Integer> getInputFluidAmounts(ItemStack stack) {
        if (!hasEncodedItem(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readIntList(stack.getTagCompound(), TAG_INPUT_FLUID_AMOUNTS, "");
    }

    /**
     */
    public List<String> getOutputFluids(ItemStack stack) {
        if (!hasEncodedItem(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readStringList(stack.getTagCompound(), TAG_OUTPUT_FLUIDS, "");
    }

    /**
     */
    public List<Integer> getOutputFluidAmounts(ItemStack stack) {
        if (!hasEncodedItem(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readIntList(stack.getTagCompound(), TAG_OUTPUT_FLUID_AMOUNTS, "");
    }

    /**
     */
    public List<String> getInputGases(ItemStack stack) {
        if (!hasEncodedItem(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readStringList(stack.getTagCompound(), TAG_INPUT_GASES, "");
    }

    /**
     */
    public List<Integer> getInputGasAmounts(ItemStack stack) {
        if (!hasEncodedItem(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readIntList(stack.getTagCompound(), TAG_INPUT_GAS_AMOUNTS, "");
    }

    /**
     */
    public List<String> getOutputGases(ItemStack stack) {
        if (!hasEncodedItem(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readStringList(stack.getTagCompound(), TAG_OUTPUT_GASES, "");
    }

    /**
     */
    public List<Integer> getOutputGasAmounts(ItemStack stack) {
        if (!hasEncodedItem(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readIntList(stack.getTagCompound(), TAG_OUTPUT_GAS_AMOUNTS, "");
    }

    /**
     */
    public void setEncodedItemWithFluids(ItemStack stack, List<String> inputOreNames, List<Integer> inputCounts,
                                         List<String> outputOreNames, List<Integer> outputCounts,
                                         List<String> inputFluids, List<Integer> inputFluidAmounts,
                                         List<String> outputFluids, List<Integer> outputFluidAmounts,
                                         String displayName) {
        setEncodedItemWithFluidsAndGases(stack, inputOreNames, inputCounts, outputOreNames, outputCounts,
            inputFluids, inputFluidAmounts, outputFluids, outputFluidAmounts,
            new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
            new ArrayList<>(), new ArrayList<>(), displayName);
    }

    /**
     */
    public void setEncodedItemWithFluidsAndGases(ItemStack stack, List<String> inputOreNames, List<Integer> inputCounts,
                                                 List<String> outputOreNames, List<Integer> outputCounts,
                                                 List<String> inputFluids, List<Integer> inputFluidAmounts,
                                                 List<String> outputFluids, List<Integer> outputFluidAmounts,
                                                 List<String> inputGases, List<Integer> inputGasAmounts,
                                                 List<String> outputGases, List<Integer> outputGasAmounts,
                                                 List<ItemStack> inputGasItems, List<ItemStack> outputGasItems,
                                                 String displayName) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound nbt = stack.getTagCompound();
        nbt.setString("EncodedItem", normalizeDisplayName(displayName));


        NBTTagList inputOreList = new NBTTagList();
        NBTTagList inputCountList = new NBTTagList();
        for (int i = 0; i < inputOreNames.size(); i++) {
            inputOreList.appendTag(new NBTTagString(inputOreNames.get(i)));
            int count = i < inputCounts.size() ? inputCounts.get(i) : 1;
            inputCountList.appendTag(new NBTTagInt(count));
        }

        NBTTagList outputOreList = new NBTTagList();
        NBTTagList outputCountList = new NBTTagList();
        for (int i = 0; i < outputOreNames.size(); i++) {
            outputOreList.appendTag(new NBTTagString(outputOreNames.get(i)));
            int count = i < outputCounts.size() ? outputCounts.get(i) : 1;
            outputCountList.appendTag(new NBTTagInt(count));
        }

        nbt.setTag(TAG_INPUT_ORES, inputOreList);
        nbt.setTag(TAG_INPUT_COUNTS, inputCountList);
        nbt.setTag(TAG_OUTPUT_ORES, outputOreList);
        nbt.setTag(TAG_OUTPUT_COUNTS, outputCountList);


        NBTTagList inputFluidList = new NBTTagList();
        NBTTagList inputFluidAmountList = new NBTTagList();
        for (int i = 0; i < inputFluids.size(); i++) {
            inputFluidList.appendTag(new NBTTagString(inputFluids.get(i)));
            int amount = i < inputFluidAmounts.size() ? inputFluidAmounts.get(i) : 0;
            inputFluidAmountList.appendTag(new NBTTagInt(amount));
        }

        NBTTagList outputFluidList = new NBTTagList();
        NBTTagList outputFluidAmountList = new NBTTagList();
        for (int i = 0; i < outputFluids.size(); i++) {
            outputFluidList.appendTag(new NBTTagString(outputFluids.get(i)));
            int amount = i < outputFluidAmounts.size() ? outputFluidAmounts.get(i) : 0;
            outputFluidAmountList.appendTag(new NBTTagInt(amount));
        }

        if (!inputFluids.isEmpty()) {
            nbt.setTag(TAG_INPUT_FLUIDS, inputFluidList);
            nbt.setTag(TAG_INPUT_FLUID_AMOUNTS, inputFluidAmountList);
        }
        if (!outputFluids.isEmpty()) {
            nbt.setTag(TAG_OUTPUT_FLUIDS, outputFluidList);
            nbt.setTag(TAG_OUTPUT_FLUID_AMOUNTS, outputFluidAmountList);
        }


        NBTTagList inputGasList = new NBTTagList();
        NBTTagList inputGasAmountList = new NBTTagList();
        for (int i = 0; i < inputGases.size(); i++) {
            inputGasList.appendTag(new NBTTagString(inputGases.get(i)));
            int amount = i < inputGasAmounts.size() ? inputGasAmounts.get(i) : 0;
            inputGasAmountList.appendTag(new NBTTagInt(amount));
        }

        NBTTagList outputGasList = new NBTTagList();
        NBTTagList outputGasAmountList = new NBTTagList();
        for (int i = 0; i < outputGases.size(); i++) {
            outputGasList.appendTag(new NBTTagString(outputGases.get(i)));
            int amount = i < outputGasAmounts.size() ? outputGasAmounts.get(i) : 0;
            outputGasAmountList.appendTag(new NBTTagInt(amount));
        }

        if (!inputGases.isEmpty()) {
            nbt.setTag(TAG_INPUT_GASES, inputGasList);
            nbt.setTag(TAG_INPUT_GAS_AMOUNTS, inputGasAmountList);
        }
        if (!outputGases.isEmpty()) {
            nbt.setTag(TAG_OUTPUT_GASES, outputGasList);
            nbt.setTag(TAG_OUTPUT_GAS_AMOUNTS, outputGasAmountList);
        }

        if (inputGasItems != null && !inputGasItems.isEmpty()) {
            nbt.setTag(TAG_INPUT_GAS_ITEMS, writeItemStackList(inputGasItems));
        } else if (nbt.hasKey(TAG_INPUT_GAS_ITEMS)) {
            nbt.removeTag(TAG_INPUT_GAS_ITEMS);
        }
        if (outputGasItems != null && !outputGasItems.isEmpty()) {
            nbt.setTag(TAG_OUTPUT_GAS_ITEMS, writeItemStackList(outputGasItems));
        } else if (nbt.hasKey(TAG_OUTPUT_GAS_ITEMS)) {
            nbt.removeTag(TAG_OUTPUT_GAS_ITEMS);
        }


        if (!inputOreNames.isEmpty()) {
            nbt.setString("InputOreName", inputOreNames.get(0));
            nbt.setInteger("InputCount", inputCounts.isEmpty() ? 1 : inputCounts.get(0));
        }
        if (!outputOreNames.isEmpty()) {
            nbt.setString("OutputOreName", outputOreNames.get(0));
            nbt.setInteger("OutputCount", outputCounts.isEmpty() ? 1 : outputCounts.get(0));
        }
    }

    /**
     */
    public static List<String> getInputFluidsStatic(ItemStack stack) {
        if (!hasEncodedItemStatic(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readStringList(stack.getTagCompound(), TAG_INPUT_FLUIDS, "");
    }

    /**
     */
    public static List<Integer> getInputFluidAmountsStatic(ItemStack stack) {
        if (!hasEncodedItemStatic(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readIntList(stack.getTagCompound(), TAG_INPUT_FLUID_AMOUNTS, "");
    }

    /**
     */
    public static List<String> getOutputFluidsStatic(ItemStack stack) {
        if (!hasEncodedItemStatic(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readStringList(stack.getTagCompound(), TAG_OUTPUT_FLUIDS, "");
    }

    /**
     */
    public static List<Integer> getOutputFluidAmountsStatic(ItemStack stack) {
        if (!hasEncodedItemStatic(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readIntList(stack.getTagCompound(), TAG_OUTPUT_FLUID_AMOUNTS, "");
    }

    /**
     */
    public static List<String> getInputGasesStatic(ItemStack stack) {
        if (!hasEncodedItemStatic(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readStringList(stack.getTagCompound(), TAG_INPUT_GASES, "");
    }

    /**
     */
    public static List<Integer> getInputGasAmountsStatic(ItemStack stack) {
        if (!hasEncodedItemStatic(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readIntList(stack.getTagCompound(), TAG_INPUT_GAS_AMOUNTS, "");
    }

    /**
     */
    public static List<String> getOutputGasesStatic(ItemStack stack) {
        if (!hasEncodedItemStatic(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readStringList(stack.getTagCompound(), TAG_OUTPUT_GASES, "");
    }

    /**
     */
    public static List<Integer> getOutputGasAmountsStatic(ItemStack stack) {
        if (!hasEncodedItemStatic(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readIntList(stack.getTagCompound(), TAG_OUTPUT_GAS_AMOUNTS, "");
    }

    public static List<ItemStack> getInputGasItemsStatic(ItemStack stack) {
        if (!hasEncodedItemStatic(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readItemStackList(stack.getTagCompound(), TAG_INPUT_GAS_ITEMS);
    }

    public static List<ItemStack> getOutputGasItemsStatic(ItemStack stack) {
        if (!hasEncodedItemStatic(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readItemStackList(stack.getTagCompound(), TAG_OUTPUT_GAS_ITEMS);
    }


    /**
     */
    public String getInputOreName(ItemStack stack) {
        List<String> names = getInputOreNames(stack);
        return names.isEmpty() ? "" : names.get(0);
    }

    public int getInputCount(ItemStack stack) {
        List<Integer> counts = getInputCounts(stack);
        return counts.isEmpty() ? 1 : counts.get(0);
    }

    public int getOutputCount(ItemStack stack) {
        List<Integer> counts = getOutputCounts(stack);
        return counts.isEmpty() ? 1 : counts.get(0);
    }

    /**
     */
    public String getOutputOreName(ItemStack stack) {
        List<String> names = getOutputOreNames(stack);
        return names.isEmpty() ? "" : names.get(0);
    }

    public List<String> getInputOreNames(ItemStack stack) {
        if (!hasEncodedItem(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readStringList(stack.getTagCompound(), TAG_INPUT_ORES, "InputOreName");
    }

    public List<String> getOutputOreNames(ItemStack stack) {
        if (!hasEncodedItem(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readStringList(stack.getTagCompound(), TAG_OUTPUT_ORES, "OutputOreName");
    }

    public List<Integer> getInputCounts(ItemStack stack) {
        if (!hasEncodedItem(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readIntList(stack.getTagCompound(), TAG_INPUT_COUNTS, "InputCount");
    }

    public List<Integer> getOutputCounts(ItemStack stack) {
        if (!hasEncodedItem(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readIntList(stack.getTagCompound(), TAG_OUTPUT_COUNTS, "OutputCount");
    }

    /**
     */
    public String getOreName(ItemStack stack) {
        if (!hasEncodedItem(stack)) return "";
        if (!stack.hasTagCompound()) return "";


        String inputOreName = stack.getTagCompound().getString("InputOreName");
        if (!inputOreName.isEmpty()) {
            String outputOreName = stack.getTagCompound().getString("OutputOreName");
            return inputOreName + " -> " + outputOreName;
        }

        return stack.getTagCompound().getString("OreName");
    }

    /**
     */
    @Override
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        boolean encoded = hasEncodedItem(stack);
        List<TooltipSlotEntry> inputEntries = readEditorTooltipEntries(stack, true);
        List<TooltipSlotEntry> outputEntries = readEditorTooltipEntries(stack, false);

        String encodedLabel = encoded
            ? resolveEncodedTooltipLabel(stack, inputEntries, outputEntries)
            : new TextComponentTranslation("tooltip.sampleintegration.bool_no").getFormattedText();
        tooltip.add((encoded ? TextFormatting.GREEN : TextFormatting.RED)
            + new TextComponentTranslation("tooltip.sampleintegration.encoded", encodedLabel).getFormattedText());

        int filterMode = getFilterModeStatic(stack);
        String filterModeLabel = new TextComponentTranslation(
            filterMode == FILTER_MODE_WHITELIST
                ? "tooltip.sampleintegration.filter_mode_whitelist"
                : "tooltip.sampleintegration.filter_mode_blacklist"
        ).getFormattedText();
        TextFormatting filterModeColor = filterMode == FILTER_MODE_WHITELIST ? TextFormatting.AQUA : TextFormatting.GOLD;
        tooltip.add(filterModeColor
            + new TextComponentTranslation("tooltip.sampleintegration.filter_mode", filterModeLabel).getFormattedText());

        if (encoded) {
            List<String> inputLines = buildTooltipLines(stack, true, inputEntries);
            List<String> outputLines = buildTooltipLines(stack, false, outputEntries);

            tooltip.add(TextFormatting.GRAY
                + new TextComponentTranslation("tooltip.sampleintegration.input_count", inputLines.size()).getFormattedText());
            tooltip.addAll(inputLines);
            tooltip.add(TextFormatting.GRAY
                + new TextComponentTranslation("tooltip.sampleintegration.output_count", outputLines.size()).getFormattedText());
            tooltip.addAll(outputLines);
        }

        tooltip.add(TextFormatting.YELLOW + new TextComponentTranslation("tooltip.sampleintegration.open_pattern_editor").getFormattedText());
        tooltip.add(TextFormatting.GRAY + new TextComponentTranslation("tooltip.sampleintegration.mark_hint").getFormattedText());
    }

    private String resolveEncodedTooltipLabel(ItemStack stack, List<TooltipSlotEntry> inputEntries, List<TooltipSlotEntry> outputEntries) {
        if (!inputEntries.isEmpty() && !outputEntries.isEmpty()) {
            return getTooltipStackDisplayName(inputEntries.get(0).stack)
                + " -> "
                + getTooltipStackDisplayName(outputEntries.get(0).stack);
        }
        return getEncodedItemName(stack);
    }

    private List<String> buildTooltipLines(ItemStack patternStack, boolean input, List<TooltipSlotEntry> previewEntries) {
        if (!previewEntries.isEmpty()) {
            return buildTooltipPreviewLines(previewEntries, input);
        }
        return buildLegacyTooltipLines(patternStack, input);
    }

    private List<String> buildTooltipPreviewLines(List<TooltipSlotEntry> entries, boolean input) {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            ItemStack entryStack = entries.get(i).stack;
            String key = resolveTooltipLineKey(entryStack, input);
            TextFormatting color = resolveTooltipColor(entryStack, input);
            lines.add(color + new TextComponentTranslation(
                key,
                i + 1,
                getTooltipStackDisplayName(entryStack),
                getLogicalTooltipAmount(entryStack)
            ).getFormattedText());
        }
        return lines;
    }

    private List<String> buildLegacyTooltipLines(ItemStack stack, boolean input) {
        List<String> lines = new ArrayList<>();

        List<String> oreNames = input ? getInputOreNames(stack) : getOutputOreNames(stack);
        List<Integer> oreCounts = input ? getInputCounts(stack) : getOutputCounts(stack);
        List<String> fluids = input ? getInputFluids(stack) : getOutputFluids(stack);
        List<Integer> fluidAmounts = input ? getInputFluidAmounts(stack) : getOutputFluidAmounts(stack);
        List<String> gases = input ? getInputGases(stack) : getOutputGases(stack);
        List<Integer> gasAmounts = input ? getInputGasAmounts(stack) : getOutputGasAmounts(stack);

        String itemKey = input ? "tooltip.sampleintegration.input_item" : "tooltip.sampleintegration.output_item";
        String fluidKey = input ? "tooltip.sampleintegration.input_fluid" : "tooltip.sampleintegration.output_fluid";
        String gasKey = input ? "tooltip.sampleintegration.input_gas" : "tooltip.sampleintegration.output_gas";

        for (int i = 0; i < oreNames.size(); i++) {
            String name = oreNames.get(i);
            int amount = i < oreCounts.size() ? oreCounts.get(i) : 1;
            lines.add(TextFormatting.DARK_GRAY + new TextComponentTranslation(
                itemKey,
                i + 1,
                getLegacyOreDisplayName(name),
                Math.max(1, amount)
            ).getFormattedText());
        }

        for (int i = 0; i < fluids.size(); i++) {
            String name = fluids.get(i);
            int amount = i < fluidAmounts.size() ? fluidAmounts.get(i) : 1;
            lines.add((input ? TextFormatting.DARK_AQUA : TextFormatting.BLUE) + new TextComponentTranslation(
                fluidKey,
                i + 1,
                getLegacyFluidDisplayName(name, amount),
                Math.max(1, amount)
            ).getFormattedText());
        }

        for (int i = 0; i < gases.size(); i++) {
            String name = gases.get(i);
            int amount = i < gasAmounts.size() ? gasAmounts.get(i) : 1;
            lines.add((input ? TextFormatting.DARK_AQUA : TextFormatting.BLUE) + new TextComponentTranslation(
                gasKey,
                i + 1,
                getLegacyGasDisplayName(name),
                Math.max(1, amount)
            ).getFormattedText());
        }

        return lines;
    }

    private String resolveTooltipLineKey(ItemStack stack, boolean input) {
        if (isGasTooltipStack(stack)) {
            return input ? "tooltip.sampleintegration.input_gas" : "tooltip.sampleintegration.output_gas";
        }
        if (isFluidTooltipStack(stack)) {
            return input ? "tooltip.sampleintegration.input_fluid" : "tooltip.sampleintegration.output_fluid";
        }
        return input ? "tooltip.sampleintegration.input_item" : "tooltip.sampleintegration.output_item";
    }

    private TextFormatting resolveTooltipColor(ItemStack stack, boolean input) {
        if (isFluidTooltipStack(stack) || isGasTooltipStack(stack)) {
            return input ? TextFormatting.DARK_AQUA : TextFormatting.BLUE;
        }
        return TextFormatting.DARK_GRAY;
    }

    private String getTooltipStackDisplayName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        if (stack.getDisplayName() != null && !stack.getDisplayName().isEmpty()) {
            return stack.getDisplayName();
        }
        if (isGasTooltipStack(stack) && stack.hasTagCompound()) {
            return getLegacyGasDisplayName(stack.getTagCompound().getString("sampleintegrationGasName"));
        }
        return stack.getDisplayName();
    }

    private int getLogicalTooltipAmount(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 1;
        }
        if (stack.hasTagCompound()) {
            NBTTagCompound tag = stack.getTagCompound();
            if (tag.hasKey("sampleintegrationFluidAmount")) {
                return Math.max(1, tag.getInteger("sampleintegrationFluidAmount"));
            }
            if (tag.hasKey("sampleintegrationGasAmount")) {
                return Math.max(1, tag.getInteger("sampleintegrationGasAmount"));
            }
            if (tag.hasKey(TAG_ITEM_AMOUNT)) {
                return Math.max(1, tag.getInteger(TAG_ITEM_AMOUNT));
            }
        }
        return Math.max(1, stack.getCount());
    }

    private boolean isFluidTooltipStack(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.hasTagCompound() && stack.getTagCompound().hasKey("sampleintegrationFluidAmount");
    }

    private boolean isGasTooltipStack(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.hasTagCompound()
            && (stack.getTagCompound().hasKey("sampleintegrationGasAmount")
                || stack.getTagCompound().hasKey("sampleintegrationGasName")
                || stack.getTagCompound().getBoolean("sampleintegrationGasMarker"));
    }

    private String getLegacyOreDisplayName(String oreName) {
        if (oreName == null || oreName.isEmpty()) {
            return "";
        }
        if (oreName.contains("*")) {
            return oreName;
        }
        List<ItemStack> stacks = OreDictionary.getOres(oreName);
        if (stacks != null) {
            for (ItemStack candidate : stacks) {
                if (candidate != null && !candidate.isEmpty()) {
                    return candidate.getDisplayName();
                }
            }
        }
        return oreName;
    }

    private String getLegacyFluidDisplayName(String fluidName, int amount) {
        if (fluidName == null || fluidName.isEmpty()) {
            return "";
        }
        net.minecraftforge.fluids.Fluid fluid = FluidRegistry.getFluid(fluidName);
        if (fluid == null) {
            return fluidName;
        }
        return fluid.getLocalizedName(new FluidStack(fluid, Math.max(1, amount)));
    }

    private String getLegacyGasDisplayName(String gasName) {
        if (gasName == null || gasName.isEmpty()) {
            return "";
        }
        return gasName;
    }

    private List<TooltipSlotEntry> readEditorTooltipEntries(ItemStack stack, boolean input) {
        List<TooltipSlotEntry> entries = new ArrayList<>();
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) {
            return entries;
        }

        String listKey = input ? TAG_EDITOR_INPUT_SLOTS : TAG_EDITOR_OUTPUT_SLOTS;
        NBTTagCompound tag = stack.getTagCompound();
        if (!tag.hasKey(listKey, 9)) {
            return entries;
        }

        NBTTagList list = tag.getTagList(listKey, 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            if (!entry.hasKey(TAG_EDITOR_SLOT) || !entry.hasKey(TAG_EDITOR_STACK, 10)) {
                continue;
            }
            ItemStack entryStack = new ItemStack(entry.getCompoundTag(TAG_EDITOR_STACK));
            if (entryStack.isEmpty()) {
                continue;
            }
            entries.add(new TooltipSlotEntry(entry.getInteger(TAG_EDITOR_SLOT), entryStack));
        }
        entries.sort((left, right) -> Integer.compare(left.slot, right.slot));
        return entries;
    }

    private static final class TooltipSlotEntry {
        private final int slot;
        private final ItemStack stack;

        private TooltipSlotEntry(int slot, ItemStack stack) {
            this.slot = slot;
            this.stack = stack;
        }
    }

    /**
     */
    public static String getInputOreNameStatic(ItemStack stack) {
        List<String> names = getInputOreNamesStatic(stack);
        return names.isEmpty() ? "" : names.get(0);
    }

    /**
     */
    public static String getOutputOreNameStatic(ItemStack stack) {
        List<String> names = getOutputOreNamesStatic(stack);
        return names.isEmpty() ? "" : names.get(0);
    }

    /**
     */
    public static String getOreNameStatic(ItemStack stack) {
        if (!hasEncodedItemStatic(stack)) return "";
        if (!stack.hasTagCompound()) return "";
        if (stack.getTagCompound().hasKey("VirtualInputOreName") && stack.getTagCompound().hasKey("VirtualOutputOreName")) {
            String inputOreName = stack.getTagCompound().getString("VirtualInputOreName");
            String outputOreName = stack.getTagCompound().getString("VirtualOutputOreName");
            return inputOreName + " -> " + outputOreName;
        }

        String inputOreName = stack.getTagCompound().getString("InputOreName");
        if (!inputOreName.isEmpty()) {
            String outputOreName = stack.getTagCompound().getString("OutputOreName");
            return inputOreName + " -> " + outputOreName;
        }

        return stack.getTagCompound().getString("OreName");
    }

    /**
     */
    public static String getEncodedItemNameStatic(ItemStack stack) {
        if (!hasEncodedItemStatic(stack)) return "";
        NBTTagCompound tag = stack.getTagCompound();
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("VirtualDisplayName")) {
            String raw = tag.getString("VirtualDisplayName");
            String normalized = normalizeDisplayName(raw);
            if (!raw.equals(normalized)) {
                tag.setString("VirtualDisplayName", normalized);
            }
            return normalized;
        }
        String raw = tag.getString("EncodedItem");
        String normalized = normalizeDisplayName(raw);
        if (!raw.equals(normalized)) {
            tag.setString("EncodedItem", normalized);
        }
        return normalized;
    }

    /**
     */
    public static boolean hasEncodedItemStatic(ItemStack stack) {
        if (!stack.hasTagCompound()) return false;
        return stack.getTagCompound().hasKey("EncodedItem");
    }

    /**
     */
    public static int getInputCountStatic(ItemStack stack) {
        List<Integer> counts = getInputCountsStatic(stack);
        return counts.isEmpty() ? 1 : counts.get(0);
    }

    /**
     */
    public static int getOutputCountStatic(ItemStack stack) {
        List<Integer> counts = getOutputCountsStatic(stack);
        return counts.isEmpty() ? 1 : counts.get(0);
    }

    public static List<String> getInputOreNamesStatic(ItemStack stack) {
        if (!hasEncodedItemStatic(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        NBTTagCompound tag = stack.getTagCompound();
        if (tag.hasKey("VirtualInputOreNames")) {
            return readStringList(tag, "VirtualInputOreNames", "VirtualInputOreName");
        }
        if (tag.hasKey("VirtualInputOreName")) {
            List<String> result = new ArrayList<>();
            result.add(tag.getString("VirtualInputOreName"));
            return result;
        }
        return readStringList(tag, TAG_INPUT_ORES, "InputOreName");
    }

    public static List<String> getOutputOreNamesStatic(ItemStack stack) {
        if (!hasEncodedItemStatic(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        NBTTagCompound tag = stack.getTagCompound();
        if (tag.hasKey("VirtualOutputOreNames")) {
            return readStringList(tag, "VirtualOutputOreNames", "VirtualOutputOreName");
        }
        if (tag.hasKey("VirtualOutputOreName")) {
            List<String> result = new ArrayList<>();
            result.add(tag.getString("VirtualOutputOreName"));
            return result;
        }
        return readStringList(tag, TAG_OUTPUT_ORES, "OutputOreName");
    }

    public static List<Integer> getInputCountsStatic(ItemStack stack) {
        if (!hasEncodedItemStatic(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        NBTTagCompound tag = stack.getTagCompound();
        return readIntList(tag, TAG_INPUT_COUNTS, "InputCount");
    }

    public static List<Integer> getOutputCountsStatic(ItemStack stack) {
        if (!hasEncodedItemStatic(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        NBTTagCompound tag = stack.getTagCompound();
        return readIntList(tag, TAG_OUTPUT_COUNTS, "OutputCount");
    }

    private static List<String> readStringList(NBTTagCompound tag, String listKey, String fallbackKey) {
        List<String> result = new ArrayList<>();
        if (tag.hasKey(listKey)) {
            NBTTagList list = tag.getTagList(listKey, 8);
            for (int i = 0; i < list.tagCount(); i++) {
                result.add(list.getStringTagAt(i));
            }
            return result;
        }
        if (tag.hasKey(fallbackKey)) {
            result.add(tag.getString(fallbackKey));
        } else if (tag.hasKey("OreName")) {
            result.add(tag.getString("OreName"));
        }
        return result;
    }

    private static List<Integer> readIntList(NBTTagCompound tag, String listKey, String fallbackKey) {
        List<Integer> result = new ArrayList<>();
        if (tag.hasKey(listKey)) {
            NBTTagList list = tag.getTagList(listKey, 3);
            for (int i = 0; i < list.tagCount(); i++) {
                result.add(((NBTTagInt) list.get(i)).getInt());
            }
            return result;
        }
        if (tag.hasKey(fallbackKey)) {
            result.add(tag.getInteger(fallbackKey));
        }
        return result;
    }

    private static NBTTagList writeItemStackList(List<ItemStack> stacks) {
        NBTTagList list = new NBTTagList();
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            NBTTagCompound stackTag = new NBTTagCompound();
            stack.writeToNBT(stackTag);
            list.appendTag(stackTag);
        }
        return list;
    }

    private static List<ItemStack> readItemStackList(NBTTagCompound tag, String listKey) {
        List<ItemStack> result = new ArrayList<>();
        if (!tag.hasKey(listKey)) {
            return result;
        }
        NBTTagList list = tag.getTagList(listKey, 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound stackTag = list.getCompoundTagAt(i);
            ItemStack stack = new ItemStack(stackTag);
            if (!stack.isEmpty()) {
                result.add(stack);
            }
        }
        return result;
    }

    /**
     */
    @Override
    public appeng.api.networking.crafting.ICraftingPatternDetails getPatternForItem(ItemStack stack, net.minecraft.world.World world) {
        if (hasEncodedItem(stack)) {

            List<String> inputOres = getInputOreNamesStatic(stack);
            List<String> outputOres = getOutputOreNamesStatic(stack);
            

            if (stack.hasTagCompound() &&
                (stack.getTagCompound().hasKey("VirtualInputOreNames") ||
                 stack.getTagCompound().hasKey("VirtualInputOreName"))) {
                return new SmartPatternDetails(stack);
            }
            


            boolean hasWildcard = false;
            for (String ore : inputOres) {
                if (ore.contains("*")) {
                    hasWildcard = true;
                    break;
                }
            }
            if (!hasWildcard) {
                for (String ore : outputOres) {
                    if (ore.contains("*")) {
                        hasWildcard = true;
                        break;
                    }
                }
            }

            if (hasWildcard) {

                return new SmartPatternDetails(stack);
            }
            

            return new SmartPatternDetails(stack);
        }
        return null;
    }
}



