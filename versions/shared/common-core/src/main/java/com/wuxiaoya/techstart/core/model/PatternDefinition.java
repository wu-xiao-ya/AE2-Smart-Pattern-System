package com.wuxiaoya.techstart.core.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class PatternDefinition {
    private final boolean encoded;
    private final String encodedItemLabel;
    private final FilterMode filterMode;
    private final List<PatternEntry> entries;
    private final Set<FilterEntry> filterEntries;

    public PatternDefinition(
            boolean encoded,
            String encodedItemLabel,
            FilterMode filterMode,
            Collection<PatternEntry> entries,
            Collection<FilterEntry> filterEntries) {
        this.encoded = encoded;
        this.encodedItemLabel = encodedItemLabel == null ? "" : encodedItemLabel.trim();
        this.filterMode = Objects.requireNonNull(filterMode, "filterMode");
        this.entries = Collections.unmodifiableList(new ArrayList<>(entries == null ? List.of() : entries));
        this.filterEntries = Collections.unmodifiableSet(new LinkedHashSet<>(filterEntries == null ? Set.of() : filterEntries));
    }

    public boolean encoded() {
        return encoded;
    }

    public String encodedItemLabel() {
        return encodedItemLabel;
    }

    public FilterMode filterMode() {
        return filterMode;
    }

    public List<PatternEntry> entries() {
        return entries;
    }

    public Set<FilterEntry> filterEntries() {
        return filterEntries;
    }

    public List<PatternEntry> inputs() {
        return entries.stream().filter(PatternEntry::isInput).toList();
    }

    public List<PatternEntry> outputs() {
        return entries.stream().filter(PatternEntry::isOutput).toList();
    }
}

