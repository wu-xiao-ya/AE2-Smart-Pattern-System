package com.wuxiaoya.techstart.client;

import com.wuxiaoya.techstart.menu.PatternEditorMenu;
import com.wuxiaoya.techstart.network.SetPatternModFiltersPayload;
import com.wuxiaoya.techstart.network.TechStartNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public class ModFilterScreen extends Screen {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(com.wuxiaoya.techstart.TechStartNeoForge.MODID, "textures/gui/pattern_editor_sousu.png");
    private static final int PANEL_WIDTH = 176;
    private static final int PANEL_HEIGHT = 209;
    private static final int ATLAS_WIDTH = 256;
    private static final int ATLAS_HEIGHT = 256;
    private static final int SIDE_BUTTON_X = 176;
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
    private final List<ModEntry> allEntries = new ArrayList<>();
    private final List<ModEntry> filteredEntries = new ArrayList<>();
    private final LinkedHashSet<String> excludedInputMods = new LinkedHashSet<>();
    private final LinkedHashSet<String> excludedOutputMods = new LinkedHashSet<>();

    private EditBox searchField;
    private int leftPos;
    private int topPos;
    private int listScroll = 0;
    private boolean draggingScrollBar = false;

    public ModFilterScreen(PatternEditorScreen parent, PatternEditorMenu menu) {
        super(Component.translatable("gui.techstart.mod_filter.title"));
        this.parent = parent;
        this.menu = menu;
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - GUI_HEIGHT) / 2;

        this.excludedInputMods.clear();
        this.excludedInputMods.addAll(this.menu.getExcludedInputModIdsSnapshot());
        this.excludedOutputMods.clear();
        this.excludedOutputMods.addAll(this.menu.getExcludedOutputModIdsSnapshot());

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
        addSideButton(SEARCH_BUTTON_Y, SEARCH_BUTTON_U, SEARCH_BUTTON_V, "search", Component.translatable("gui.techstart.open_search"), ignored -> openSearch());
        addSideButton(MOD_FILTER_BUTTON_Y, MOD_FILTER_BUTTON_U, MOD_FILTER_BUTTON_V, "mod", Component.translatable("gui.techstart.open_mod_filter"), ignored -> focusSearch());

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

    private void openSearch() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new PatternSearchScreen(this.parent, this.menu));
        }
    }

    private void focusSearch() {
        if (this.searchField != null) {
            this.searchField.setFocused(true);
            this.setInitialFocus(this.searchField);
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
        if (mouseButton == 0 || mouseButton == 1) {
            ModEntry entry = getEntryAt(mouseX, mouseY);
            if (entry != null) {
                if (mouseButton == 0) {
                    toggleInput(entry.modId());
                } else {
                    toggleOutput(entry.modId());
                }
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
        Component stat = Component.translatable(
                "gui.techstart.mod_filter.stats",
                this.excludedInputMods.size(),
                this.excludedOutputMods.size(),
                this.allEntries.size());
        guiGraphics.drawString(this.font, stat, this.leftPos + 16, this.topPos + 45, 0x666666, false);

        drawEntryRows(guiGraphics, mouseX, mouseY);
        drawScrollBar(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        ModEntry hovered = getEntryAt(mouseX, mouseY);
        if (hovered != null) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal(hovered.modId() + " - " + hovered.name()));
            tooltip.add(Component.translatable("gui.techstart.mod_filter.tooltip.left"));
            tooltip.add(Component.translatable("gui.techstart.mod_filter.tooltip.right"));
            Component inputState = Component.translatable(this.excludedInputMods.contains(hovered.modId())
                    ? "gui.techstart.mod_filter.state.excluded"
                    : "gui.techstart.mod_filter.state.allowed");
            Component outputState = Component.translatable(this.excludedOutputMods.contains(hovered.modId())
                    ? "gui.techstart.mod_filter.state.excluded"
                    : "gui.techstart.mod_filter.state.allowed");
            tooltip.add(Component.translatable("gui.techstart.mod_filter.tooltip.state", inputState, outputState));
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
        for (var modInfo : ModList.get().getMods()) {
            String modId = normalizeModId(modInfo.getModId());
            if (modId.isBlank()) {
                continue;
            }
            String displayName = modInfo.getDisplayName();
            if (displayName == null || displayName.isBlank()) {
                displayName = modId;
            }
            this.allEntries.add(new ModEntry(modId, displayName));
        }
        this.allEntries.sort(Comparator.comparing(ModEntry::modId));
    }

    private void applyFilter() {
        this.filteredEntries.clear();
        String keyword = this.searchField == null ? "" : this.searchField.getValue().trim().toLowerCase(Locale.ROOT);
        for (ModEntry entry : this.allEntries) {
            if (keyword.isEmpty() || entry.matches(keyword)) {
                this.filteredEntries.add(entry);
            }
        }
        this.listScroll = Mth.clamp(this.listScroll, 0, getMaxScroll());
    }

    private void toggleInput(String modId) {
        if (!this.excludedInputMods.add(modId)) {
            this.excludedInputMods.remove(modId);
        }
        syncExcludedMods();
    }

    private void toggleOutput(String modId) {
        if (!this.excludedOutputMods.add(modId)) {
            this.excludedOutputMods.remove(modId);
        }
        syncExcludedMods();
    }

    private void syncExcludedMods() {
        List<String> inputValues = new ArrayList<>(this.excludedInputMods);
        List<String> outputValues = new ArrayList<>(this.excludedOutputMods);
        this.menu.applyExcludedModFilters(inputValues.toArray(new String[0]), outputValues.toArray(new String[0]));
        TechStartNetwork.sendToServer(new SetPatternModFiltersPayload(inputValues, outputValues));
    }


    private void drawEntryRows(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int left = getListLeft();
        int top = getListTop();
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = this.listScroll + row;
            if (index >= this.filteredEntries.size()) {
                return;
            }
            ModEntry entry = this.filteredEntries.get(index);
            int rowY = top + row * ROW_HEIGHT;
            boolean hovered = mouseX >= left && mouseX < left + LIST_WIDTH && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;

            boolean inputExcluded = this.excludedInputMods.contains(entry.modId());
            boolean outputExcluded = this.excludedOutputMods.contains(entry.modId());
            if (inputExcluded && outputExcluded) {
                guiGraphics.fill(left, rowY, left + LIST_WIDTH, rowY + ROW_HEIGHT, 0x446633CC);
            } else if (inputExcluded) {
                guiGraphics.fill(left, rowY, left + LIST_WIDTH, rowY + ROW_HEIGHT, 0x44CC3333);
            } else if (outputExcluded) {
                guiGraphics.fill(left, rowY, left + LIST_WIDTH, rowY + ROW_HEIGHT, 0x443366CC);
            }
            if (hovered) {
                guiGraphics.fill(left, rowY, left + LIST_WIDTH, rowY + ROW_HEIGHT, 0x33FFFFFF);
            }

            String marker = (inputExcluded ? "I" : "-") + "/" + (outputExcluded ? "O" : "-");
            String line = this.font.plainSubstrByWidth(marker + " " + entry.modId() + " - " + entry.name(), LIST_WIDTH - 4);
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
        return mouseX >= barLeft && mouseX < barLeft + SCROLLBAR_WIDTH
                && mouseY >= barTop && mouseY < barTop + barHeight;
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

    private ModEntry getEntryAt(double mouseX, double mouseY) {
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

    private static String normalizeModId(String modId) {
        return modId == null ? "" : modId.trim().toLowerCase(Locale.ROOT);
    }

    private record ModEntry(String modId, String name) {
        private boolean matches(String keyword) {
            return modId.toLowerCase(Locale.ROOT).contains(keyword)
                    || name.toLowerCase(Locale.ROOT).contains(keyword);
        }
    }
}
