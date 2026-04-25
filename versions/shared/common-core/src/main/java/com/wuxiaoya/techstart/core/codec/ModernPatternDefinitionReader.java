package com.wuxiaoya.techstart.core.codec;

import com.wuxiaoya.techstart.core.codec.tag.TagObject;
import com.wuxiaoya.techstart.core.model.EntryKind;
import com.wuxiaoya.techstart.core.model.FilterEntry;
import com.wuxiaoya.techstart.core.model.FilterMode;
import com.wuxiaoya.techstart.core.model.PatternDefinition;
import com.wuxiaoya.techstart.core.model.PatternEntry;
import com.wuxiaoya.techstart.core.model.PatternSide;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ModernPatternDefinitionReader implements PatternDefinitionReader {
    @Override
    public PatternDefinition read(TagObject root) {
        boolean encoded = root.getBoolean(PatternNbtKeys.TAG_ENCODED, false);
        String encodedItem = root.getString(PatternNbtKeys.TAG_ENCODED_ITEM);
        FilterMode filterMode = FilterMode.fromSerializedValue(readFilterMode(root));

        List<PatternEntry> entries = new ArrayList<>();
        entries.addAll(readSlots(root.getObjectList(PatternNbtKeys.TAG_INPUTS), PatternSide.INPUT));
        entries.addAll(readSlots(root.getObjectList(PatternNbtKeys.TAG_OUTPUTS), PatternSide.OUTPUT));

        if (entries.stream().noneMatch(entry -> entry.kind() == EntryKind.FLUID)) {
            appendNamedEntries(entries, PatternSide.INPUT,
                    root.getStringList(PatternNbtKeys.TAG_INPUT_FLUIDS),
                    root.getIntList(PatternNbtKeys.TAG_INPUT_FLUID_AMOUNTS),
                    EntryKind.FLUID);
            appendNamedEntries(entries, PatternSide.OUTPUT,
                    root.getStringList(PatternNbtKeys.TAG_OUTPUT_FLUIDS),
                    root.getIntList(PatternNbtKeys.TAG_OUTPUT_FLUID_AMOUNTS),
                    EntryKind.FLUID);
        }
        if (entries.stream().noneMatch(entry -> entry.kind() == EntryKind.GAS)) {
            appendNamedEntries(entries, PatternSide.INPUT,
                    root.getStringList(PatternNbtKeys.TAG_INPUT_GASES),
                    root.getIntList(PatternNbtKeys.TAG_INPUT_GAS_AMOUNTS),
                    EntryKind.GAS);
            appendNamedEntries(entries, PatternSide.OUTPUT,
                    root.getStringList(PatternNbtKeys.TAG_OUTPUT_GASES),
                    root.getIntList(PatternNbtKeys.TAG_OUTPUT_GAS_AMOUNTS),
                    EntryKind.GAS);
        }

        Set<FilterEntry> filterEntries = new LinkedHashSet<>();
        for (String raw : root.getStringList(PatternNbtKeys.TAG_FILTER_ENTRIES)) {
            if (raw != null && !raw.isBlank()) {
                filterEntries.add(FilterEntry.fromSerializedId(raw));
            }
        }

        return new PatternDefinition(encoded, encodedItem, filterMode, entries, filterEntries);
    }

    private int readFilterMode(TagObject root) {
        if (root.has(PatternNbtKeys.TAG_FILTER_MODE)) {
            return root.getInt(PatternNbtKeys.TAG_FILTER_MODE, 1);
        }
        if (root.has(PatternNbtKeys.TAG_FILTER_MODE_LEGACY)) {
            return root.getInt(PatternNbtKeys.TAG_FILTER_MODE_LEGACY, 1);
        }
        return 1;
    }

    private List<PatternEntry> readSlots(List<TagObject> serializedEntries, PatternSide side) {
        List<PatternEntry> result = new ArrayList<>();
        for (TagObject entry : serializedEntries) {
            int slot = entry.getInt(PatternNbtKeys.TAG_SLOT, -1);
            TagObject stack = entry.getObject(PatternNbtKeys.TAG_STACK);
            if (slot < 0 || stack == null) {
                continue;
            }

            TagObject stackTag = stack.getObject("tag");
            if (stackTag.getBoolean(PatternNbtKeys.TAG_FLUID_MARKER, false)) {
                String fluidName = stackTag.getString(PatternNbtKeys.TAG_FLUID_NAME);
                long amount = stackTag.getInt(PatternNbtKeys.TAG_FLUID_AMOUNT, 1);
                if (!fluidName.isBlank()) {
                    result.add(new PatternEntry(slot, side, EntryKind.FLUID, fluidName, amount, fluidName, fluidName));
                }
                continue;
            }
            if (stackTag.getBoolean(PatternNbtKeys.TAG_GAS_MARKER, false)) {
                String gasName = stackTag.getString(PatternNbtKeys.TAG_GAS_NAME);
                long amount = stackTag.getInt(PatternNbtKeys.TAG_GAS_AMOUNT, 1);
                if (!gasName.isBlank()) {
                    result.add(new PatternEntry(slot, side, EntryKind.GAS, gasName, amount, gasName, gasName));
                }
                continue;
            }

            String itemId = stack.getString("id");
            long amount = resolveItemAmount(stack, stackTag);
            String descriptor = itemId.isBlank() ? stack.getString("Name") : itemId;
            if (!descriptor.isBlank()) {
                result.add(new PatternEntry(slot, side, EntryKind.ITEM, descriptor, amount, descriptor, descriptor));
            }
        }
        return result;
    }

    private long resolveItemAmount(TagObject stack, TagObject stackTag) {
        if (stackTag.getBoolean(PatternNbtKeys.TAG_ITEM_MARKER, false)) {
            return Math.max(1, stackTag.getInt(PatternNbtKeys.TAG_ITEM_AMOUNT, 1));
        }
        return Math.max(1, stack.getInt("Count", 1));
    }

    private void appendNamedEntries(List<PatternEntry> target, PatternSide side, List<String> names, List<Integer> amounts, EntryKind kind) {
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            if (name == null || name.isBlank()) {
                continue;
            }
            long amount = i < amounts.size() ? Math.max(1, amounts.get(i)) : 1;
            target.add(new PatternEntry(i, side, kind, name, amount, name, name));
        }
    }
}