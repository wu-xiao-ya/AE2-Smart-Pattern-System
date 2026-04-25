package com.wuxiaoya.techstart.core.model;

public enum FilterMode {
    WHITELIST(0),
    BLACKLIST(1);

    private final int serializedValue;

    FilterMode(int serializedValue) {
        this.serializedValue = serializedValue;
    }

    public int serializedValue() {
        return serializedValue;
    }

    public static FilterMode fromSerializedValue(int value) {
        return value == 0 ? WHITELIST : BLACKLIST;
    }
}

