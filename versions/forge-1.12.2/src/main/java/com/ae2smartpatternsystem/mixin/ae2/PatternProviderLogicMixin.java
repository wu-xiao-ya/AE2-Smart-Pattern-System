package com.ae2smartpatternsystem.mixin.ae2;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.util.inv.AppEngInternalInventory;
import com.ae2smartpatternsystem.ItemTest;
import com.ae2smartpatternsystem.SmartPatternDetails;
import com.ae2smartpatternsystem.TechStart;
import com.ae2smartpatternsystem.integration.ae2.TechStartPatternDetails;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PatternProviderLogic.class, remap = false)
public abstract class PatternProviderLogicMixin {
    @Shadow @Final private AppEngInternalInventory patternInventory;
    @Shadow @Final private ObjectList<IPatternDetails> patterns;
    @Shadow @Final private ObjectSet<AEKey> patternInputs;
    @Shadow @Final private PatternProviderLogicHost host;
    @Shadow @Final private IManagedGridNode mainNode;
    @Shadow private boolean hasLastSuccessfulPatternHash;
    @Shadow private int lastSuccessfulPatternHash;

    @Inject(method = "updatePatterns", at = @At("HEAD"), cancellable = true)
    private void ae2sps$expandCustomPatterns(CallbackInfo ci) {
        this.patterns.clear();
        this.patternInputs.clear();

        World world = this.host.getTileEntity() == null ? null : this.host.getTileEntity().getWorld();
        for (int slot = 0; slot < this.patternInventory.size(); slot++) {
            ItemStack stack = this.patternInventory.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            if (stack.getItem() instanceof ItemTest) {
                SmartPatternDetails helper = new SmartPatternDetails(stack);
                if (helper.isWildcardPattern()) {
                    for (SmartPatternDetails expanded : helper.expandToVirtualPatterns()) {
                        ItemStack virtualStack = expanded.getPattern();
                        AEItemKey key = AEItemKey.of(virtualStack);
                        if (key != null) {
                            addExpandedPattern(key, virtualStack);
                        }
                    }
                    continue;
                }
            }

            IPatternDetails decoded = PatternDetailsHelper.decodePattern(stack, world);
            if (decoded != null) {
                addPattern(decoded);
            }
        }

        clearLastSuccessfulPatternIfMissing();
        ICraftingProvider.requestUpdate(this.mainNode);
        ci.cancel();
    }

    private void addExpandedPattern(AEItemKey key, ItemStack virtualStack) {
        try {
            addPattern(new TechStartPatternDetails(key, virtualStack));
        } catch (IllegalArgumentException e) {
            TechStart.LOGGER.warn("AE2SPS skipped invalid expanded pattern stack: {}", virtualStack);
        }
    }

    private void addPattern(IPatternDetails pattern) {
        this.patterns.add(pattern);
        for (IPatternDetails.IInput input : pattern.getInputs()) {
            for (GenericStack possibleInput : input.possibleInputs()) {
                this.patternInputs.add(possibleInput.what().dropSecondary());
            }
        }
    }

    private void clearLastSuccessfulPatternIfMissing() {
        if (!this.hasLastSuccessfulPatternHash) {
            return;
        }
        for (IPatternDetails pattern : this.patterns) {
            if (pattern.getDefinition().hashCode() == this.lastSuccessfulPatternHash) {
                return;
            }
        }
        this.hasLastSuccessfulPatternHash = false;
    }
}
