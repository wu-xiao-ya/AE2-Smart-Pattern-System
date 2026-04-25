package com.wuxiaoya.techstart.client;

import appeng.api.crafting.IPatternDetails;
import com.mojang.blaze3d.systems.RenderSystem;
import com.wuxiaoya.techstart.TechStartNeoForge;
import com.wuxiaoya.techstart.integration.ae2.TechStartPatternExpansion;
import com.wuxiaoya.techstart.integration.mekanism.MekanismGasHelper;
import com.wuxiaoya.techstart.menu.PatternEditorMenu;
import com.wuxiaoya.techstart.network.SetPatternSlotAmountPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.inventory.InventoryMenu;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PatternEditorScreen extends AbstractContainerScreen<PatternEditorMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TechStartNeoForge.MODID, "textures/gui/pattern_all.png");
    private static final String TAG_FLUID_MARKER = "TechStartFluidMarker";
    private static final String TAG_FLUID_NAME = "TechStartFluidName";
    private static final String TAG_FLUID_AMOUNT = "TechStartFluidAmount";
    private static final String TAG_GAS_MARKER = "TechStartGasMarker";
    private static final String TAG_GAS_NAME = "TechStartGasName";
    private static final String TAG_GAS_AMOUNT = "TechStartGasAmount";
    private static final String TAG_ITEM_MARKER = "TechStartItemMarker";
    private static final String TAG_ITEM_AMOUNT = "TechStartItemAmount";
    private static final String TAG_INPUTS = "TechStartInputs";
    private static final String TAG_OUTPUTS = "TechStartOutputs";
    private static final String TAG_STACK = "Stack";
    private static final String TAG_VIRTUAL_INPUT_STACKS = "VirtualInputStacks";
    private static final String TAG_VIRTUAL_OUTPUT_STACKS = "VirtualOutputStacks";
    private static final String TAG_VIRTUAL_FILTER_ENTRY_ID = "VirtualFilterEntryId";
    private static final int LIST_ROWS = 3;
    private static final int LIST_COLS = 3;
    private static final int SLOT_SIZE = 18;
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
    private static final int INPUT_X = 24;
    private static final int INPUT_Y = 35;
    private static final int OUTPUT_X = 96;
    private static final int OUTPUT_Y = 35;
    private static final int EDITOR_X = SIDE_BUTTON_X;
    private static final int EDITOR_Y = 0;
    private static final int FILTER_X = SIDE_BUTTON_X;
    private static final int FILTER_Y = 24;
    private static final int MODE_X = SIDE_BUTTON_X;
    private static final int MODE_Y = 48;
    private static final int SEARCH_X = SIDE_BUTTON_X;
    private static final int SEARCH_Y = 72;
    private static final int MOD_FILTER_X = SIDE_BUTTON_X;
    private static final int MOD_FILTER_Y = 96;
    private static final int SEARCH_HIGHLIGHT_COLOR = 0x66FFFF00;

    private InvisibleButton editorButton;
    private InvisibleButton switchButton;
    private InvisibleButton modeButton;
    private InvisibleButton searchButton;
    private InvisibleButton modFilterButton;
    private boolean filterView = false;
    private int listScroll = 0;
    private final Set<String> selectedFilterEntries = new LinkedHashSet<>();
    private final Set<String> highlightedFilterEntryIds = new LinkedHashSet<>();
    private String pendingSearchJumpId;

    public PatternEditorScreen(PatternEditorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = SIDE_BUTTON_X + SIDE_BUTTON_WIDTH;
        this.imageHeight = PANEL_HEIGHT;
        this.inventoryLabelY = 110;
    }

    @Override
    protected void init() {
        super.init();
        this.editorButton = this.addRenderableWidget(new InvisibleButton(
                this.leftPos + EDITOR_X,
                this.topPos + EDITOR_Y,
                SIDE_BUTTON_WIDTH,
                SIDE_BUTTON_HEIGHT,
                Component.literal("editor"),
                Component.translatable("gui.techstart.switch_to_editor"),
                ignored -> {
                    this.filterView = false;
                    this.listScroll = 0;
                    updateModeButtons();
                }
        ));
        this.editorButton.setSprite(SWITCH_TO_EDITOR_BUTTON_U, SWITCH_TO_EDITOR_BUTTON_V, SWITCH_TO_EDITOR_BUTTON_V, ATLAS_WIDTH, ATLAS_HEIGHT);
        this.modeButton = this.addRenderableWidget(new InvisibleButton(
                this.leftPos + MODE_X,
                this.topPos + MODE_Y,
                SIDE_BUTTON_WIDTH,
                SIDE_BUTTON_HEIGHT,
                Component.literal("mode"),
                Component.translatable("gui.techstart.toggle_filter_mode"),
                ignored -> {
                    int mode = this.menu.getFilterMode();
                    int next = mode == PatternEditorMenu.FILTER_MODE_WHITELIST
                            ? PatternEditorMenu.FILTER_MODE_BLACKLIST
                            : PatternEditorMenu.FILTER_MODE_WHITELIST;
                    sendMenuButton(next);
                    this.listScroll = 0;
                    this.selectedFilterEntries.clear();
                    updateModeButtons();
                }
        ));
        this.modeButton.setSprite(MODE_BUTTON_U, MODE_BUTTON_V, MODE_BUTTON_V, ATLAS_WIDTH, ATLAS_HEIGHT);
        this.searchButton = this.addRenderableWidget(new InvisibleButton(
                this.leftPos + SEARCH_X,
                this.topPos + SEARCH_Y,
                SIDE_BUTTON_WIDTH,
                SIDE_BUTTON_HEIGHT,
                Component.literal("search"),
                Component.translatable("gui.techstart.open_search"),
                ignored -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new PatternSearchScreen(this, this.menu));
                    }
                }
        ));
        this.searchButton.setSprite(SEARCH_BUTTON_U, SEARCH_BUTTON_V, SEARCH_BUTTON_V, ATLAS_WIDTH, ATLAS_HEIGHT);
        this.modFilterButton = this.addRenderableWidget(new InvisibleButton(
                this.leftPos + MOD_FILTER_X,
                this.topPos + MOD_FILTER_Y,
                SIDE_BUTTON_WIDTH,
                SIDE_BUTTON_HEIGHT,
                Component.literal("mod"),
                Component.translatable("gui.techstart.open_mod_filter"),
                ignored -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new ModFilterScreen(this, this.menu));
                    }
                }
        ));
        this.modFilterButton.setSprite(MOD_FILTER_BUTTON_U, MOD_FILTER_BUTTON_V, MOD_FILTER_BUTTON_V, ATLAS_WIDTH, ATLAS_HEIGHT);
        this.switchButton = this.addRenderableWidget(new InvisibleButton(
                this.leftPos + FILTER_X,
                this.topPos + FILTER_Y,
                SIDE_BUTTON_WIDTH,
                SIDE_BUTTON_HEIGHT,
                Component.literal("filter"),
                Component.translatable("gui.techstart.switch_to_filter"),
                ignored -> {
                    this.menu.refreshPatternStackSnapshot();
                    this.selectedFilterEntries.clear();
                    this.selectedFilterEntries.addAll(this.menu.getFilterEntriesSnapshot());
                    this.highlightedFilterEntryIds.clear();
                    this.filterView = true;
                    this.listScroll = 0;
                    updateModeButtons();
                }
        ));
        this.switchButton.setSprite(SWITCH_TO_FILTER_BUTTON_U, SWITCH_TO_FILTER_BUTTON_V, SWITCH_TO_FILTER_BUTTON_V, ATLAS_WIDTH, ATLAS_HEIGHT);
        this.selectedFilterEntries.clear();
        this.selectedFilterEntries.addAll(this.menu.getFilterEntriesSnapshot());
        this.highlightedFilterEntryIds.clear();
        if (this.pendingSearchJumpId != null) {
            applySearchJump(this.pendingSearchJumpId);
            this.pendingSearchJumpId = null;
        }
        updateModeButtons();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateModeButtons();
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBg(guiGraphics, partialTick, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component title = this.filterView
                ? Component.translatable("gui.techstart.pattern_filter")
                : this.title;
        guiGraphics.drawString(this.font, title, (PANEL_WIDTH - this.font.width(title)) / 2, 2, 0x404040, false);
        if (!this.filterView) {
            guiGraphics.drawString(this.font, Component.translatable("gui.techstart.input"), 38, 20, 0x404040, false);
            guiGraphics.drawString(this.font, Component.translatable("gui.techstart.output"), 109, 20, 0x404040, false);
        } else {
            guiGraphics.drawString(this.font, Component.translatable("gui.techstart.mode", getModeComponent()), 26, 20, 0x404040, false);
        }
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        updatePatternSlotVisibility();
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (this.filterView) {
            redrawFilterSlotBackgrounds(guiGraphics);
            drawFilterOverlay(guiGraphics);
            renderFilterTooltip(guiGraphics, mouseX, mouseY);
        } else {
            renderPatternMarkerMaterials(guiGraphics);
            renderMarkerAmounts(guiGraphics);
        }
        renderButtonTooltips(guiGraphics, mouseX, mouseY);
        if (!this.filterView) {
            this.renderTooltip(guiGraphics, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (this.filterView) {
            if (handleFilterClick(mouseX, mouseY, mouseButton)) {
                return true;
            }
            if (isWithinPatternArea(mouseX, mouseY)) {
                return true;
            }
        }
        if (!this.filterView && mouseButton == 2) {
            Slot slot = this.getSlotUnderMouse();
            int slotId = resolvePatternSlotId(slot);
            if (slotId >= 0 && slot != null && slot.hasItem() && this.minecraft != null) {
                this.minecraft.setScreen(new PatternAmountInputScreen(
                        this,
                        slotId,
                        getLogicalSlotAmount(slot.getItem()),
                        getEditableAmountLimit(slot.getItem()),
                        Component.translatable("gui.techstart.amount.set")
                ));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.filterView) {
            return;
        }
        Slot slot = this.getSlotUnderMouse();
        List<Component> customTooltip = buildPatternSlotTooltip(slot);
        if (!customTooltip.isEmpty()) {
            List<FormattedCharSequence> lines = customTooltip.stream()
                    .map(Component::getVisualOrderText)
                    .toList();
            guiGraphics.renderTooltip(this.font, lines, mouseX, mouseY);
            return;
        }
        super.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.filterView && scrollY != 0 && isWithinPatternArea(mouseX, mouseY)) {
            int maxScroll = getMaxScroll(getFilterEntries().size());
            if (maxScroll > 0) {
                int step = scrollY > 0 ? -1 : 1;
                this.listScroll = Mth.clamp(this.listScroll + step, 0, maxScroll);
                return true;
            }
        }
        if (!this.filterView && scrollY != 0) {
            Slot slot = this.getSlotUnderMouse();
            int slotId = resolvePatternSlotId(slot);
            if (slotId >= 0 && slot != null && slot.hasItem()) {
                int current = getLogicalSlotAmount(slot.getItem());
                int step = hasShiftDown() ? 10 : 1;
                int max = getEditableAmountLimit(slot.getItem());
                int target = Mth.clamp(current + (scrollY > 0 ? step : -step), 1, max);
                if (target != current) {
                    applyPatternSlotAmount(slotId, target);
                }
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void sendMenuButton(int buttonId) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        }
    }

    private void updatePatternSlotVisibility() {
        boolean visible = !this.filterView;
        for (int index = 0; index < 18 && index < this.menu.slots.size(); index++) {
            Slot slot = this.menu.slots.get(index);
            if (slot instanceof PatternEditorMenu.PatternSlot patternSlot) {
                patternSlot.setActive(visible);
            }
        }
    }

    private void updateModeButtons() {
        if (this.editorButton == null || this.switchButton == null || this.modeButton == null || this.searchButton == null || this.modFilterButton == null) {
            return;
        }
        this.editorButton.visible = true;
        this.editorButton.active = true;
        this.editorButton.setHint(Component.translatable("gui.techstart.switch_to_editor"));
        this.switchButton.visible = true;
        this.switchButton.active = true;
        this.switchButton.setHint(Component.translatable("gui.techstart.switch_to_filter"));
        this.modeButton.visible = true;
        this.modeButton.active = true;
        this.modeButton.setMessage(getModeComponent());
        this.modeButton.setHint(Component.translatable("gui.techstart.toggle_filter_mode"));
        this.searchButton.visible = true;
        this.searchButton.active = true;
        this.searchButton.setHint(Component.translatable("gui.techstart.open_search"));
        this.modFilterButton.visible = true;
        this.modFilterButton.active = true;
        this.modFilterButton.setHint(Component.translatable("gui.techstart.open_mod_filter"));
        updatePatternSlotVisibility();
    }

    private Component getModeComponent() {
        return this.menu.getFilterMode() == PatternEditorMenu.FILTER_MODE_WHITELIST
                ? Component.translatable("gui.techstart.filter_mode_whitelist")
                : Component.translatable("gui.techstart.filter_mode_blacklist");
    }

    public void openFilterViewForEntry(String entryId) {
        this.menu.refreshPatternStackSnapshot();
        this.selectedFilterEntries.clear();
        this.selectedFilterEntries.addAll(this.menu.getFilterEntriesSnapshot());
        this.filterView = true;
        this.pendingSearchJumpId = entryId;
        applySearchJump(entryId);
        updateModeButtons();
    }

    private void applySearchJump(String entryId) {
        this.highlightedFilterEntryIds.clear();
        if (entryId == null || entryId.isBlank()) {
            this.listScroll = 0;
            return;
        }
        List<FilterEntry> entries = getFilterEntries();
        List<Integer> matchedIndices = new ArrayList<>();
        for (int index = 0; index < entries.size(); index++) {
            if (matchesSearchJump(entries.get(index), entryId)) {
                matchedIndices.add(index);
            }
        }
        if (matchedIndices.isEmpty()) {
            this.listScroll = 0;
            return;
        }
        int targetPage = matchedIndices.get(0) / 9;
        this.listScroll = Mth.clamp(targetPage, 0, getMaxScroll(entries.size()));
        for (Integer index : matchedIndices) {
            if (index == null || index / 9 != targetPage) {
                continue;
            }
            FilterEntry entry = entries.get(index);
            if (entry.id() != null && !entry.id().isBlank()) {
                this.highlightedFilterEntryIds.add(entry.id());
            }
            if (entry.legacyId() != null && !entry.legacyId().isBlank()) {
                this.highlightedFilterEntryIds.add(entry.legacyId());
            }
        }
    }

    private boolean matchesSearchJump(FilterEntry entry, String entryId) {
        if (entry == null || entryId == null || entryId.isBlank()) {
            return false;
        }
        String id = entry.id();
        String legacyId = entry.legacyId();
        if (entryId.equals(id) || entryId.equals(legacyId)) {
            return true;
        }
        return id != null && id.startsWith(entryId + "|");
    }

    private boolean isFilterEntryHighlighted(FilterEntry entry) {
        if (entry == null || this.highlightedFilterEntryIds.isEmpty()) {
            return false;
        }
        String id = entry.id();
        String legacyId = entry.legacyId();
        return (id != null && this.highlightedFilterEntryIds.contains(id))
                || (legacyId != null && this.highlightedFilterEntryIds.contains(legacyId));
    }

    private void redrawFilterSlotBackgrounds(GuiGraphics guiGraphics) {
        int gridWidth = LIST_COLS * SLOT_SIZE;
        int gridHeight = LIST_ROWS * SLOT_SIZE;
        guiGraphics.blit(TEXTURE, this.leftPos + INPUT_X, this.topPos + INPUT_Y, INPUT_X, INPUT_Y, gridWidth, gridHeight, ATLAS_WIDTH, ATLAS_HEIGHT);
        guiGraphics.blit(TEXTURE, this.leftPos + OUTPUT_X, this.topPos + OUTPUT_Y, OUTPUT_X, OUTPUT_Y, gridWidth, gridHeight, ATLAS_WIDTH, ATLAS_HEIGHT);
    }

    private void drawFilterOverlay(GuiGraphics guiGraphics) {
        List<FilterEntry> entries = getFilterEntries();
        int maxScroll = getMaxScroll(entries.size());
        if (this.listScroll > maxScroll) {
            this.listScroll = maxScroll;
        }
        int startIndex = this.listScroll * 9;
        int mode = this.menu.getFilterMode();

        for (int row = 0; row < LIST_ROWS; row++) {
            for (int col = 0; col < LIST_COLS; col++) {
                int entryIndex = startIndex + row * LIST_COLS + col;
                if (entryIndex >= entries.size()) {
                    continue;
                }
                FilterEntry entry = entries.get(entryIndex);
                int inputX = this.leftPos + INPUT_X + col * SLOT_SIZE;
                int inputY = this.topPos + INPUT_Y + row * SLOT_SIZE;
                int outputX = this.leftPos + OUTPUT_X + col * SLOT_SIZE;
                int outputY = this.topPos + OUTPUT_Y + row * SLOT_SIZE;
                boolean highlighted = isFilterEntryHighlighted(entry);
                if (highlighted) {
                    guiGraphics.fill(inputX, inputY, inputX + 16, inputY + 16, SEARCH_HIGHLIGHT_COLOR);
                    guiGraphics.fill(outputX, outputY, outputX + 16, outputY + 16, SEARCH_HIGHLIGHT_COLOR);
                }
                renderFilterEntryStack(guiGraphics, entry.input(), inputX, inputY);
                renderFilterEntryStack(guiGraphics, entry.output(), outputX, outputY);
                if (isFilterEntrySelected(entry)) {
                    int color = mode == PatternEditorMenu.FILTER_MODE_BLACKLIST ? 0x66FF0000 : 0x6600FF00;
                    guiGraphics.fill(inputX, inputY, inputX + 16, inputY + 16, color);
                    guiGraphics.fill(outputX, outputY, outputX + 16, outputY + 16, color);
                }
            }
        }
        if (maxScroll > 0) {
            drawScrollBar(guiGraphics, this.leftPos + OUTPUT_X + LIST_COLS * SLOT_SIZE + 2, this.topPos + INPUT_Y, entries.size(), maxScroll);
        }
    }

    private void drawScrollBar(GuiGraphics guiGraphics, int x, int y, int total, int maxScroll) {
        int height = LIST_ROWS * SLOT_SIZE;
        int barHeight = Math.max(12, height / 3);
        int barY = y;
        if (maxScroll > 0 && total > 0) {
            float ratio = this.listScroll / (float) maxScroll;
            barY = y + Math.round((height - barHeight) * ratio);
        }
        guiGraphics.fill(x, y, x + 6, y + height, 0xFFCCCCCC);
        guiGraphics.fill(x, barY, x + 6, barY + barHeight, 0xFF888888);
    }

    private void renderFilterEntryStack(GuiGraphics guiGraphics, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) {
            return;
        }
        boolean renderedCustom = renderMarkerMaterial(guiGraphics, stack, x, y);
        if (!renderedCustom) {
            guiGraphics.renderItem(stack, x, y);
            guiGraphics.renderItemDecorations(this.font, stack, x, y);
        }
        if (isAmountMarkerStack(stack)) {
            renderAmountText(guiGraphics, x, y, getLogicalSlotAmount(stack));
        }
    }

    private boolean handleFilterClick(double mouseX, double mouseY, int mouseButton) {
        if (mouseButton != 0 && mouseButton != 1) {
            return false;
        }
        List<FilterEntry> entries = getFilterEntries();
        int maxScroll = getMaxScroll(entries.size());
        if (this.listScroll > maxScroll) {
            this.listScroll = maxScroll;
        }
        int entryIndex = resolveEntryIndex(mouseX, mouseY, entries.size());
        if (entryIndex < 0 || entryIndex >= entries.size()) {
            int scrollX = this.leftPos + OUTPUT_X + LIST_COLS * SLOT_SIZE + 2;
            int listTop = this.topPos + INPUT_Y;
            int listHeight = LIST_ROWS * SLOT_SIZE;
            if (maxScroll > 0 && mouseX >= scrollX && mouseX < scrollX + 6 && mouseY >= listTop && mouseY < listTop + listHeight) {
                if (maxScroll > 0) {
                    float ratio = (float) ((mouseY - listTop) / listHeight);
                    this.listScroll = Mth.clamp(Math.round(ratio * maxScroll), 0, maxScroll);
                }
                return true;
            }
            return false;
        }

        FilterEntry entry = entries.get(entryIndex);
        boolean contains = this.selectedFilterEntries.contains(entry.id());
        if ((mouseButton == 0 && !contains) || (mouseButton == 1 && contains)) {
            if (!this.selectedFilterEntries.add(entry.id())) {
                this.selectedFilterEntries.remove(entry.id());
            }
            sendMenuButton(PatternEditorMenu.encodeFilterEntryButtonId(entryIndex));
        }
        return true;
    }

    private boolean isFilterEntrySelected(FilterEntry entry) {
        if (entry == null || this.selectedFilterEntries.isEmpty()) {
            return false;
        }
        if (entry.id() != null && this.selectedFilterEntries.contains(entry.id())) {
            return true;
        }
        String legacyId = entry.legacyId();
        if (legacyId == null || legacyId.isBlank()) {
            return false;
        }
        String prefix = legacyId + "|";
        for (String selected : this.selectedFilterEntries) {
            if (selected != null && selected.startsWith(prefix)) {
                return false;
            }
        }
        return this.selectedFilterEntries.contains(legacyId);
    }

    private void renderFilterTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        List<FilterEntry> entries = getFilterEntries();
        int entryIndex = resolveEntryIndex(mouseX, mouseY, entries.size());
        if (entryIndex < 0 || entryIndex >= entries.size()) {
            return;
        }
        List<Component> detailLines = buildDetailLines(entries.get(entryIndex));
        if (detailLines.isEmpty()) {
            return;
        }
        List<FormattedCharSequence> lines = detailLines.stream()
                .map(Component::getVisualOrderText)
                .toList();
        guiGraphics.renderTooltip(this.font, lines, mouseX, mouseY);
    }

    private void renderButtonTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
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

    private List<FilterEntry> getFilterEntries() {
        ItemStack patternStack = this.menu.getPatternStackSnapshot();
        if (!patternStack.isEmpty() && this.minecraft != null && this.minecraft.level != null) {
            List<FilterEntry> expanded = new ArrayList<>();
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
                    String legacyId = getLegacyPairId(id, input, output);
                    expanded.add(new FilterEntry(id, legacyId, input, output, variantStack));
                }
            } catch (Throwable ignored) {
                expanded.clear();
            }
            if (!expanded.isEmpty()) {
                return expanded;
            }
        }

        List<ItemStack> inputs = new ArrayList<>();
        List<ItemStack> outputs = new ArrayList<>();
        for (int index = 0; index < 9; index++) {
            ItemStack stack = this.menu.slots.get(index).getItem();
            if (!stack.isEmpty()) {
                inputs.add(stack.copy());
            }
        }
        for (int index = 9; index < 18; index++) {
            ItemStack stack = this.menu.slots.get(index).getItem();
            if (!stack.isEmpty()) {
                outputs.add(stack.copy());
            }
        }

        List<FilterEntry> entries = new ArrayList<>();
        if (inputs.isEmpty() || outputs.isEmpty()) {
            return entries;
        }
        for (ItemStack input : inputs) {
            for (ItemStack output : outputs) {
                String id = PatternEditorMenu.buildFilterEntryId(input, output);
                entries.add(new FilterEntry(id, id, input, output, ItemStack.EMPTY));
            }
        }
        return entries;
    }

    private List<Component> buildDetailLines(FilterEntry hoveredEntry) {
        List<Component> lines = new ArrayList<>();
        ItemStack detailPattern = hoveredEntry.pattern().isEmpty() ? this.menu.getPatternStackSnapshot() : hoveredEntry.pattern();
        appendStoredItemLines(lines, detailPattern, true);
        appendLegacyAmountLines(lines, detailPattern, true, "InputFluids", "InputFluidAmounts", "tooltip.techstart.input_fluid");
        appendLegacyAmountLines(lines, detailPattern, true, "InputGases", "InputGasAmounts", "tooltip.techstart.input_gas");
        appendStoredItemLines(lines, detailPattern, false);
        appendLegacyAmountLines(lines, detailPattern, false, "OutputFluids", "OutputFluidAmounts", "tooltip.techstart.output_fluid");
        appendLegacyAmountLines(lines, detailPattern, false, "OutputGases", "OutputGasAmounts", "tooltip.techstart.output_gas");

        if (lines.isEmpty()) {
            lines.add(Component.translatable("tooltip.techstart.input_item", 1, hoveredEntry.input().getHoverName(), Math.max(1, getLogicalSlotAmount(hoveredEntry.input())))
                    .withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("tooltip.techstart.output_item", 1, hoveredEntry.output().getHoverName(), Math.max(1, getLogicalSlotAmount(hoveredEntry.output())))
                    .withStyle(ChatFormatting.GRAY));
        }

        return lines;
    }

    private void appendStoredItemLines(List<Component> lines, ItemStack patternStack, boolean input) {
        String key = input ? "tooltip.techstart.input_item" : "tooltip.techstart.output_item";
        int index = 1;
        for (ItemStack stack : readStoredStacks(patternStack, input)) {
            if (stack.isEmpty() || isFluidMarkerStack(stack) || isGasMarkerStack(stack)) {
                continue;
            }
            lines.add(Component.translatable(key, index, stack.getHoverName(), getLogicalSlotAmount(stack))
                    .withStyle(ChatFormatting.GRAY));
            index++;
        }
    }

    private List<ItemStack> readStoredStacks(ItemStack patternStack, boolean input) {
        List<ItemStack> stacks = new ArrayList<>();
        if (patternStack.isEmpty() || this.minecraft == null || this.minecraft.level == null) {
            return stacks;
        }
        CompoundTag tag = patternStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.isEmpty()) {
            return stacks;
        }
        String virtualKey = input ? TAG_VIRTUAL_INPUT_STACKS : TAG_VIRTUAL_OUTPUT_STACKS;
        if (tag.contains(virtualKey, Tag.TAG_LIST)) {
            ListTag list = tag.getList(virtualKey, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                ItemStack stack = ItemStack.parseOptional(this.minecraft.level.registryAccess(), list.getCompound(i));
                if (!stack.isEmpty()) {
                    stacks.add(stack);
                }
            }
            return stacks;
        }
        String modernKey = input ? TAG_INPUTS : TAG_OUTPUTS;
        if (tag.contains(modernKey, Tag.TAG_LIST)) {
            ListTag list = tag.getList(modernKey, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                if (!entry.contains(TAG_STACK, Tag.TAG_COMPOUND)) {
                    continue;
                }
                ItemStack stack = ItemStack.parseOptional(this.minecraft.level.registryAccess(), entry.getCompound(TAG_STACK));
                if (!stack.isEmpty()) {
                    stacks.add(stack);
                }
            }
        }
        return stacks;
    }

    private void appendLegacyAmountLines(List<Component> lines, ItemStack pattern, boolean input, String listKey, String amountKey, String lineKey) {
        if (pattern.isEmpty()) {
            return;
        }
        CompoundTag tag = pattern.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.isEmpty() || !tag.contains(listKey, Tag.TAG_LIST)) {
            return;
        }

        ListTag names = tag.getList(listKey, Tag.TAG_STRING);
        ListTag amounts = tag.contains(amountKey, Tag.TAG_LIST) ? tag.getList(amountKey, Tag.TAG_INT) : new ListTag();
        int index = 1;
        for (int i = 0; i < names.size(); i++) {
            String name = names.getString(i);
            if (name == null || name.isBlank()) {
                continue;
            }
            int amount = i < amounts.size() ? amounts.getInt(i) : 1;
            Object displayName = lineKey.contains("fluid")
                    ? getFluidDisplayName(name, Math.max(1, amount))
                    : getGasDisplayName(name);
            lines.add(Component.translatable(lineKey, index, displayName, Math.max(1, amount))
                    .withStyle(input ? ChatFormatting.DARK_AQUA : ChatFormatting.BLUE));
            index++;
        }
    }

    private ItemStack getPrimaryPatternStack(ItemStack patternStack, boolean input) {
        List<ItemStack> stacks = readStoredStacks(patternStack, input);
        return stacks.isEmpty() ? ItemStack.EMPTY : stacks.get(0).copy();
    }

    private String getFilterEntryId(ItemStack patternStack, ItemStack input, ItemStack output) {
        if (!patternStack.isEmpty()) {
            CompoundTag tag = patternStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (!tag.isEmpty()) {
                String id = tag.getString(TAG_VIRTUAL_FILTER_ENTRY_ID);
                if (id != null && !id.isBlank()) {
                    return id;
                }
            }
        }
        return PatternEditorMenu.buildFilterEntryId(input, output);
    }

    private String getLegacyPairId(String id, ItemStack input, ItemStack output) {
        if (id != null && id.contains("|")) {
            return id.substring(0, id.indexOf('|'));
        }
        return PatternEditorMenu.buildFilterEntryId(input, output);
    }

    private int getMaxScroll(int entryCount) {
        return Math.max(0, (int) Math.ceil(entryCount / 9.0D) - 1);
    }

    private int resolveEntryIndex(double mouseX, double mouseY, int totalEntries) {
        int localX = Mth.floor(mouseX) - this.leftPos;
        int localY = Mth.floor(mouseY) - this.topPos;
        int listHeight = LIST_ROWS * SLOT_SIZE;
        int startIndex = this.listScroll * 9;

        if (localX >= INPUT_X && localX < INPUT_X + LIST_COLS * SLOT_SIZE && localY >= INPUT_Y && localY < INPUT_Y + listHeight) {
            int col = (localX - INPUT_X) / SLOT_SIZE;
            int row = (localY - INPUT_Y) / SLOT_SIZE;
            int idx = startIndex + row * LIST_COLS + col;
            return idx < totalEntries ? idx : -1;
        }

        if (localX >= OUTPUT_X && localX < OUTPUT_X + LIST_COLS * SLOT_SIZE && localY >= OUTPUT_Y && localY < OUTPUT_Y + listHeight) {
            int col = (localX - OUTPUT_X) / SLOT_SIZE;
            int row = (localY - OUTPUT_Y) / SLOT_SIZE;
            int idx = startIndex + row * LIST_COLS + col;
            return idx < totalEntries ? idx : -1;
        }

        return -1;
    }

    private boolean isWithinPatternArea(double mouseX, double mouseY) {
        int localX = Mth.floor(mouseX) - this.leftPos;
        int localY = Mth.floor(mouseY) - this.topPos;
        int listHeight = LIST_ROWS * SLOT_SIZE;
        boolean inputArea = localX >= INPUT_X && localX < INPUT_X + LIST_COLS * SLOT_SIZE
                && localY >= INPUT_Y && localY < INPUT_Y + listHeight;
        boolean outputArea = localX >= OUTPUT_X && localX < OUTPUT_X + LIST_COLS * SLOT_SIZE
                && localY >= OUTPUT_Y && localY < OUTPUT_Y + listHeight;
        return inputArea || outputArea;
    }

    private List<Component> buildPatternSlotTooltip(Slot slot) {
        if (slot == null || resolvePatternSlotId(slot) < 0 || !slot.hasItem()) {
            return List.of();
        }
        return buildStackTooltip(slot.getItem());
    }

    private List<Component> buildStackTooltip(ItemStack stack) {
        if (stack.isEmpty()) {
            return List.of();
        }
        int amount = getLogicalSlotAmount(stack);
        if (isFluidMarkerStack(stack)) {
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            String fluidId = tag.getString(TAG_FLUID_NAME);
            return List.of(
                    getFluidDisplayName(fluidId, amount),
                    Component.literal(amount + " mB").withStyle(ChatFormatting.GRAY)
            );
        }
        if (isGasMarkerStack(stack)) {
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            String gasId = tag.getString(TAG_GAS_NAME);
            return List.of(
                    getGasDisplayName(gasId),
                    Component.literal(amount + " mB").withStyle(ChatFormatting.GRAY)
            );
        }
        MekanismGasHelper.GasStackView gas = MekanismGasHelper.extractGas(stack);
        if (gas != null && gas.gasId() != null && !gas.gasId().isBlank()) {
            return List.of(
                    gas.displayName(),
                    Component.literal(Math.max(1, gas.amount()) + " mB").withStyle(ChatFormatting.GRAY)
            );
        }
        FluidStack contained = FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
        if (!contained.isEmpty()) {
            int fluidAmount = Math.max(1, contained.getAmount());
            return List.of(
                    contained.getHoverName(),
                    Component.literal(fluidAmount + " mB").withStyle(ChatFormatting.GRAY)
            );
        }
        if (stack.getItem() instanceof BucketItem bucketItem && bucketItem.content != Fluids.EMPTY) {
            return List.of(
                    getFluidDisplayName(BuiltInRegistries.FLUID.getKey(bucketItem.content).toString(), 1000),
                    Component.literal("1000 mB").withStyle(ChatFormatting.GRAY)
            );
        }
        return List.of();
    }

    public void applyPatternSlotAmount(int slotId, int amount) {
        if (!isPatternSlot(slotId)) {
            return;
        }
        Slot slot = this.menu.slots.get(slotId);
        if (!slot.hasItem()) {
            return;
        }

        PacketDistributor.sendToServer(new SetPatternSlotAmountPayload(slotId, amount));
        ItemStack updated = slot.getItem().copy();
        CompoundTag tag = updated.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int clamped = Mth.clamp(amount, 1, this.menu.getAmountLimitForStack(updated));
        if (isFluidMarkerStack(updated)) {
            tag.putBoolean(TAG_FLUID_MARKER, true);
            tag.putInt(TAG_FLUID_AMOUNT, clamped);
        } else if (isGasMarkerStack(updated)) {
            tag.putBoolean(TAG_GAS_MARKER, true);
            tag.putInt(TAG_GAS_AMOUNT, clamped);
        } else {
            tag.putBoolean(TAG_ITEM_MARKER, true);
            tag.putInt(TAG_ITEM_AMOUNT, clamped);
        }
        CustomData.set(DataComponents.CUSTOM_DATA, updated, tag);
        updated.setCount(1);
        slot.set(updated);
        this.menu.refreshPatternStackSnapshot();
    }

    private void renderPatternMarkerMaterials(GuiGraphics guiGraphics) {
        for (int slotId = 0; slotId < 18 && slotId < this.menu.slots.size(); slotId++) {
            Slot slot = this.menu.slots.get(slotId);
            if (!slot.hasItem()) {
                continue;
            }
            renderMarkerMaterial(guiGraphics, slot.getItem(), this.leftPos + slot.x, this.topPos + slot.y);
        }
    }

    private boolean renderMarkerMaterial(GuiGraphics guiGraphics, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) {
            return false;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0D, 0.0D, 250.0D);
        RenderSystem.disableDepthTest();
        boolean rendered = false;
        if (isFluidMarkerStack(stack)) {
            rendered = renderFluidMarkerMaterial(guiGraphics, stack, x, y);
        } else if (isGasMarkerStack(stack)) {
            rendered = renderGasMarkerMaterial(guiGraphics, stack, x, y);
        } else {
            if (renderGasFromContainerFallback(guiGraphics, stack, x, y)) {
                rendered = true;
            } else if (renderFluidFromContainerFallback(guiGraphics, stack, x, y)) {
                rendered = true;
            }
        }
        RenderSystem.enableDepthTest();
        guiGraphics.pose().popPose();
        return rendered;
    }

    private void redrawMarkerSlotBackground(GuiGraphics guiGraphics, int x, int y) {
        int u = x - this.leftPos;
        int v = y - this.topPos;
        guiGraphics.blit(TEXTURE, x, y, u, v, 16, 16, ATLAS_WIDTH, ATLAS_HEIGHT);
    }

    private boolean renderFluidMarkerMaterial(GuiGraphics guiGraphics, ItemStack stack, int x, int y) {
        if (this.minecraft == null) {
            return false;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String fluidId = tag.getString(TAG_FLUID_NAME);
        if (fluidId == null || fluidId.isBlank()) {
            return false;
        }
        ResourceLocation key = ResourceLocation.tryParse(fluidId.trim());
        if (key == null) {
            return false;
        }
        var fluid = BuiltInRegistries.FLUID.getOptional(key).orElse(Fluids.EMPTY);
        if (fluid == Fluids.EMPTY) {
            return false;
        }
        FluidStack fluidStack = new FluidStack(fluid, Math.max(1, tag.getInt(TAG_FLUID_AMOUNT)));
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid);
        ResourceLocation texture = extensions.getStillTexture();
        int tint = extensions.getTintColor(fluidStack);
        TextureAtlasSprite sprite = texture == null ? null : this.minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
        return renderFluidSprite(guiGraphics, x, y, sprite, tint);
    }

    private boolean renderGasMarkerMaterial(GuiGraphics guiGraphics, ItemStack stack, int x, int y) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String gasId = tag.getString(TAG_GAS_NAME);
        if (gasId == null || gasId.isBlank()) {
            return false;
        }
        MekanismGasHelper.GasRenderData renderData = MekanismGasHelper.getRenderData(gasId);
        if (renderData == null) {
            return renderGasPlaceholder(guiGraphics, x, y, 0xFF7FC8C8, getGasDisplayName(gasId).getString());
        }
        if (renderData.icon() == null) {
            return renderGasPlaceholder(guiGraphics, x, y, renderData.tint(), renderData.displayName().getString());
        }
        return renderGasIcon(guiGraphics, x, y, renderData.icon(), renderData.tint());
    }

    private boolean renderFluidFromContainerFallback(GuiGraphics guiGraphics, ItemStack stack, int x, int y) {
        if (this.minecraft == null) {
            return false;
        }
        FluidStack fluidStack = FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
        if (fluidStack.isEmpty() && stack.getItem() instanceof BucketItem bucketItem && bucketItem.content != Fluids.EMPTY) {
            fluidStack = new FluidStack(bucketItem.content, 1000);
        }
        if (fluidStack.isEmpty() || fluidStack.getFluid() == null || fluidStack.getFluid() == Fluids.EMPTY) {
            return false;
        }

        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluidStack.getFluid());
        ResourceLocation texture = extensions.getStillTexture();
        int tint = extensions.getTintColor(fluidStack);
        TextureAtlasSprite sprite = texture == null ? null : this.minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
        return renderFluidSprite(guiGraphics, x, y, sprite, tint);
    }

    private boolean renderFluidSprite(GuiGraphics guiGraphics, int x, int y, TextureAtlasSprite sprite, int tint) {
        redrawMarkerSlotBackground(guiGraphics, x, y);

        if (this.minecraft == null) {
            return true;
        }

        TextureAtlasSprite missing = this.minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(MissingTextureAtlasSprite.getLocation());
        if (sprite == null || sprite == missing) {
            return true;
        }

        float red = ((tint >> 16) & 0xFF) / 255.0F;
        float green = ((tint >> 8) & 0xFF) / 255.0F;
        float blue = (tint & 0xFF) / 255.0F;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
        RenderSystem.setShaderColor(red, green, blue, 1.0F);
        guiGraphics.blit(x, y, 0, 16, 16, sprite);
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        return true;
    }

    private boolean renderGasFromContainerFallback(GuiGraphics guiGraphics, ItemStack stack, int x, int y) {
        MekanismGasHelper.GasStackView gas = MekanismGasHelper.extractGas(stack);
        if (gas == null || gas.gasId() == null || gas.gasId().isBlank()) {
            return false;
        }
        MekanismGasHelper.GasRenderData renderData = MekanismGasHelper.getRenderData(gas.gasId());
        if (renderData == null) {
            return renderGasPlaceholder(guiGraphics, x, y, gas.tint(), gas.displayName().getString());
        }
        if (renderData.icon() == null) {
            return renderGasPlaceholder(guiGraphics, x, y, renderData.tint(), renderData.displayName().getString());
        }
        return renderGasIcon(guiGraphics, x, y, renderData.icon(), renderData.tint());
    }

    private boolean renderGasPlaceholder(GuiGraphics guiGraphics, int x, int y, int tint, String label) {
        int resolvedTint = tint == -1 ? 0xFF7FC8C8 : (0xFF000000 | (tint & 0x00FFFFFF));
        redrawMarkerSlotBackground(guiGraphics, x, y);
        guiGraphics.fill(x + 2, y + 2, x + 14, y + 14, resolvedTint);
        String markerText = (label == null || label.isBlank())
                ? "G"
                : label.substring(0, 1).toUpperCase();
        guiGraphics.drawString(this.font, markerText, x + 5, y + 4, 0xFFFFFFFF, true);
        return true;
    }

    private boolean renderGasIcon(GuiGraphics guiGraphics, int x, int y, ResourceLocation icon, int tint) {
        redrawMarkerSlotBackground(guiGraphics, x, y);
        float red = ((tint >> 16) & 0xFF) / 255.0F;
        float green = ((tint >> 8) & 0xFF) / 255.0F;
        float blue = (tint & 0xFF) / 255.0F;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(red, green, blue, 1.0F);
        if (this.minecraft != null) {
            TextureAtlasSprite sprite = this.minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(icon);
            TextureAtlasSprite missing = this.minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(MissingTextureAtlasSprite.getLocation());
            if (sprite != null && sprite != missing) {
                RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
                guiGraphics.blit(x, y, 0, 16, 16, sprite);
            } else {
                RenderSystem.setShaderTexture(0, icon);
                guiGraphics.blit(icon, x, y, 0, 0, 16, 16, 16, 16);
            }
        }
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        return true;
    }

    private void renderMarkerAmounts(GuiGraphics guiGraphics) {
        for (int slotId = 0; slotId < 18 && slotId < this.menu.slots.size(); slotId++) {
            Slot slot = this.menu.slots.get(slotId);
            ItemStack stack = slot.getItem();
            if (!isAmountMarkerStack(stack)) {
                continue;
            }
            renderAmountText(guiGraphics, this.leftPos + slot.x, this.topPos + slot.y, getLogicalSlotAmount(stack));
        }
    }

    private void renderAmountText(GuiGraphics guiGraphics, int slotX, int slotY, int amount) {
        if (amount <= 1) {
            return;
        }
        String text = formatAmountShort(amount);
        float scale = 0.6F;
        int scaledWidth = Math.round(this.font.width(text) * scale);
        int x = slotX + 17 - scaledWidth;
        int y = slotY + 11;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0D, 0.0D, 260.0D);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.drawString(this.font, text, Math.round(x / scale), Math.round(y / scale), 0xFF00E5FF, true);
        guiGraphics.pose().popPose();
    }

    private String formatAmountShort(int amount) {
        if (amount < 1_000) {
            return Integer.toString(amount);
        }
        if (amount < 1_000_000) {
            return (amount / 1_000) + "k";
        }
        if (amount < 1_000_000_000) {
            return (amount / 1_000_000) + "m";
        }
        return (amount / 1_000_000_000) + "b";
    }

    private int resolvePatternSlotId(Slot slot) {
        if (slot == null) {
            return -1;
        }
        int slotId = this.menu.slots.indexOf(slot);
        return isPatternSlot(slotId) ? slotId : -1;
    }

    private boolean isPatternSlot(int slotId) {
        return slotId >= 0 && slotId < 18;
    }

    public boolean isPatternSlotId(int slotId) {
        return isPatternSlot(slotId);
    }

    private int getEditableAmountLimit(ItemStack stack) {
        if (stack.isEmpty()) {
            return 1;
        }
        if (isAmountMarkerStack(stack)) {
            return this.menu.getAmountLimitForStack(stack);
        }
        return Math.min(this.menu.getAmountLimitForStack(stack), stack.getMaxStackSize());
    }

    private int getLogicalSlotAmount(ItemStack stack) {
        if (stack.isEmpty()) {
            return 1;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.getBoolean(TAG_GAS_MARKER)) {
            return Math.max(1, tag.getInt(TAG_GAS_AMOUNT));
        }
        if (tag.getBoolean(TAG_FLUID_MARKER)) {
            return Math.max(1, tag.getInt(TAG_FLUID_AMOUNT));
        }
        if (tag.getBoolean(TAG_ITEM_MARKER)) {
            return Math.max(1, tag.getInt(TAG_ITEM_AMOUNT));
        }
        MekanismGasHelper.GasStackView gas = MekanismGasHelper.extractGas(stack);
        if (gas != null) {
            return Math.max(1, gas.amount());
        }
        FluidStack contained = FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
        if (!contained.isEmpty()) {
            return Math.max(1, contained.getAmount());
        }
        if (stack.getItem() instanceof BucketItem bucketItem && bucketItem.content != Fluids.EMPTY) {
            return 1000;
        }
        return Math.max(1, stack.getCount());
    }

    private boolean isFluidMarkerStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getBoolean(TAG_FLUID_MARKER) && tag.contains(TAG_FLUID_NAME);
    }

    private boolean isGasMarkerStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getBoolean(TAG_GAS_MARKER) && tag.contains(TAG_GAS_NAME);
    }

    private boolean isItemMarkerStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getBoolean(TAG_ITEM_MARKER);
    }

    private boolean isAmountMarkerStack(ItemStack stack) {
        if (isFluidMarkerStack(stack) || isGasMarkerStack(stack) || isItemMarkerStack(stack)) {
            return true;
        }
        if (MekanismGasHelper.extractGas(stack) != null) {
            return true;
        }
        FluidStack contained = FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
        if (!contained.isEmpty()) {
            return true;
        }
        return stack.getItem() instanceof BucketItem bucketItem && bucketItem.content != Fluids.EMPTY;
    }

    private Component getFluidDisplayName(String fluidId, int amount) {
        if (fluidId == null || fluidId.isBlank()) {
            return Component.literal("unknown");
        }
        ResourceLocation key = ResourceLocation.tryParse(fluidId.trim());
        if (key == null) {
            return Component.literal(fluidId);
        }
        return BuiltInRegistries.FLUID.getOptional(key)
                .filter(fluid -> fluid != Fluids.EMPTY)
                .map(fluid -> new FluidStack(fluid, Math.max(1, amount)).getHoverName())
                .orElse(Component.literal(fluidId));
    }

    private Component getGasDisplayName(String gasId) {
        return MekanismGasHelper.getDisplayName(gasId);
    }

    private record FilterEntry(String id, String legacyId, ItemStack input, ItemStack output, ItemStack pattern) {
    }
}
