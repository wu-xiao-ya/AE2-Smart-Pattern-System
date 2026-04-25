package com.wuxiaoya.techstart.core.validation;

import com.wuxiaoya.techstart.core.model.EntryKind;
import com.wuxiaoya.techstart.core.model.FilterEntry;
import com.wuxiaoya.techstart.core.model.PatternDefinition;
import com.wuxiaoya.techstart.core.model.PatternEntry;

import java.util.LinkedHashSet;
import java.util.Set;

public final class PatternValidators {
    private PatternValidators() {
    }

    public static void validate(PatternDefinition definition) {
        if (definition.inputs().isEmpty()) {
            throw new IllegalArgumentException("PatternDefinition must contain at least one input.");
        }
        if (definition.outputs().isEmpty()) {
            throw new IllegalArgumentException("PatternDefinition must contain at least one output.");
        }

        Set<String> duplicateFilterIds = new LinkedHashSet<>();
        Set<String> seenFilterIds = new LinkedHashSet<>();
        for (FilterEntry entry : definition.filterEntries()) {
            String id = entry.asSerializedId();
            if (!seenFilterIds.add(id)) {
                duplicateFilterIds.add(id);
            }
        }
        if (!duplicateFilterIds.isEmpty()) {
            throw new IllegalArgumentException("Duplicate filter entries: " + duplicateFilterIds);
        }

        for (PatternEntry entry : definition.entries()) {
            if (entry.key().isBlank()) {
                throw new IllegalArgumentException("PatternEntry key must not be blank for slot " + entry.slot());
            }
            if (entry.kind() == EntryKind.ITEM && entry.descriptor().isBlank()) {
                throw new IllegalArgumentException("Item PatternEntry descriptor must not be blank for slot " + entry.slot());
            }
        }
    }
}
