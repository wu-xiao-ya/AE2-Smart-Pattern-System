package com.ae2smartpatternsystem.core.model;

import com.ae2smartpatternsystem.core.codec.PatternNbtKeys;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModFilterRuleTest {
    @Test
    void normalizesValidIdsAndPreservesFirstOccurrenceOrder() {
        ModFilterRule rule = ModFilterRule.blacklist(List.of(
                "  Create ",
                "create",
                "AE2",
                "foo_bar",
                "foo-bar",
                "foo.bar",
                "foo.bar"
        ));

        assertEquals(List.of("create", "ae2", "foo_bar", "foo-bar", "foo.bar"), rule.modIds());
        assertEquals(FilterMode.BLACKLIST, rule.mode());
        assertEquals("some_mod", ModFilterRule.normalizeModId(" Some_Mod "));
    }

    @Test
    void ignoresNullBlankAndIllegalIds() {
        ModFilterRule rule = ModFilterRule.blacklist(Arrays.asList(
                null,
                "",
                "   ",
                "valid.mod",
                "bad id",
                "bad/id",
                "bad:id",
                "bad~id",
                "valid.mod!"
        ));

        assertEquals(List.of("valid.mod"), rule.modIds());
        assertEquals("", ModFilterRule.normalizeModId(null));
        assertEquals("", ModFilterRule.normalizeModId(" "));
        assertEquals("", ModFilterRule.normalizeModId("bad id"));
        assertEquals("", ModFilterRule.normalizeModId("UPPER/CASE"));
    }

    @Test
    void blacklistAllowsUnlistedNamespacesAndRejectsListedNamespaces() {
        ModFilterRule rule = ModFilterRule.blacklist(List.of(" create "));

        assertFalse(rule.allows("CREATE"));
        assertTrue(rule.allows("minecraft"));
        assertTrue(rule.allows(null));
        assertTrue(rule.allows("bad id"));
    }

    @Test
    void whitelistAllowsListedNamespacesAndRejectsEverythingElse() {
        ModFilterRule rule = ModFilterRule.of(FilterMode.WHITELIST, List.of(" create "));

        assertTrue(rule.allows("CREATE"));
        assertFalse(rule.allows("minecraft"));
        assertFalse(rule.allows(null));
        assertFalse(rule.allows("bad id"));
    }

    @Test
    void emptyListsHaveModeSpecificDefaults() {
        ModFilterRule blacklist = ModFilterRule.blacklist(List.of());
        ModFilterRule whitelist = ModFilterRule.of(FilterMode.WHITELIST, List.of());

        assertTrue(blacklist.allows("minecraft"));
        assertTrue(blacklist.allows(""));
        assertFalse(whitelist.allows("minecraft"));
        assertFalse(whitelist.allows(""));
    }

    @Test
    void nullCollectionsAreTreatedAsEmpty() {
        assertTrue(ModFilterRule.blacklist(null).modIds().isEmpty());
        assertFalse(ModFilterRule.of(FilterMode.WHITELIST, null).allows("minecraft"));
    }

    @Test
    void returnedIdsAreImmutableAndInputChangesDoNotAffectRule() {
        List<String> source = new ArrayList<>(List.of("create"));
        ModFilterRule rule = ModFilterRule.blacklist(source);
        source.add("minecraft");

        assertEquals(List.of("create"), rule.modIds());
        assertThrows(UnsupportedOperationException.class, () -> rule.modIds().add("minecraft"));
    }

    @Test
    void inputAndOutputRulesCanBeCombinedIndependently() {
        ModFilterRule inputRule = ModFilterRule.blacklist(List.of("create"));
        ModFilterRule outputRule = ModFilterRule.of(FilterMode.WHITELIST, List.of("ae2"));

        assertFalse(inputRule.allows("create"));
        assertTrue(inputRule.allows("ae2"));
        assertTrue(outputRule.allows("ae2"));
        assertFalse(outputRule.allows("create"));
        assertFalse(outputRule.allows("bad id"));
    }

    @Test
    void migratesLegacyExcludedIdsWithoutChangingBlacklistBehavior() {
        ModFilterRule rule = ModFilterRule.fromStoredData(
                null,
                false,
                List.of(),
                List.of(" Create ", "AE2"));

        assertEquals(FilterMode.BLACKLIST, rule.mode());
        assertEquals(List.of("create", "ae2"), rule.modIds());
        assertFalse(rule.allows("create"));
        assertTrue(rule.allows("minecraft"));
    }

    @Test
    void canonicalModeCanReuseLegacyIdsDuringPartialMigration() {
        ModFilterRule rule = ModFilterRule.fromStoredData(
                FilterMode.WHITELIST.serializedValue(),
                false,
                List.of(),
                List.of(" Create "));

        assertEquals(FilterMode.WHITELIST, rule.mode());
        assertTrue(rule.allows("create"));
        assertFalse(rule.allows("minecraft"));
    }

    @Test
    void canonicalIdsTakePrecedenceEvenWhenTheyAreEmpty() {
        ModFilterRule rule = ModFilterRule.fromStoredData(
                FilterMode.WHITELIST.serializedValue(),
                true,
                List.of(),
                List.of("create"));

        assertEquals(FilterMode.WHITELIST, rule.mode());
        assertTrue(rule.modIds().isEmpty());
        assertFalse(rule.allows("create"));
    }

    @Test
    void exposesCanonicalAndLegacyNbtKeys() {
        assertEquals("TechStartInputModFilterMode", PatternNbtKeys.TAG_INPUT_MOD_FILTER_MODE);
        assertEquals("TechStartOutputModFilterMode", PatternNbtKeys.TAG_OUTPUT_MOD_FILTER_MODE);
        assertEquals("TechStartInputModFilterIds", PatternNbtKeys.TAG_INPUT_MOD_FILTER_IDS);
        assertEquals("TechStartOutputModFilterIds", PatternNbtKeys.TAG_OUTPUT_MOD_FILTER_IDS);
        assertEquals("ExcludedInputModIds", PatternNbtKeys.TAG_EXCLUDED_INPUT_MOD_IDS);
        assertEquals("ExcludedOutputModIds", PatternNbtKeys.TAG_EXCLUDED_OUTPUT_MOD_IDS);
    }

    @Test
    void rejectsNullMode() {
        assertThrows(NullPointerException.class, () -> ModFilterRule.of(null, List.of("create")));
    }
}
