package com.wuxiaoya.techstart.core.model;

import java.util.Objects;

public final class FilterEntry {
    private final String inputDescriptor;
    private final String outputDescriptor;

    public FilterEntry(String inputDescriptor, String outputDescriptor) {
        this.inputDescriptor = normalize(inputDescriptor);
        this.outputDescriptor = normalize(outputDescriptor);
    }

    public String inputDescriptor() {
        return inputDescriptor;
    }

    public String outputDescriptor() {
        return outputDescriptor;
    }

    public String asSerializedId() {
        return inputDescriptor + "->" + outputDescriptor;
    }

    public static FilterEntry fromSerializedId(String value) {
        String normalized = normalize(value);
        int split = normalized.indexOf("->");
        if (split < 0) {
            return new FilterEntry(normalized, "");
        }
        return new FilterEntry(normalized.substring(0, split), normalized.substring(split + 2));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof FilterEntry other)) {
            return false;
        }
        return inputDescriptor.equals(other.inputDescriptor) && outputDescriptor.equals(other.outputDescriptor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inputDescriptor, outputDescriptor);
    }

    @Override
    public String toString() {
        return asSerializedId();
    }
}

