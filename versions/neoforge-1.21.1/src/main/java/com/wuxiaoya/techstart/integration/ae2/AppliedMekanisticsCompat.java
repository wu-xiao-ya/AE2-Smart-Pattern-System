package com.wuxiaoya.techstart.integration.ae2;

import appeng.api.stacks.AEKey;
import com.wuxiaoya.techstart.TechStartNeoForge;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

final class AppliedMekanisticsCompat {
    private AppliedMekanisticsCompat() {
    }

    static @Nullable AEKey createChemicalKey(String chemicalId, long amount) {
        if (chemicalId == null || chemicalId.isBlank()) {
            TechStartNeoForge.LOGGER.info("AppliedMekanisticsCompat: skipped blank chemical id");
            return null;
        }
        ResourceLocation key = ResourceLocation.tryParse(chemicalId.trim());
        if (key == null) {
            TechStartNeoForge.LOGGER.info("AppliedMekanisticsCompat: invalid chemical id {}", chemicalId);
            return null;
        }

        try {
            Chemical chemical = MekanismAPI.CHEMICAL_REGISTRY.get(key);
            if (chemical == null) {
                TechStartNeoForge.LOGGER.info("AppliedMekanisticsCompat: registry miss for {}", chemicalId);
                return null;
            }
            ChemicalStack chemicalStack = new ChemicalStack(chemical, Math.max(1L, amount));
            AEKey aeKey = MekanismKey.of(chemicalStack);
            TechStartNeoForge.LOGGER.info(
                    "AppliedMekanisticsCompat: resolved {} amount {} -> chemicalClass={} keyClass={}",
                    chemicalId,
                    amount,
                    chemical.getClass().getName(),
                    aeKey == null ? "null" : aeKey.getClass().getName());
            return aeKey;
        } catch (Exception exception) {
            TechStartNeoForge.LOGGER.info(
                    "AppliedMekanisticsCompat: failed resolving {} amount {} -> {}: {}",
                    chemicalId,
                    amount,
                    exception.getClass().getName(),
                    exception.getMessage());
            return null;
        }
    }
}
