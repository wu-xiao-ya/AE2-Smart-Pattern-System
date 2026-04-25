package com.wuxiaoya.techstart.client;

import appeng.api.crafting.IPatternDetails;
import com.wuxiaoya.techstart.TechStartNeoForge;
import com.wuxiaoya.techstart.integration.ae2.TechStartPatternExpansion;
import com.wuxiaoya.techstart.menu.PatternEditorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PatternSearchScreen extends Screen {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TechStartNeoForge.MODID, "textures/gui/pattern_editor_sousu.png");
    private static final int PANEL_WIDTH = 176;
    private static final int PANEL_HEIGHT = 209;
    private static final int ATLAS_WIDTH = 256;
    private static final int ATLAS_HEIGHT = 256;
    private static final int SIDE_BUTTON_X = PANEL_WIDTH;
    private static final int SIDE_BUTTON_WIDTH = 24;
    private static final int SIDE_BUTTON_HEIGHT = 24;
    private static final int SWITCH_TO_FILTER_BUTTON_U = 232;
    private static final int SWITCH_TO_FILTER_BUTTON_V = 0;
    private static final int SWITCH_TO_EDITOR_BUTTON_U = 232;
    private static final int SWITCH_TO_EDITOR_BUTTON_V = 25;
    private static final int MODE_BUTTON_U = 207;
    private static final int MODE_BUTTON_V = 0;
    private static final int SEARCH_BUTTON_U = 207;
    private static final int SEARCH_BUTTON_V = 25;
    private static final int MOD_FILTER_BUTTON_U = 207;
    private static final int MOD_FILTER_BUTTON_V = 50;
    private static final int EDITOR_BUTTON_Y = 0;
    private static final int FILTER_BUTTON_Y = 24;
    private static final int MODE_BUTTON_Y = 48;
    private static final int SEARCH_BUTTON_Y = 72;
    private static final int MOD_FILTER_BUTTON_Y = 96;
    private static final int GUI_WIDTH = SIDE_BUTTON_X + SIDE_BUTTON_WIDTH;
    private static final int GUI_HEIGHT = PANEL_HEIGHT;
    private static final int LIST_X = 16;
    private static final int LIST_Y = 52;
    private static final int LIST_WIDTH = 152;
    private static final int ROW_HEIGHT = 16;
    private static final int VISIBLE_ROWS = 9;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_GAP = 2;
    private static final int SCROLLBAR_OFFSET_X = -23;
    private static final int SCROLLBAR_MIN_THUMB_HEIGHT = 12;
    private final PatternEditorScreen parent;
    private final PatternEditorMenu menu;
    private final List<SearchEntry> allEntries = new ArrayList<>();
    private final List<SearchEntry> filteredEntries = new ArrayList<>();

    private EditBox searchField;
    private int leftPos;
    private int topPos;
    private int listScroll = 0;
    private boolean draggingScrollBar = false;

    public PatternSearchScreen(PatternEditorScreen parent, PatternEditorMenu menu) {
        super(Component.translatable("gui.techstart.search.title"));
        this.parent = parent;
        this.menu = menu;
    }

    @Override
    protected void init() {
        this.menu.refreshPatternStackSnapshot();
        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - GUI_HEIGHT) / 2;

        this.searchField = new EditBox(this.font, this.leftPos + 38, this.topPos + 25, 136, 12, Component.empty());
        this.searchField.setCanLoseFocus(false);
        this.searchField.setFocused(true);
        this.searchField.setBordered(false);
        this.searchField.setTextColor(0x404040);
        this.searchField.setTextColorUneditable(0x404040);
        this.searchField.setMaxLength(64);
        this.searchField.setResponder(ignored -> applyFilter());
        this.addRenderableWidget(this.searchField);
        this.setInitialFocus(this.searchField);

        addSideButton(EDITOR_BUTTON_Y, SWITCH_TO_EDITOR_BUTTON_U, SWITCH_TO_EDITOR_BUTTON_V, "editor", Component.translatable("gui.techstart.switch_to_editor"), ignored -> closeToParent());
        addSideButton(FILTER_BUTTON_Y, SWITCH_TO_FILTER_BUTTON_U, SWITCH_TO_FILTER_BUTTON_V, "filter", Component.translatable("gui.techstart.switch_to_filter"), ignored -> switchToFilter());
        addSideButton(MODE_BUTTON_Y, MODE_BUTTON_U, MODE_BUTTON_V, "mode", Component.translatable("gui.techstart.toggle_filter_mode"), ignored -> toggleFilterMode());
        addSideButton(SEARCH_BUTTON_Y, SEARCH_BUTTON_U, SEARCH_BUTTON_V, "search", Component.translatable("gui.techstart.open_search"), ignored -> focusSearch());
        addSideButton(MOD_FILTER_BUTTON_Y, MOD_FILTER_BUTTON_U, MOD_FILTER_BUTTON_V, "mod", Component.translatable("gui.techstart.open_mod_filter"), ignored -> openModFilter());

        reloadEntries();
        applyFilter();
    }

    private void addSideButton(int y, int u, int v, String id, Component hint, InvisibleButton.OnPress onPress) {
        InvisibleButton button = this.addRenderableWidget(new InvisibleButton(
                this.leftPos + SIDE_BUTTON_X,
                this.topPos + y,
                SIDE_BUTTON_WIDTH,
                SIDE_BUTTON_HEIGHT,
                Component.literal(id),
                hint,
                onPress
        ));
        button.setSprite(u, v, v, ATLAS_WIDTH, ATLAS_HEIGHT);
    }

    private void toggleFilterMode() {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            int mode = this.menu.getFilterMode();
            int next = mode == PatternEditorMenu.FILTER_MODE_WHITELIST
                    ? PatternEditorMenu.FILTER_MODE_BLACKLIST
                    : PatternEditorMenu.FILTER_MODE_WHITELIST;
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, next);
        }
    }

    private void focusSearch() {
        if (this.searchField != null) {
            this.searchField.setFocused(true);
            this.setInitialFocus(this.searchField);
        }
    }

    private void openModFilter() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new ModFilterScreen(this.parent, this.menu));
        }
    }

    private void switchToFilter() {
        this.parent.openFilterViewForEntry("");
        closeToParent();
    }

    @Override
    public void onClose() {
        closeToParent();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            closeToParent();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (mouseButton == 0 && isOnScrollBar(mouseX, mouseY)) {
            this.draggingScrollBar = true;
            updateScrollByMouse(mouseY);
            return true;
        }
        if (mouseButton == 0) {
            SearchEntry entry = getEntryAt(mouseX, mouseY);
            if (entry != null) {
                this.parent.openFilterViewForEntry(entry.id());
                closeToParent();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.draggingScrollBar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.draggingScrollBar) {
            updateScrollByMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        int maxScroll = getMaxScroll();
        if (maxScroll <= 0 || deltaY == 0) {
            return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
        }
        this.listScroll = Mth.clamp(this.listScroll + (deltaY > 0 ? -1 : 1), 0, maxScroll);
        return true;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
        guiGraphics.drawString(this.font, this.title, this.leftPos + 16, this.topPos + 16, 0x404040, false);
        Component stat = Component.translatable("gui.techstart.search.matches", this.filteredEntries.size(), this.allEntries.size());
        guiGraphics.drawString(this.font, stat, this.leftPos + 16, this.topPos + 45, 0x666666, false);
        drawRows(guiGraphics, mouseX, mouseY);
        drawScrollBar(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        SearchEntry hovered = getEntryAt(mouseX, mouseY);
        if (hovered != null) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal(hovered.displayName()));
            tooltip.add(Component.translatable("gui.techstart.search.tooltip.input", hovered.inputLabel()));
            tooltip.add(Component.translatable("gui.techstart.search.tooltip.output", hovered.outputLabel()));
            guiGraphics.renderTooltip(this.font, tooltip.stream().map(Component::getVisualOrderText).toList(), mouseX, mouseY);
            return;
        }
        for (var widget : this.renderables) {
            if (widget instanceof InvisibleButton button && button.visible && button.isMouseOver(mouseX, mouseY)) {
                Component tip = button.getHint();
                if (tip != null && tip != CommonComponents.EMPTY) {
                    guiGraphics.renderTooltip(this.font, tip, mouseX, mouseY);
                }
                return;
            }
        }
    }

    private void reloadEntries() {
        this.allEntries.clear();
        if (this.minecraft == null || this.minecraft.level == null) {
            return;
        }
        ItemStack patternStack = this.menu.getPatternStackSnapshot();
        if (patternStack.isEmpty()) {
            return;
        }
        Set<String> seenIds = new LinkedHashSet<>();
        try {
            for (IPatternDetails detail : TechStartPatternExpansion.expandFilterCandidates(patternStack, this.minecraft.level)) {
                ItemStack variantStack = detail.getDefinition().toStack();
                if (variantStack.isEmpty()) {
                    continue;
                }
                ItemStack input = getPrimaryPatternStack(variantStack, true);
                ItemStack output = getPrimaryPatternStack(variantStack, false);
                if (input.isEmpty() || output.isEmpty()) {
                    continue;
                }
                String id = getFilterEntryId(variantStack, input, output);
                if (!seenIds.add(id)) {
                    continue;
                }
                this.allEntries.add(new SearchEntry(
                        id,
                        input.getHoverName().getString(),
                        output.getHoverName().getString(),
                        input.getHoverName().getString() + " -> " + output.getHoverName().getString()
                ));
            }
        } catch (Throwable ignored) {
            this.allEntries.clear();
        }

        if (!this.allEntries.isEmpty()) {
            return;
        }

        List<ItemStack> inputs = new ArrayList<>();
        List<ItemStack> outputs = new ArrayList<>();
        for (int slotIndex = 0; slotIndex < 9 && slotIndex < this.menu.slots.size(); slotIndex++) {
            ItemStack stack = this.menu.slots.get(slotIndex).getItem();
            if (!stack.isEmpty()) {
                inputs.add(stack.copy());
            }
        }
        for (int slotIndex = 9; slotIndex < 18 && slotIndex < this.menu.slots.size(); slotIndex++) {
            ItemStack stack = this.menu.slots.get(slotIndex).getItem();
            if (!stack.isEmpty()) {
                outputs.add(stack.copy());
            }
        }
        if (inputs.isEmpty() || outputs.isEmpty()) {
            return;
        }
        Set<String> fallbackIds = new LinkedHashSet<>();
        for (ItemStack input : inputs) {
            for (ItemStack output : outputs) {
                String id = PatternEditorMenu.buildFilterEntryId(input, output);
                if (!fallbackIds.add(id)) {
                    continue;
                }
                this.allEntries.add(new SearchEntry(
                        id,
                        input.getHoverName().getString(),
                        output.getHoverName().getString(),
                        input.getHoverName().getString() + " -> " + output.getHoverName().getString()
                ));
            }
        }
    }

    private void applyFilter() {
        this.filteredEntries.clear();
        String keyword = this.searchField == null ? "" : this.searchField.getValue().trim().toLowerCase(Locale.ROOT);
        for (SearchEntry entry : this.allEntries) {
            if (keyword.isEmpty() || entry.matches(keyword)) {
                this.filteredEntries.add(entry);
            }
        }
        this.listScroll = Mth.clamp(this.listScroll, 0, getMaxScroll());
    }


    private void drawRows(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int left = getListLeft();
        int top = getListTop();
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = this.listScroll + row;
            if (index >= this.filteredEntries.size()) {
                return;
            }
            SearchEntry entry = this.filteredEntries.get(index);
            int rowY = top + row * ROW_HEIGHT;
            boolean hovered = mouseX >= left && mouseX < left + LIST_WIDTH && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            if (hovered) {
                guiGraphics.fill(left, rowY, left + LIST_WIDTH, rowY + ROW_HEIGHT, 0x33FFFFFF);
            }
            String line = this.font.plainSubstrByWidth(entry.displayName(), LIST_WIDTH - 4);
            guiGraphics.drawString(this.font, line, left + 2, rowY + 4, 0x404040, false);
        }
    }

    private void drawScrollBar(GuiGraphics guiGraphics) {
        int barLeft = getScrollBarLeft();
        int barTop = getListTop();
        int barHeight = getListHeight();
        guiGraphics.fill(barLeft, barTop, barLeft + SCROLLBAR_WIDTH, barTop + barHeight, 0x55000000);

        int maxScroll = getMaxScroll();
        int thumbHeight = getScrollThumbHeight();
        int thumbTop = barTop;
        if (maxScroll > 0) {
            float ratio = this.listScroll / (float) maxScroll;
            thumbTop = barTop + Math.round((barHeight - thumbHeight) * ratio);
        }
        guiGraphics.fill(barLeft, thumbTop, barLeft + SCROLLBAR_WIDTH, thumbTop + thumbHeight, 0xAAFFFFFF);
    }

    private boolean isOnScrollBar(double mouseX, double mouseY) {
        int barLeft = getScrollBarLeft();
        int barTop = getListTop();
        int barHeight = getListHeight();
        return mouseX >= barLeft && mouseX < barLeft + SCROLLBAR_WIDTH && mouseY >= barTop && mouseY < barTop + barHeight;
    }

    private void updateScrollByMouse(double mouseY) {
        int maxScroll = getMaxScroll();
        if (maxScroll <= 0) {
            this.listScroll = 0;
            return;
        }
        int barTop = getListTop();
        int barHeight = getListHeight();
        int thumbHeight = getScrollThumbHeight();
        int minCenter = barTop + thumbHeight / 2;
        int maxCenter = barTop + barHeight - thumbHeight / 2;
        int center = Mth.clamp((int) mouseY, minCenter, maxCenter);
        float ratio = (center - minCenter) / (float) Math.max(1, maxCenter - minCenter);
        this.listScroll = Math.round(ratio * maxScroll);
    }

    private int getMaxScroll() {
        return Math.max(0, this.filteredEntries.size() - VISIBLE_ROWS);
    }

    private int getScrollThumbHeight() {
        if (this.filteredEntries.isEmpty()) {
            return getListHeight();
        }
        int visible = Math.min(VISIBLE_ROWS, this.filteredEntries.size());
        int height = Math.round((visible / (float) this.filteredEntries.size()) * getListHeight());
        return Math.max(SCROLLBAR_MIN_THUMB_HEIGHT, Math.min(getListHeight(), height));
    }

    private int getListLeft() {
        return this.leftPos + LIST_X;
    }

    private int getListTop() {
        return this.topPos + LIST_Y;
    }

    private int getListHeight() {
        return VISIBLE_ROWS * ROW_HEIGHT;
    }

    private int getScrollBarLeft() {
        return getListLeft() + LIST_WIDTH + SCROLLBAR_GAP + SCROLLBAR_OFFSET_X;
    }

    private SearchEntry getEntryAt(double mouseX, double mouseY) {
        int left = getListLeft();
        int top = getListTop();
        if (mouseX < left || mouseX >= left + LIST_WIDTH || mouseY < top) {
            return null;
        }
        int row = ((int) mouseY - top) / ROW_HEIGHT;
        if (row < 0 || row >= VISIBLE_ROWS) {
            return null;
        }
        int index = this.listScroll + row;
        return index >= 0 && index < this.filteredEntries.size() ? this.filteredEntries.get(index) : null;
    }

    private void closeToParent() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    private ItemStack getPrimaryPatternStack(ItemStack patternStack, boolean input) {
        if (patternStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        var tag = patternStack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if (tag.isEmpty()) {
            return ItemStack.EMPTY;
        }
        String key = input ? "VirtualInputStacks" : "VirtualOutputStacks";
        if (tag.contains(key, net.minecraft.nbt.Tag.TAG_LIST)) {
            var list = tag.getList(key, net.minecraft.nbt.Tag.TAG_COMPOUND);
            if (!list.isEmpty() && this.minecraft != null && this.minecraft.level != null) {
                return ItemStack.parseOptional(this.minecraft.level.registryAccess(), list.getCompound(0));
            }
        }
        return ItemStack.EMPTY;
    }

    private String getFilterEntryId(ItemStack patternStack, ItemStack input, ItemStack output) {
        if (!patternStack.isEmpty()) {
            var tag = patternStack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
            if (!tag.isEmpty()) {
                String id = tag.getString("VirtualFilterEntryId");
                if (id != null && !id.isBlank()) {
                    return id;
                }
            }
        }
        return PatternEditorMenu.buildFilterEntryId(input, output);
    }

    private record SearchEntry(String id, String inputLabel, String outputLabel, String displayName) {
        private boolean matches(String keyword) {
            return this.inputLabel.toLowerCase(Locale.ROOT).contains(keyword)
                    || this.outputLabel.toLowerCase(Locale.ROOT).contains(keyword)
                    || this.displayName.toLowerCase(Locale.ROOT).contains(keyword);
        }
    }
}
