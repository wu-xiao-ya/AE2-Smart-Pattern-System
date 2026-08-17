package com.ae2smartpatternsystem.core.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public final class ModFilterRule {
    private static final Pattern MOD_ID_PATTERN = Pattern.compile("[a-z0-9_.-]+");

    private final FilterMode mode;
    private final List<String> modIds;

    private ModFilterRule(FilterMode mode, Collection<String> modIds) {
        this.mode = Objects.requireNonNull(mode, "mode");

        Set<String> normalizedIds = new LinkedHashSet<>();
        if (modIds != null) {
            for (String modId : modIds) {
                String normalized = normalizeModId(modId);
                if (!normalized.isEmpty()) {
                    normalizedIds.add(normalized);
                }
            }
        }
        this.modIds = Collections.unmodifiableList(new ArrayList<>(normalizedIds));
    }

    public static ModFilterRule of(FilterMode mode, Collection<String> modIds) {
        return new ModFilterRule(mode, modIds);
    }

    public static ModFilterRule blacklist(Collection<String> modIds) {
        return of(FilterMode.BLACKLIST, modIds);
    }

    public static ModFilterRule fromStoredData(
            Integer serializedMode,
            boolean canonicalIdsPresent,
            Collection<String> canonicalIds,
            Collection<String> legacyExcludedIds) {
        FilterMode mode = serializedMode == null
                ? FilterMode.BLACKLIST
                : FilterMode.fromSerializedValue(serializedMode);
        Collection<String> ids = canonicalIdsPresent ? canonicalIds : legacyExcludedIds;
        return of(mode, ids);
    }

    public FilterMode mode() {
        return mode;
    }

    public List<String> modIds() {
        return modIds;
    }

    public boolean allows(String namespace) {
        String normalized = normalizeModId(namespace);
        if (normalized.isEmpty()) {
            return mode == FilterMode.BLACKLIST;
        }

        boolean listed = modIds.contains(normalized);
        return mode == FilterMode.BLACKLIST ? !listed : listed;
    }

    public static String normalizeModId(String modId) {
        if (modId == null) {
            return "";
        }

        String normalized = modId.trim().toLowerCase(Locale.ROOT);
        return MOD_ID_PATTERN.matcher(normalized).matches() ? normalized : "";
    }
}
