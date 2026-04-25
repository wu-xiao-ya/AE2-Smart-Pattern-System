package com.wuxiaoya.techstart.client;

import com.wuxiaoya.techstart.TechStartForge;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class InvisibleButton extends AbstractWidget {
    private static final ResourceLocation DEFAULT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TechStartForge.MODID, "textures/gui/pattern_all.png");

    @FunctionalInterface
    public interface OnPress {
        void onPress(InvisibleButton button);
    }

    private final OnPress onPress;
    private Component hint;
    private ResourceLocation texture = DEFAULT_TEXTURE;
    private int u = 200;
    private int v = 0;
    private int hoverV = 0;
    private int textureWidth = 256;
    private int textureHeight = 256;

    public InvisibleButton(int x, int y, int width, int height, Component message, Component tooltip, OnPress onPress) {
        super(x, y, width, height, message);
        this.onPress = onPress;
        this.hint = tooltip;
        this.hoverV = this.v;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (this.active && this.visible) {
            this.onPress.onPress(this);
        }
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int drawV = this.isHoveredOrFocused() ? this.hoverV : this.v;
        guiGraphics.blit(this.texture, this.getX(), this.getY(), this.u, drawV, this.width, this.height, this.textureWidth, this.textureHeight);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }

    public Component getHint() {
        return this.hint;
    }

    public void setHint(Component tooltip) {
        this.hint = tooltip;
    }

    public void setSprite(int u, int v, int hoverV, int textureWidth, int textureHeight) {
        this.u = u;
        this.v = v;
        this.hoverV = hoverV;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }
}
