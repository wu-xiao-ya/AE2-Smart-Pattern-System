package com.wuxiaoya.techstart.core.service;

import com.wuxiaoya.techstart.core.model.FilterMode;
import com.wuxiaoya.techstart.core.model.PatternDefinition;
import com.wuxiaoya.techstart.core.model.PatternEntry;

import java.util.ArrayList;
import java.util.List;

public final class PatternTooltipPayloadBuilder {
    private PatternTooltipPayloadBuilder() {
    }

    public static List<String> buildLines(PatternDefinition definition) {
        List<String> lines = new ArrayList<>();
        lines.add("encoded=" + (definition.encoded() ? yesLabel(definition) : "no"));
        lines.add("filterMode=" + formatFilterMode(definition.filterMode()));

        List<PatternEntry> inputs = definition.inputs();
        List<PatternEntry> outputs = definition.outputs();
        lines.add("inputCount=" + inputs.size());
        appendEntries(lines, inputs, "input");
        lines.add("outputCount=" + outputs.size());
        appendEntries(lines, outputs, "output");

        if (!definition.filterEntries().isEmpty()) {
            lines.add("filterEntries=" + definition.filterEntries().size());
            definition.filterEntries().forEach(entry -> lines.add("filter:" + entry.asSerializedId()));
        }
        return lines;
    }

    private static String yesLabel(PatternDefinition definition) {
        String label = definition.encodedItemLabel();
        return label.isBlank() ? "yes" : label;
    }

    private static String formatFilterMode(FilterMode filterMode) {
        return filterMode == FilterMode.WHITELIST ? "whitelist" : "blacklist";
    }

    private static void appendEntries(List<String> lines, List<PatternEntry> entries, String prefix) {
        for (PatternEntry entry : entries) {
            String name = entry.displayName().isBlank() ? entry.key() : entry.displayName();
            lines.add(prefix + ":slot=" + entry.slot()
                    + ",kind=" + entry.kind().name().toLowerCase()
                    + ",name=" + name
                    + ",amount=" + entry.amount());
        }
    }
}

