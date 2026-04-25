package com.wuxiaoya.techstart.core.service;

import com.wuxiaoya.techstart.core.model.WildcardRecipe;
import com.wuxiaoya.techstart.core.model.WildcardRuleConfig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WildcardRecipeResolver {
    private final WildcardRuleConfig config;

    public WildcardRecipeResolver(WildcardRuleConfig config) {
        this.config = config == null ? WildcardRuleConfig.defaults() : config;
    }

    public List<WildcardRecipe> resolve(String inputWildcard, String outputWildcard, List<String> availableKeys) {
        if (!isWildcard(inputWildcard) || !isWildcard(outputWildcard)) {
            return List.of();
        }

        String inputPrefix = inputWildcard.substring(0, inputWildcard.length() - 1);
        String outputPrefix = outputWildcard.substring(0, outputWildcard.length() - 1);

        MaterialIndex index = buildIndex(availableKeys == null ? List.of() : availableKeys);
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<WildcardRecipe> recipes = new ArrayList<>();

        appendExplicitPairs(recipes, seen, inputPrefix, outputPrefix);

        Map<String, String> inputMap = index.byPrefix().get(inputPrefix);
        Map<String, String> outputMap = index.byPrefix().get(outputPrefix);
        if (inputMap == null || outputMap == null) {
            return recipes;
        }

        for (Map.Entry<String, String> entry : inputMap.entrySet()) {
            String material = entry.getKey();
            String inputKey = entry.getValue();
            String outputKey = outputMap.get(material);
            if (outputKey == null) {
                continue;
            }
            addRecipe(recipes, seen, inputKey, outputKey, buildDisplayName(inputKey, outputKey));
        }

        return recipes;
    }

    public String findPrefix(String key) {
        if (key == null) {
            return null;
        }
        for (String prefix : config.supportedPrefixes()) {
            if (key.startsWith(prefix)) {
                return prefix;
            }
        }
        return null;
    }

    public String extractMaterial(String key) {
        if (key == null) {
            return null;
        }
        String prefix = findPrefix(key);
        if (prefix == null) {
            return null;
        }
        String material = key.substring(prefix.length());
        if (material.isEmpty()) {
            return null;
        }
        for (String knownPrefix : config.supportedPrefixes()) {
            if (material.startsWith(knownPrefix)) {
                return null;
            }
        }
        return material;
    }

    private MaterialIndex buildIndex(List<String> availableKeys) {
        Map<String, Map<String, String>> byPrefix = new LinkedHashMap<>();
        for (String prefix : config.supportedPrefixes()) {
            LinkedHashMap<String, String> materialMap = new LinkedHashMap<>();
            addCustomOreNames(prefix, materialMap);
            for (String key : availableKeys) {
                if (key == null || !key.startsWith(prefix)) {
                    continue;
                }
                String material = key.substring(prefix.length());
                if (material.isEmpty() || materialMap.containsKey(material)) {
                    continue;
                }
                materialMap.put(material, key);
            }
            byPrefix.put(prefix, materialMap);
        }
        return new MaterialIndex(byPrefix);
    }

    private void addCustomOreNames(String prefix, Map<String, String> materialMap) {
        for (String key : config.customOreNames()) {
            if (!key.startsWith(prefix)) {
                continue;
            }
            String material = key.substring(prefix.length());
            if (!material.isEmpty() && !materialMap.containsKey(material)) {
                materialMap.put(material, key);
            }
        }
    }

    private void appendExplicitPairs(List<WildcardRecipe> recipes, Set<String> seen, String inputPrefix, String outputPrefix) {
        for (String rawPair : config.explicitPairs()) {
            String[] pair = parsePair(rawPair);
            if (pair == null) {
                continue;
            }
            String inputKey = pair[0];
            String outputKey = pair[1];
            if (!inputKey.startsWith(inputPrefix) || !outputKey.startsWith(outputPrefix)) {
                continue;
            }
            addRecipe(recipes, seen, inputKey, outputKey, buildDisplayName(inputKey, outputKey));
        }
    }

    private String[] parsePair(String rawPair) {
        if (rawPair == null) {
            return null;
        }
        String trimmed = rawPair.trim();
        int split = trimmed.indexOf("->");
        if (split < 0) {
            return null;
        }
        String input = trimmed.substring(0, split).trim();
        String output = trimmed.substring(split + 2).trim();
        if (input.isEmpty() || output.isEmpty()) {
            return null;
        }
        return new String[]{input, output};
    }

    private void addRecipe(List<WildcardRecipe> recipes, Set<String> seen, String inputKey, String outputKey, String displayName) {
        String id = inputKey + "->" + outputKey;
        if (seen.add(id)) {
            recipes.add(new WildcardRecipe(inputKey, outputKey, displayName));
        }
    }

    private String buildDisplayName(String inputKey, String outputKey) {
        return inputKey + " -> " + outputKey;
    }

    private boolean isWildcard(String value) {
        return value != null && value.endsWith("*") && value.length() > 1;
    }

    private record MaterialIndex(Map<String, Map<String, String>> byPrefix) {
    }
}
