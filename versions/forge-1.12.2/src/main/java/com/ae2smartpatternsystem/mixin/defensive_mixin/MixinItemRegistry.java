package com.ae2smartpatternsystem.mixin.defensive_mixin;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.item.ItemStack;

@Pseudo
@Mixin(targets = "org.orecruncher.dsurround.registry.item.ItemRegistry", remap = false)
public class MixinItemRegistry {
    @Inject(method = "getItemClass", at = @At("HEAD"), cancellable = true, remap = false)
    private void onGetItemClass(ItemStack stack, CallbackInfoReturnable<Object> cir) {
        if (stack == null || stack.isEmpty()) {
            cir.setReturnValue(getNoneDataReflect());
            cir.cancel();
            return;
        }
        Object result = cir.getReturnValue();
        if (result == null) {
            cir.setReturnValue(getNoneDataReflect());
            cir.cancel();
        }
    }

    private Object getNoneDataReflect() {
        try {
            Class<?> clazz = Class.forName("org.orecruncher.dsurround.registry.item.ItemRegistry");
            java.lang.reflect.Field f = clazz.getDeclaredField("NONE_DATA");
            f.setAccessible(true);
            return f.get(null);
        } catch (Throwable t) {
            return null;
        }
    }
}

