package com.lwx1145.sampleintegration;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class GuiModFilter extends GuiScreen {

    private static final ResourceLocation TEXTURE =
        new ResourceLocation("sampleintegration", "textures/gui/pattern_editor_sousu.png");
    private static final int TEXTURE_DRAW_OFFSET_X = 2;
    private static final int GUI_WIDTH = 201;
    private static final int GUI_HEIGHT = 220;

    private static final int BUTTON_TO_FILTER = 0;
    private static final int BUTTON_TO_SEARCH = 1;
    private static final int OFFHAND_SLOT = -100;

    private static final int LIST_X = 16;
    private static final int LIST_Y = 52;
    private static final int LIST_WIDTH = 152;
    private static final int ROW_HEIGHT = 16;
    private static final int VISIBLE_ROWS = 9;

    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_GAP = 2;
    private static final int SCROLLBAR_OFFSET_X = -23;
    private static final int SCROLLBAR_MIN_THUMB_HEIGHT = 12;

    private final EntityPlayer player;
    private final int returnGuiId;
    private final List<ModEntry> allEntries = new ArrayList<>();
    private final List<ModEntry> filteredEntries = new ArrayList<>();
    private final Set<String> excludedInputMods = new LinkedHashSet<>();
    private final Set<String> excludedOutputMods = new LinkedHashSet<>();
    private ItemStack selectedPatternStack = ItemStack.EMPTY;
    private int selectedPatternSlot = -1;

    private GuiTextField searchField;
    private int guiLeft;
    private int guiTop;
    private int listScroll = 0;
    private boolean draggingScrollBar = false;

    public GuiModFilter(EntityPlayer player, int returnGuiId) {
        this.player = player;
        this.returnGuiId = returnGuiId;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);

        this.guiLeft = (this.width - GUI_WIDTH) / 2;
        this.guiTop = (this.height - GUI_HEIGHT) / 2;

        this.buttonList.clear();
        this.buttonList.add(new InvisibleButton(
            BUTTON_TO_FILTER,
            textureLeft() + 180,
            this.guiTop + 2,
            20,
            20,
            "1",
            I18n.format("gui.sampleintegration.switch_to_filter")
        ));
        this.buttonList.add(new InvisibleButton(
            BUTTON_TO_SEARCH,
            textureLeft() + 180,
            this.guiTop + 26,
            20,
            20,
            "2",
            I18n.format("gui.sampleintegration.open_search")
        ));

        this.searchField = new GuiTextField(
            0,
            this.fontRenderer,
            textureLeft() + 38,
            this.guiTop + 25,
            136,
            12
        );
        this.searchField.setCanLoseFocus(false);
        this.searchField.setFocused(true);
        this.searchField.setEnableBackgroundDrawing(false);
        this.searchField.setMaxStringLength(64);

        PatternTarget target = resolvePatternTarget(this.player);
        this.selectedPatternStack = target.stack;
        this.selectedPatternSlot = target.slot;

        reloadEntries();
        applyFilter();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (this.searchField != null) {
            this.searchField.updateCursorCounter();
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == BUTTON_TO_FILTER) {
            PacketHandler.INSTANCE.sendToServer(new PacketOpenPatternGui(GuiHandler.PATTERN_FILTER_GUI));
            return;
        }
        if (button.id == BUTTON_TO_SEARCH) {
            this.mc.displayGuiScreen(new GuiPatternSearch(this.player, this.returnGuiId));
            return;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.searchField != null && this.searchField.textboxKeyTyped(typedChar, keyCode)) {
            applyFilter();
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.displayGuiScreen(new GuiPatternSearch(this.player, this.returnGuiId));
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0 && isOnScrollBar(mouseX, mouseY)) {
            this.draggingScrollBar = true;
            updateScrollByMouse(mouseY);
            return;
        }
        if (mouseButton == 0 || mouseButton == 1) {
            ModEntry entry = getEntryAt(mouseX, mouseY);
            if (entry != null) {
                if (mouseButton == 0) {
                    toggleInput(entry.modId);
                } else {
                    toggleOutput(entry.modId);
                }
                return;
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (this.searchField != null) {
            this.searchField.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        this.draggingScrollBar = false;
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (this.draggingScrollBar) {
            updateScrollByMouse(mouseY);
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        int delta = Mouse.getEventDWheel();
        if (delta == 0) {
            return;
        }
        int maxScroll = getMaxScroll();
        if (maxScroll <= 0) {
            return;
        }
        if (delta > 0) {
            listScroll = Math.max(0, listScroll - 1);
        } else {
            listScroll = Math.min(maxScroll, listScroll + 1);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        this.drawTexturedModalRect(textureLeft(), this.guiTop, 0, 0, GUI_WIDTH, GUI_HEIGHT);

        String title = I18n.format("gui.sampleintegration.mod_filter.title");
        this.fontRenderer.drawString(title, textureLeft() + 16, this.guiTop + 16, 0x404040);

        String stat = I18n.format(
            "gui.sampleintegration.mod_filter.stats",
            Integer.toString(excludedInputMods.size()),
            Integer.toString(excludedOutputMods.size()),
            Integer.toString(allEntries.size())
        );
        this.fontRenderer.drawString(stat, textureLeft() + 16, this.guiTop + 45, 0x666666);

        if (this.searchField != null) {
            this.searchField.drawTextBox();
        }

        drawEntryRows(mouseX, mouseY);
        drawScrollBar();

        super.drawScreen(mouseX, mouseY, partialTicks);

        ModEntry hovered = getEntryAt(mouseX, mouseY);
        if (hovered != null) {
            List<String> tips = new ArrayList<>();
            tips.add(hovered.modId + " - " + hovered.name);
            tips.add(I18n.format("gui.sampleintegration.mod_filter.tooltip.left"));
            tips.add(I18n.format("gui.sampleintegration.mod_filter.tooltip.right"));
            String inputState = excludedInputMods.contains(hovered.modId)
                ? I18n.format("gui.sampleintegration.mod_filter.state.excluded")
                : I18n.format("gui.sampleintegration.mod_filter.state.allowed");
            String outputState = excludedOutputMods.contains(hovered.modId)
                ? I18n.format("gui.sampleintegration.mod_filter.state.excluded")
                : I18n.format("gui.sampleintegration.mod_filter.state.allowed");
            tips.add(I18n.format("gui.sampleintegration.mod_filter.tooltip.state", inputState, outputState));
            this.drawHoveringText(tips, mouseX, mouseY);
        }

        for (GuiButton button : this.buttonList) {
            if (!(button instanceof InvisibleButton) || !button.visible) {
                continue;
            }
            if (mouseX >= button.x && mouseX < button.x + button.width
                && mouseY >= button.y && mouseY < button.y + button.height) {
                String tip = ((InvisibleButton) button).getTooltip();
                if (tip != null && !tip.isEmpty()) {
                    this.drawHoveringText(Collections.singletonList(tip), mouseX, mouseY);
                }
            }
        }
    }

    private void drawEntryRows(int mouseX, int mouseY) {
        int left = getListLeft();
        int top = getListTop();
        int start = listScroll;

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = start + row;
            if (index >= filteredEntries.size()) {
                return;
            }
            ModEntry entry = filteredEntries.get(index);
            int rowY = top + row * ROW_HEIGHT;
            boolean hovered = mouseX >= left && mouseX < left + LIST_WIDTH
                && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;

            boolean inputExcluded = excludedInputMods.contains(entry.modId);
            boolean outputExcluded = excludedOutputMods.contains(entry.modId);
            if (inputExcluded && outputExcluded) {
                drawRect(left, rowY, left + LIST_WIDTH, rowY + ROW_HEIGHT, 0x446633CC);
            } else if (inputExcluded) {
                drawRect(left, rowY, left + LIST_WIDTH, rowY + ROW_HEIGHT, 0x44CC3333);
            } else if (outputExcluded) {
                drawRect(left, rowY, left + LIST_WIDTH, rowY + ROW_HEIGHT, 0x443366CC);
            }
            if (hovered) {
                drawRect(left, rowY, left + LIST_WIDTH, rowY + ROW_HEIGHT, 0x33FFFFFF);
            }

            String marker = (inputExcluded ? "I" : "-") + "/" + (outputExcluded ? "O" : "-");
            String line = marker + " " + entry.modId + " - " + entry.name;
            line = this.fontRenderer.trimStringToWidth(line, LIST_WIDTH - 4);
            this.fontRenderer.drawString(line, left + 2, rowY + 4, 0x404040);
        }
    }

    private void reloadEntries() {
        allEntries.clear();
        filteredEntries.clear();
        excludedInputMods.clear();
        excludedOutputMods.clear();

        for (String modId : ItemTest.getExcludedInputModIdsStatic(selectedPatternStack)) {
            String normalized = normalizeModId(modId);
            if (!normalized.isEmpty()) {
                excludedInputMods.add(normalized);
            }
        }
        for (String modId : ItemTest.getExcludedOutputModIdsStatic(selectedPatternStack)) {
            String normalized = normalizeModId(modId);
            if (!normalized.isEmpty()) {
                excludedOutputMods.add(normalized);
            }
        }

        Map<String, ModContainer> modMap = Loader.instance().getIndexedModList();
        for (Map.Entry<String, ModContainer> entry : modMap.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            String modId = normalizeModId(entry.getKey());
            if (modId.isEmpty()) {
                continue;
            }
            String name = entry.getValue().getName();
            if (name == null || name.isEmpty()) {
                name = modId;
            }
            allEntries.add(new ModEntry(modId, name));
        }

        allEntries.sort(Comparator.comparing(a -> a.modId));
    }

    private void applyFilter() {
        filteredEntries.clear();
        String keyword = this.searchField == null ? "" : this.searchField.getText().trim().toLowerCase(Locale.ROOT);
        for (ModEntry entry : allEntries) {
            if (keyword.isEmpty() || entry.matches(keyword)) {
                filteredEntries.add(entry);
            }
        }
        listScroll = Math.max(0, Math.min(listScroll, getMaxScroll()));
    }

    private void toggleInput(String modId) {
        if (modId == null || modId.isEmpty()) {
            return;
        }
        if (excludedInputMods.contains(modId)) {
            excludedInputMods.remove(modId);
        } else {
            excludedInputMods.add(modId);
        }
        saveExcludedMods();
    }

    private void toggleOutput(String modId) {
        if (modId == null || modId.isEmpty()) {
            return;
        }
        if (excludedOutputMods.contains(modId)) {
            excludedOutputMods.remove(modId);
        } else {
            excludedOutputMods.add(modId);
        }
        saveExcludedMods();
    }

    private void saveExcludedMods() {
        String[] inputValues = excludedInputMods.toArray(new String[0]);
        String[] outputValues = excludedOutputMods.toArray(new String[0]);
        Arrays.sort(inputValues);
        Arrays.sort(outputValues);
        if (selectedPatternStack == null || selectedPatternStack.isEmpty()) {
            return;
        }
        ItemTest.setExcludedModFiltersStatic(selectedPatternStack, inputValues, outputValues);
        this.player.inventory.markDirty();
        PacketHandler.INSTANCE.sendToServer(
            new PacketUpdatePatternModFilters(this.selectedPatternSlot, inputValues, outputValues)
        );
    }

    private int textureLeft() {
        return this.guiLeft + TEXTURE_DRAW_OFFSET_X;
    }

    private int getListLeft() {
        return textureLeft() + LIST_X;
    }

    private int getListTop() {
        return this.guiTop + LIST_Y;
    }

    private int getListHeight() {
        return VISIBLE_ROWS * ROW_HEIGHT;
    }

    private int getScrollBarLeft() {
        return getListLeft() + LIST_WIDTH + SCROLLBAR_GAP + SCROLLBAR_OFFSET_X;
    }

    private int getScrollThumbHeight() {
        if (filteredEntries.isEmpty()) {
            return getListHeight();
        }
        int total = filteredEntries.size();
        int visible = Math.min(VISIBLE_ROWS, total);
        int height = Math.round((visible / (float) total) * getListHeight());
        return Math.max(SCROLLBAR_MIN_THUMB_HEIGHT, Math.min(getListHeight(), height));
    }

    private boolean isOnScrollBar(int mouseX, int mouseY) {
        int barLeft = getScrollBarLeft();
        int barTop = getListTop();
        int barHeight = getListHeight();
        return mouseX >= barLeft && mouseX < barLeft + SCROLLBAR_WIDTH
            && mouseY >= barTop && mouseY < barTop + barHeight;
    }

    private void updateScrollByMouse(int mouseY) {
        int maxScroll = getMaxScroll();
        if (maxScroll <= 0) {
            listScroll = 0;
            return;
        }

        int barTop = getListTop();
        int barHeight = getListHeight();
        int thumbHeight = getScrollThumbHeight();

        int minCenter = barTop + thumbHeight / 2;
        int maxCenter = barTop + barHeight - thumbHeight / 2;
        int center = Math.max(minCenter, Math.min(maxCenter, mouseY));

        float ratio = (center - minCenter) / (float) Math.max(1, (maxCenter - minCenter));
        listScroll = Math.round(ratio * maxScroll);
    }

    private void drawScrollBar() {
        int barLeft = getScrollBarLeft();
        int barTop = getListTop();
        int barHeight = getListHeight();
        drawRect(barLeft, barTop, barLeft + SCROLLBAR_WIDTH, barTop + barHeight, 0x55000000);

        int maxScroll = getMaxScroll();
        int thumbHeight = getScrollThumbHeight();
        int thumbTop = barTop;
        if (maxScroll > 0) {
            float ratio = listScroll / (float) maxScroll;
            thumbTop = barTop + Math.round((barHeight - thumbHeight) * ratio);
        }
        drawRect(barLeft, thumbTop, barLeft + SCROLLBAR_WIDTH, thumbTop + thumbHeight, 0xAAFFFFFF);
    }

    private int getMaxScroll() {
        return Math.max(0, filteredEntries.size() - VISIBLE_ROWS);
    }

    private ModEntry getEntryAt(int mouseX, int mouseY) {
        int left = getListLeft();
        int top = getListTop();
        if (mouseX < left || mouseX >= left + LIST_WIDTH || mouseY < top) {
            return null;
        }

        int row = (mouseY - top) / ROW_HEIGHT;
        if (row < 0 || row >= VISIBLE_ROWS) {
            return null;
        }

        int index = listScroll + row;
        if (index < 0 || index >= filteredEntries.size()) {
            return null;
        }
        return filteredEntries.get(index);
    }

    private static String normalizeModId(String modId) {
        if (modId == null) {
            return "";
        }
        return modId.trim().toLowerCase(Locale.ROOT);
    }

    private static PatternTarget resolvePatternTarget(EntityPlayer player) {
        if (player == null) {
            return new PatternTarget(ItemStack.EMPTY, -1);
        }

        ItemStack main = player.getHeldItemMainhand();
        if (!main.isEmpty() && main.getItem() instanceof ItemTest) {
            return new PatternTarget(main, player.inventory.currentItem);
        }

        ItemStack off = player.getHeldItemOffhand();
        if (!off.isEmpty() && off.getItem() instanceof ItemTest) {
            return new PatternTarget(off, OFFHAND_SLOT);
        }

        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemTest) {
                return new PatternTarget(stack, i);
            }
        }

        return new PatternTarget(ItemStack.EMPTY, -1);
    }

    private static final class PatternTarget {
        private final ItemStack stack;
        private final int slot;

        private PatternTarget(ItemStack stack, int slot) {
            this.stack = stack == null ? ItemStack.EMPTY : stack;
            this.slot = slot;
        }
    }

    private static final class ModEntry {
        private final String modId;
        private final String name;

        private ModEntry(String modId, String name) {
            this.modId = modId;
            this.name = name;
        }

        private boolean matches(String keyword) {
            if (keyword == null || keyword.isEmpty()) {
                return true;
            }
            String lower = keyword.toLowerCase(Locale.ROOT);
            return (modId != null && modId.toLowerCase(Locale.ROOT).contains(lower))
                || (name != null && name.toLowerCase(Locale.ROOT).contains(lower));
        }
    }
}
