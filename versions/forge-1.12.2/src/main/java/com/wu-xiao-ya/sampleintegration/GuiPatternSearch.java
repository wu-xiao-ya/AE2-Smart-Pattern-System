package com.lwx1145.sampleintegration;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Search-only screen for wildcard recipe entries. No player inventory panel is rendered.
 */
public class GuiPatternSearch extends GuiScreen {

    private static final ResourceLocation TEXTURE =
        new ResourceLocation("sampleintegration", "textures/gui/pattern_editor_sousu.png");
    private static final int TEXTURE_DRAW_OFFSET_X = 2;
    private static final int GUI_WIDTH = 201;
    private static final int GUI_HEIGHT = 220;

    private static final int BUTTON_TO_FILTER = 0;
    private static final int BUTTON_TO_EDITOR = 1;
    private static final int BUTTON_TBD = 2;

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
    private final List<SearchEntry> allEntries = new ArrayList<>();
    private final List<SearchEntry> filteredEntries = new ArrayList<>();

    private GuiTextField searchField;
    private int listScroll = 0;
    private int guiLeft;
    private int guiTop;
    private boolean draggingScrollBar = false;

    public GuiPatternSearch(EntityPlayer player, int returnGuiId) {
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
            BUTTON_TO_EDITOR,
            textureLeft() + 180,
            this.guiTop + 26,
            20,
            20,
            "2",
            I18n.format("gui.sampleintegration.switch_to_editor")
        ));
        this.buttonList.add(new InvisibleButton(
            BUTTON_TBD,
            textureLeft() + 180,
            this.guiTop + 50,
            20,
            20,
            "3",
            I18n.format("gui.sampleintegration.open_mod_filter")
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
        this.searchField.setMaxStringLength(64);
        this.searchField.setEnableBackgroundDrawing(false);

        rebuildEntries();
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
        if (button.id == BUTTON_TO_EDITOR) {
            PacketHandler.INSTANCE.sendToServer(new PacketOpenPatternGui(GuiHandler.PATTERN_EDITOR_GUI));
            return;
        }
        if (button.id == BUTTON_TBD) {
            this.mc.displayGuiScreen(new GuiModFilter(this.player, this.returnGuiId));
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
            openReturnGui();
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
        if (mouseButton == 0) {
            SearchEntry clickedEntry = getEntryAt(mouseX, mouseY);
            if (clickedEntry != null) {
                GuiNavigationState.setPendingFilterEntryId(clickedEntry.inputOre + "->" + clickedEntry.outputOre);
                PacketHandler.INSTANCE.sendToServer(new PacketOpenPatternGui(GuiHandler.PATTERN_FILTER_GUI));
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

        String title = I18n.format("gui.sampleintegration.search.title");
        this.fontRenderer.drawString(title, textureLeft() + 16, this.guiTop + 16, 0x404040);
        String stat = I18n.format(
            "gui.sampleintegration.search.matches",
            Integer.toString(filteredEntries.size()),
            Integer.toString(allEntries.size())
        );
        this.fontRenderer.drawString(stat, textureLeft() + 16, this.guiTop + 45, 0x666666);

        if (this.searchField != null) {
            this.searchField.drawTextBox();
        }

        drawEntryRows(mouseX, mouseY);
        drawScrollBar();

        super.drawScreen(mouseX, mouseY, partialTicks);

        SearchEntry hoveredEntry = getEntryAt(mouseX, mouseY);
        if (hoveredEntry != null) {
            List<String> tips = new ArrayList<>();
            tips.add(hoveredEntry.displayName);
            tips.add(I18n.format("gui.sampleintegration.search.tooltip.input", hoveredEntry.inputOre));
            tips.add(I18n.format("gui.sampleintegration.search.tooltip.output", hoveredEntry.outputOre));
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

            SearchEntry entry = filteredEntries.get(index);
            int rowY = top + row * ROW_HEIGHT;
            boolean hovered = mouseX >= left && mouseX < left + LIST_WIDTH
                && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            if (hovered) {
                drawRect(left, rowY, left + LIST_WIDTH, rowY + ROW_HEIGHT, 0x33FFFFFF);
            }

            String line = entry.displayName;
            if (line == null || line.isEmpty()) {
                line = entry.inputOre + " -> " + entry.outputOre;
            }
            line = this.fontRenderer.trimStringToWidth(line, LIST_WIDTH - 4);
            this.fontRenderer.drawString(line, left + 2, rowY + 4, 0x404040);
        }
    }

    private SearchEntry getEntryAt(int mouseX, int mouseY) {
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

    private void applyFilter() {
        filteredEntries.clear();
        String keyword = this.searchField == null ? "" : this.searchField.getText().trim().toLowerCase(Locale.ROOT);

        for (SearchEntry entry : allEntries) {
            if (keyword.isEmpty() || entry.matches(keyword)) {
                filteredEntries.add(entry);
            }
        }

        listScroll = Math.max(0, Math.min(listScroll, getMaxScroll()));
    }

    private int getMaxScroll() {
        return Math.max(0, filteredEntries.size() - VISIBLE_ROWS);
    }

    private void rebuildEntries() {
        allEntries.clear();

        ItemStack patternStack = findPatternInInventory(this.player);
        if (patternStack.isEmpty() || !(patternStack.getItem() instanceof ItemTest)) {
            return;
        }
        SmartPatternDetails mainPattern = new SmartPatternDetails(patternStack);
        List<SmartPatternDetails> details = mainPattern.expandToVirtualPatterns();
        Set<String> seenPairs = new LinkedHashSet<>();

        for (SmartPatternDetails detail : details) {
            if (detail == null) {
                continue;
            }
            ItemStack detailPattern = detail.getPattern();
            if (detailPattern == null || detailPattern.isEmpty()) {
                continue;
            }
            String input = ItemTest.getInputOreNameStatic(detailPattern);
            String output = ItemTest.getOutputOreNameStatic(detailPattern);
            if (input == null || input.isEmpty() || output == null || output.isEmpty()) {
                continue;
            }
            String pairKey = input + "->" + output;
            if (!seenPairs.add(pairKey)) {
                continue;
            }
            String display = ItemTest.getEncodedItemNameStatic(detailPattern);
            if (display == null || display.isEmpty()) {
                display = input + " -> " + output;
            }
            allEntries.add(new SearchEntry(input, output, sanitizeDisplayText(display)));
        }
    }

    private static String sanitizeDisplayText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String value = text
            .replace("\u2192", "->")
            .replace("\u21D2", "->")
            .replace("\u27F6", "->");
        value = value.replace("闂?", "->");
        value = value.replaceAll("\\s*->\\s*", " -> ");
        return value.trim();
    }

    private static String toWildcard(String oreName) {
        if (oreName == null || oreName.isEmpty()) {
            return null;
        }
        if (oreName.endsWith("*")) {
            return oreName;
        }
        String prefix = OreDictRecipeCache.findPrefix(oreName);
        if (prefix == null || prefix.isEmpty()) {
            return null;
        }
        return prefix + "*";
    }

    private static ItemStack findPatternInInventory(EntityPlayer player) {
        ItemStack main = player.getHeldItemMainhand();
        if (!main.isEmpty() && main.getItem() instanceof ItemTest) {
            return main;
        }

        ItemStack off = player.getHeldItemOffhand();
        if (!off.isEmpty() && off.getItem() instanceof ItemTest) {
            return off;
        }

        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemTest) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    private void openReturnGui() {
        PacketHandler.INSTANCE.sendToServer(new PacketOpenPatternGui(returnGuiId));
    }

    private static final class SearchEntry {
        private final String inputOre;
        private final String outputOre;
        private final String displayName;

        private SearchEntry(String inputOre, String outputOre, String displayName) {
            this.inputOre = inputOre;
            this.outputOre = outputOre;
            this.displayName = displayName;
        }

        private boolean matches(String keyword) {
            if (keyword == null || keyword.isEmpty()) {
                return true;
            }
            String lower = keyword.toLowerCase(Locale.ROOT);
            return containsIgnoreCase(inputOre, lower)
                || containsIgnoreCase(outputOre, lower)
                || containsIgnoreCase(displayName, lower);
        }

        private static boolean containsIgnoreCase(String source, String keyword) {
            return source != null && source.toLowerCase(Locale.ROOT).contains(keyword);
        }
    }
}
