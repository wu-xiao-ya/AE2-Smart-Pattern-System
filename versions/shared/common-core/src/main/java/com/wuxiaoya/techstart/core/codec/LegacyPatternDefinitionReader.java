package com.wuxiaoya.techstart.core.codec;

import com.wuxiaoya.techstart.core.codec.tag.TagObject;
import com.wuxiaoya.techstart.core.model.EntryKind;
import com.wuxiaoya.techstart.core.model.FilterMode;
import com.wuxiaoya.techstart.core.model.PatternDefinition;
import com.wuxiaoya.techstart.core.model.PatternEntry;
import com.wuxiaoya.techstart.core.model.PatternSide;

import java.util.ArrayList;
import java.util.List;

public final class LegacyPatternDefinitionReader implements PatternDefinitionReader {
    private static final String TAG_INPUT_ORES = "InputOreNames";
    private static final String TAG_OUTPUT_ORES = "OutputOreNames";
    private static final String TAG_INPUT_COUNTS = "InputCounts";
    private static final String TAG_OUTPUT_COUNTS = "OutputCounts";
    private static final String TAG_VIRTUAL_INPUT_ORES = "VirtualInputOreNames";
    private static final String TAG_VIRTUAL_OUTPUT_ORES = "VirtualOutputOreNames";
    private static final String TAG_VIRTUAL_INPUT_ORE = "VirtualInputOreName";
    private static final String TAG_VIRTUAL_OUTPUT_ORE = "VirtualOutputOreName";
    private static final String TAG_INPUT_ORE = "InputOreName";
    private static final String TAG_OUTPUT_ORE = "OutputOreName";

    @Override
    public PatternDefinition read(TagObject root) {
        boolean encoded = root.has(PatternNbtKeys.TAG_ENCODED_ITEM) || root.has(TAG_INPUT_ORE) || root.has(TAG_OUTPUT_ORE)
                || root.has(TAG_INPUT_ORES) || root.has(TAG_OUTPUT_ORES);
        String encodedItem = root.getString(PatternNbtKeys.TAG_ENCODED_ITEM);

        List<PatternEntry> entries = new ArrayList<>();
        appendNamedEntries(entries, PatternSide.INPUT,
                readLegacyOreNames(root, true),
                readLegacyCounts(root, true),
                EntryKind.ITEM);
        appendNamedEntries(entries, PatternSide.OUTPUT,
                readLegacyOreNames(root, false),
                readLegacyCounts(root, false),
                EntryKind.ITEM);
        appendNamedEntries(entries, PatternSide.INPUT,
                root.getStringList(PatternNbtKeys.TAG_INPUT_FLUIDS),
                root.getIntList(PatternNbtKeys.TAG_INPUT_FLUID_AMOUNTS),
                EntryKind.FLUID);
        appendNamedEntries(entries, PatternSide.OUTPUT,
                root.getStringList(PatternNbtKeys.TAG_OUTPUT_FLUIDS),
                root.getIntList(PatternNbtKeys.TAG_OUTPUT_FLUID_AMOUNTS),
                EntryKind.FLUID);
        appendNamedEntries(entries, PatternSide.INPUT,
                root.getStringList(PatternNbtKeys.TAG_INPUT_GASES),
                root.getIntList(PatternNbtKeys.TAG_INPUT_GAS_AMOUNTS),
                EntryKind.GAS);
        appendNamedEntries(entries, PatternSide.OUTPUT,
                root.getStringList(PatternNbtKeys.TAG_OUTPUT_GASES),
                root.getIntList(PatternNbtKeys.TAG_OUTPUT_GAS_AMOUNTS),
                EntryKind.GAS);

        return new PatternDefinition(encoded, encodedItem, FilterMode.BLACKLIST, entries, List.of());
    }

    private List<String> readLegacyOreNames(TagObject root, boolean input) {
        if (input) {
            List<String> virtual = root.getStringList(TAG_VIRTUAL_INPUT_ORES);
            if (!virtual.isEmpty()) {
                return virtual;
            }
            String singleVirtual = root.getString(TAG_VIRTUAL_INPUT_ORE);
            if (!singleVirtual.isBlank()) {
                return List.of(singleVirtual);
            }
            List<String> names = root.getStringList(TAG_INPUT_ORES);
            if (!names.isEmpty()) {
                return names;
            }
            String single = root.getString(TAG_INPUT_ORE);
            return single.isBlank() ? List.of() : List.of(single);
        }

        List<String> virtual = root.getStringList(TAG_VIRTUAL_OUTPUT_ORES);
        if (!virtual.isEmpty()) {
            return virtual;
        }
        String singleVirtual = root.getString(TAG_VIRTUAL_OUTPUT_ORE);
        if (!singleVirtual.isBlank()) {
            return List.of(singleVirtual);
        }
        List<String> names = root.getStringList(TAG_OUTPUT_ORES);
        if (!names.isEmpty()) {
            return names;
        }
        String single = root.getString(TAG_OUTPUT_ORE);
        return single.isBlank() ? List.of() : List.of(single);
    }

    private List<Integer> readLegacyCounts(TagObject root, boolean input) {
        List<Integer> counts = input ? root.getIntList(TAG_INPUT_COUNTS) : root.getIntList(TAG_OUTPUT_COUNTS);
        if (!counts.isEmpty()) {
            return counts;
        }
        String fallbackKey = input ? "InputCount" : "OutputCount";
        return root.has(fallbackKey) ? List.of(root.getInt(fallbackKey, 1)) : List.of();
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

