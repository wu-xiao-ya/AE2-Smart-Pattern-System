package com.wuxiaoya.techstart.core.service;

import com.wuxiaoya.techstart.core.model.FilterEntry;
import com.wuxiaoya.techstart.core.model.FilterMode;
import com.wuxiaoya.techstart.core.model.PatternDefinition;
import com.wuxiaoya.techstart.core.model.WildcardRecipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class WildcardRecipeFilter {
    private WildcardRecipeFilter() {
    }

    public static List<WildcardRecipe> apply(PatternDefinition definition, Collection<WildcardRecipe> recipes) {
        return apply(definition.filterMode(), definition.filterEntries(), recipes);
    }

    public static List<WildcardRecipe> apply(FilterMode mode, Collection<FilterEntry> selectedEntries, Collection<WildcardRecipe> recipes) {
        if (recipes == null || recipes.isEmpty()) {
            return List.of();
        }
        if (selectedEntries == null || selectedEntries.isEmpty()) {
            return List.copyOf(recipes);
        }

        Set<String> rawEntries = new LinkedHashSet<>();
        for (FilterEntry entry : selectedEntries) {
            if (entry != null) {
                rawEntries.add(entry.asSerializedId());
            }
        }

        List<WildcardRecipe> result = new ArrayList<>();
        for (WildcardRecipe recipe : recipes) {
            if (isAllowed(mode, rawEntries, recipe)) {
                result.add(recipe);
            }
        }
        return result;
    }

    public static boolean isAllowed(FilterMode mode, Collection<FilterEntry> selectedEntries, WildcardRecipe recipe) {
        if (selectedEntries == null || selectedEntries.isEmpty()) {
            return true;
        }
        Set<String> rawEntries = new LinkedHashSet<>();
        for (FilterEntry entry : selectedEntries) {
            if (entry != null) {
                rawEntries.add(entry.asSerializedId());
            }
        }
        return isAllowed(mode, rawEntries, recipe);
    }

    private static boolean isAllowed(FilterMode mode, Set<String> rawEntries, WildcardRecipe recipe) {
        String pairId = recipe.inputKey() + "->" + recipe.outputKey();

        boolean hasVariantEntriesForPair = false;
        String pairPrefix = pairId + "|";
        for (String entry : rawEntries) {
            if (entry != null && entry.startsWith(pairPrefix)) {
                hasVariantEntriesForPair = true;
                break;
            }
        }

        boolean contains;
        if (hasVariantEntriesForPair) {
            contains = false;
            for (String entry : rawEntries) {
                if (entry != null && entry.startsWith(pairPrefix)) {
                    contains = true;
                    break;
                }
            }
        } else {
            contains = rawEntries.contains(pairId);
        }

        return mode == FilterMode.BLACKLIST ? !contains : contains;
    }
}
