package com.wuxiaoya.techstart.core.model;

import java.util.Objects;

public final class PatternEntry {
    private final int slot;
    private final PatternSide side;
    private final EntryKind kind;
    private final String key;
    private final long amount;
    private final String displayName;
    private final String descriptor;

    public PatternEntry(int slot, PatternSide side, EntryKind kind, String key, long amount, String displayName, String descriptor) {
        this.slot = slot;
        this.side = Objects.requireNonNull(side, "side");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.key = normalize(key);
        this.amount = Math.max(1L, amount);
        this.displayName = normalize(displayName);
        this.descriptor = normalize(descriptor);
    }

    public int slot() {
        return slot;
    }

    public PatternSide side() {
        return side;
    }

    public EntryKind kind() {
        return kind;
    }

    public String key() {
        return key;
    }

    public long amount() {
        return amount;
    }

    public String displayName() {
        return displayName;
    }

    public String descriptor() {
        return descriptor;
    }

    public boolean isInput() {
        return side == PatternSide.INPUT;
    }

    public boolean isOutput() {
        return side == PatternSide.OUTPUT;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}

