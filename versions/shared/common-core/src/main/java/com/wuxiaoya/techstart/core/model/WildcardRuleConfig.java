package com.wuxiaoya.techstart.core.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class WildcardRuleConfig {
    private static final List<String> DEFAULT_PREFIXES = List.of(
            "ingot",
            "plate",
            "block",
            "nugget",
            "rod",
            "gear",
            "wire",
            "dust"
    );

    private final List<String> supportedPrefixes;
    private final Set<String> customOreNames;
    private final Set<String> explicitPairs;

    public WildcardRuleConfig(Collection<String> supportedPrefixes, Collection<String> customOreNames, Collection<String> explicitPairs) {
        LinkedHashSet<String> normalizedPrefixes = new LinkedHashSet<>();
        normalizedPrefixes.addAll(DEFAULT_PREFIXES);
        addNormalized(normalizedPrefixes, supportedPrefixes);

        LinkedHashSet<String> normalizedOreNames = new LinkedHashSet<>();
        addNormalized(normalizedOreNames, customOreNames);

        LinkedHashSet<String> normalizedPairs = new LinkedHashSet<>();
        addNormalized(normalizedPairs, explicitPairs);

        this.supportedPrefixes = Collections.unmodifiableList(new ArrayList<>(normalizedPrefixes));
        this.customOreNames = Collections.unmodifiableSet(normalizedOreNames);
        this.explicitPairs = Collections.unmodifiableSet(normalizedPairs);
    }

    public static WildcardRuleConfig defaults() {
        return new WildcardRuleConfig(List.of(), List.of(), List.of());
    }

    public List<String> supportedPrefixes() {
        return supportedPrefixes;
    }

    public Set<String> customOreNames() {
        return customOreNames;
    }

    public Set<String> explicitPairs() {
        return explicitPairs;
    }

    private static void addNormalized(Set<String> target, Collection<String> rawValues) {
        if (rawValues == null) {
            return;
        }
        for (String raw : rawValues) {
            if (raw == null) {
                continue;
            }
            String trimmed = raw.trim();
            if (!trimmed.isEmpty()) {
                target.add(trimmed);
            }
        }
    }
}

