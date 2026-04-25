package com.wuxiaoya.techstart.integration.emi;

import com.wuxiaoya.techstart.client.PatternEditorScreen;
import com.wuxiaoya.techstart.menu.PatternEditorMenu;
import com.wuxiaoya.techstart.network.SetPatternSlotPayload;
import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class PatternEditorEmiDragDropHandler implements EmiDragDropHandler<PatternEditorScreen> {
    @Override
    public boolean dropStack(PatternEditorScreen screen, EmiIngredient ingredient, int x, int y) {
        ItemStack marker = createMarker(screen.getMenu(), ingredient);
        if (marker.isEmpty()) {
            return false;
        }

        PatternEditorMenu menu = screen.getMenu();
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        for (int slotId = 0; slotId < menu.slots.size(); slotId++) {
            if (!screen.isPatternSlotId(slotId)) {
                continue;
            }
            Slot slot = menu.slots.get(slotId);
            int minX = left + slot.x - 1;
            int minY = top + slot.y - 1;
            if (x >= minX && x < minX + 18 && y >= minY && y < minY + 18) {
                applyMarker(screen, slotId, marker);
                return true;
            }
        }
        return false;
    }

    @Override
    public void render(PatternEditorScreen screen, EmiIngredient dragged, GuiGraphics draw, int mouseX, int mouseY, float delta) {
        if (createMarker(screen.getMenu(), dragged).isEmpty()) {
            return;
        }
        PatternEditorMenu menu = screen.getMenu();
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        for (int slotId = 0; slotId < menu.slots.size(); slotId++) {
            if (!screen.isPatternSlotId(slotId)) {
                continue;
            }
            Slot slot = menu.slots.get(slotId);
            draw.fill(left + slot.x - 1, top + slot.y - 1, left + slot.x + 17, top + slot.y + 17, 0x8822BB33);
        }
    }

    private ItemStack createMarker(PatternEditorMenu menu, EmiIngredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) {
            return ItemStack.EMPTY;
        }
        for (EmiStack emiStack : ingredient.getEmiStacks()) {
            ItemStack marker = createMarker(menu, emiStack);
            if (!marker.isEmpty()) {
                return marker;
            }
        }
        return ItemStack.EMPTY;
    }

    private ItemStack createMarker(PatternEditorMenu menu, EmiStack emiStack) {
        if (emiStack == null || emiStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        long amount = Math.max(1L, emiStack.getAmount());
        Object key = emiStack.getKey();
        ItemStack marker = menu.createMarkerFromIngredient(key, amount);
        if (!marker.isEmpty()) {
            return marker;
        }
        if (key instanceof Fluid fluid) {
            marker = menu.createMarkerFromIngredient(new FluidStack(fluid, (int) Math.min(Integer.MAX_VALUE, amount)), amount);
            if (!marker.isEmpty()) {
                return marker;
            }
        }
        ItemStack itemStack = emiStack.getItemStack();
        if (!itemStack.isEmpty()) {
            marker = menu.createMarkerFromIngredient(itemStack, amount);
            if (!marker.isEmpty()) {
                return marker;
            }
        }
        return menu.createMarkerFromIngredient(emiStack, amount);
    }

    private void applyMarker(PatternEditorScreen screen, int slotId, ItemStack marker) {
        if (marker.isEmpty() || !screen.isPatternSlotId(slotId)) {
            return;
        }
        screen.getMenu().slots.get(slotId).set(marker.copy());
        PacketDistributor.sendToServer(new SetPatternSlotPayload(slotId, marker.copy()));
    }
}
