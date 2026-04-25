package com.lwx1145.sampleintegration;



import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.renderer.GlStateManager;
import java.util.Collections;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class GuiPatternEditor extends GuiContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation("sampleintegration", "textures/gui/pattern_editor_smart.png");
    private static final int TEXTURE_DRAW_OFFSET_X = 2;
    private final EntityPlayer player;
    private final ContainerPatternEditor container;
    private static final int BUTTON_SWITCH = 0;
    private static final int BUTTON_SEARCH = 2;
    private static final int BUTTON_MOD_FILTER = 3;

    public GuiPatternEditor(ContainerPatternEditor container, EntityPlayer player) {
        super(container);
        this.container = container;
        this.player = player;
        this.xSize = 201;
        this.ySize = 220;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.clear();
        int guiLeft = (this.width - this.xSize) / 2;
        int guiTop = (this.height - this.ySize) / 2;

        this.buttonList.add(new InvisibleButton(BUTTON_SWITCH, guiLeft + 180, guiTop + 2, 20, 20, "2",
            I18n.format("gui.sampleintegration.switch_to_filter")));
        this.buttonList.add(new InvisibleButton(BUTTON_SEARCH, guiLeft + 180, guiTop + 26, 20, 20, "S",
            I18n.format("gui.sampleintegration.open_search")));
        this.buttonList.add(new InvisibleButton(BUTTON_MOD_FILTER, guiLeft + 180, guiTop + 50, 20, 20, "M",
            I18n.format("gui.sampleintegration.open_mod_filter")));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == BUTTON_SWITCH) {
            PacketHandler.INSTANCE.sendToServer(new PacketRequestEncodePattern());
            PacketHandler.INSTANCE.sendToServer(new PacketOpenPatternGui(GuiHandler.PATTERN_FILTER_GUI));
            return;
        }
        if (button.id == BUTTON_SEARCH) {
            this.mc.displayGuiScreen(new GuiPatternSearch(this.player, GuiHandler.PATTERN_EDITOR_GUI));
            return;
        }
        if (button.id == BUTTON_MOD_FILTER) {
            this.mc.displayGuiScreen(new GuiModFilter(this.player, GuiHandler.PATTERN_EDITOR_GUI));
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(i + TEXTURE_DRAW_OFFSET_X, j, 0, 0, this.xSize, this.ySize);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws java.io.IOException {
        if (mouseButton == 2) {
            Slot slot = this.getSlotUnderMouse();
            if (slot != null && container.isPatternSlotId(slot.slotNumber)) {
                ItemStack stack = slot.getStack();
                if (container.isItemMarkerStack(stack)) {
                    int amount = container.getMarkerAmountForSlot(slot.slotNumber);
                    this.mc.displayGuiScreen(new GuiFluidAmountInput(this, container, slot.slotNumber, amount, "gui.sampleintegration.amount.set"));
                    return;
                }
                if (container.isFluidMarkerStack(stack)) {
                    int amount = container.getMarkerAmountForSlot(slot.slotNumber);
                    this.mc.displayGuiScreen(new GuiFluidAmountInput(this, container, slot.slotNumber, amount, "gui.sampleintegration.amount.set_mb"));
                    return;
                }
            }
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = I18n.format("gui.sampleintegration.pattern_editor");
        this.fontRenderer.drawString(title, (this.xSize - this.fontRenderer.getStringWidth(title)) / 2, 2, 4210752);
        this.fontRenderer.drawString(I18n.format("gui.sampleintegration.input"), 38, 20, 0x404040);
        this.fontRenderer.drawString(I18n.format("gui.sampleintegration.output"), 109, 20, 0x404040);
        this.fontRenderer.drawString(I18n.format("gui.sampleintegration.tip_marker"), 15, 110, 0x404040);
    }

    @Override
    public void handleMouseInput() throws java.io.IOException {
        super.handleMouseInput();
        int delta = Mouse.getEventDWheel();
        if (delta == 0) {
            return;
        }
        Slot slot = this.getSlotUnderMouse();
        if (slot == null || !container.isPatternSlotId(slot.slotNumber)) {
            return;
        }
        ItemStack stack = slot.getStack();
        if (stack.isEmpty() || !container.isMarkerStack(stack)) {
            return;
        }
        int step = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT) ? 10 : 1;
        int amount = container.getMarkerAmountForSlot(slot.slotNumber);
        int next = delta > 0 ? amount + step : amount - step;
        next = Math.max(1, next);
        PacketHandler.INSTANCE.sendToServer(new PacketSetFluidAmount(slot.slotNumber, next));
        container.applyMarkerAmountToSlot(slot.slotNumber, next);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderMarkerAmounts();
        this.renderHoveredToolTip(mouseX, mouseY);

        for (GuiButton btn : this.buttonList) {
            if (!(btn instanceof InvisibleButton)) continue;
            if (!btn.visible) continue;
            int bx = btn.x;
            int by = btn.y;
            if (mouseX >= bx && mouseX < bx + btn.width && mouseY >= by && mouseY < by + btn.height) {
                String tip = ((InvisibleButton) btn).getTooltip();
                if (tip != null && !tip.isEmpty()) {
                    this.drawHoveringText(Collections.singletonList(tip), mouseX, mouseY);
                }
            }
        }
    }

    public int getGuiLeft() {
        return this.guiLeft;
    }

    public int getGuiTop() {
        return this.guiTop;
    }

    private void renderMarkerAmounts() {
        for (Slot slot : this.inventorySlots.inventorySlots) {
            if (slot == null || !container.isPatternSlotId(slot.slotNumber)) {
                continue;
            }
            ItemStack stack = slot.getStack();
            if (stack.isEmpty() || !container.isMarkerStack(stack)) {
                continue;
            }
            renderAmountText(slot.xPos, slot.yPos, container.getMarkerAmountForSlot(slot.slotNumber));
        }
    }

    private void renderAmountText(int slotX, int slotY, int amount) {
        if (amount <= 1) {
            return;
        }

        String text = formatAmountShort(amount);
        int x = this.guiLeft + slotX + 17 - this.fontRenderer.getStringWidth(text);
        int y = this.guiTop + slotY + 9;

        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        float oldGuiZ = this.zLevel;
        float oldItemZ = this.itemRender.zLevel;
        this.zLevel = 300.0F;
        this.itemRender.zLevel = 300.0F;
        this.fontRenderer.drawStringWithShadow(text, x, y, 0x00E5FF);
        this.zLevel = oldGuiZ;
        this.itemRender.zLevel = oldItemZ;
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
    }

    private String formatAmountShort(int amount) {
        if (amount < 1000) {
            return Integer.toString(amount);
        }
        if (amount < 1_000_000) {
            return (amount / 1000) + "k";
        }
        if (amount < 1_000_000_000) {
            return (amount / 1_000_000) + "m";
        }
        return (amount / 1_000_000_000) + "b";
    }

}

