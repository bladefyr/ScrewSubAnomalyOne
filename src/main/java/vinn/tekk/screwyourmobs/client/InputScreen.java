package vinn.tekk.screwyourmobs.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import vinn.tekk.screwyourmobs.helpers.SuggestionHelper;

import java.util.List;
import java.util.function.Consumer;

public class InputScreen extends Screen {

    private static final int INPUT_WIDTH = 260;
    private static final int INPUT_HEIGHT = 20;
    private static final int SUGGESTION_ROW_HEIGHT = 12;
    private static final int MAX_VISIBLE_SUGGESTIONS = 6;

    private final Screen parent;
    private final List<String> allOptions;
    private final Consumer<String> onAccept;
    private final String initialValue;
    private final java.util.Set<String> markedOptions;

    private EditBox input;
    private List<String> suggestions = List.of();
    private List<String> frozenSuggestions = null;
    private int cycleIndex = -1;
    private boolean suppressResponder = false;
    private int suggestionScroll = 0;

    private double lastMouseX = -1;
    private double lastMouseY = -1;

    private Button okButton;

    public InputScreen(Screen parent,
                       Component title,
                       List<String> allOptions,
                       String initialValue,
                       Consumer<String> onAccept) {
        this(parent, title, allOptions, initialValue, java.util.Set.of(), onAccept);
    }

    public InputScreen(Screen parent,
                       Component title,
                       List<String> allOptions,
                       String initialValue,
                       java.util.Set<String> markedOptions,
                       Consumer<String> onAccept) {
        super(title);
        this.parent = parent;
        this.allOptions = allOptions;
        this.initialValue = initialValue;
        this.markedOptions = markedOptions;
        this.onAccept = onAccept;
    }

    @Override
    protected void init() {
        super.init();

        int cx = (this.width - INPUT_WIDTH) / 2;
        int inputY = this.height / 2 - 40;

        this.input = new EditBox(this.font, cx, inputY, INPUT_WIDTH, INPUT_HEIGHT,
                Component.empty());
        this.input.setValue(initialValue == null ? "" : initialValue);
        this.input.setMaxLength(256);
        this.input.setResponder(this::onInputChanged);
        this.input.setFocused(true);
        addRenderableWidget(this.input);
        setInitialFocus(this.input);

        onInputChanged(this.input.getValue());

        int buttonY = this.height / 2 + 60;
        int buttonWidth = 100;

        this.okButton = Button.builder(
                        Component.translatable("screwyourmobs.screen.input.ok"),
                        b -> submit())
                .bounds(this.width / 2 - buttonWidth - 4, buttonY, buttonWidth, 20)
                .build();
        addRenderableWidget(this.okButton);

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.cancel"),
                        b -> cancel())
                .bounds(this.width / 2 + 4, buttonY, buttonWidth, 20)
                .build());
    }

    // ---- Hover detection ----

    @Override
    public void mouseMoved(double mx, double my) {
        this.lastMouseX = mx;
        this.lastMouseY = my;
        super.mouseMoved(mx, my);
    }

    private int hoveredIndex() {
        if (lastMouseX < 0 || lastMouseY < 0) return -1;

        int suggestionTop = this.input.getY() + INPUT_HEIGHT + 6;
        int listBottom = suggestionTop + MAX_VISIBLE_SUGGESTIONS * SUGGESTION_ROW_HEIGHT;

        int left = this.input.getX();
        int right = this.input.getX() + INPUT_WIDTH;

        if (lastMouseX < left || lastMouseX > right) return -1;
        if (lastMouseY < suggestionTop || lastMouseY > listBottom) return -1;

        int row = (int) ((lastMouseY - suggestionTop) / SUGGESTION_ROW_HEIGHT);
        int index = suggestionScroll + row;

        return (index >= 0 && index < suggestions.size()) ? index : -1;
    }

    // ---- Input handling ----

    private void onInputChanged(String value) {
        if (suppressResponder) return;

        frozenSuggestions = null;
        cycleIndex = -1;

        suggestions = SuggestionHelper.filter(allOptions, value);
        suggestionScroll = 0;
    }

    private void ensureHighlightVisible() {
        if (cycleIndex < 0) return;

        if (cycleIndex < suggestionScroll) {
            suggestionScroll = cycleIndex;
        } else if (cycleIndex >= suggestionScroll + MAX_VISIBLE_SUGGESTIONS) {
            suggestionScroll = cycleIndex - MAX_VISIBLE_SUGGESTIONS + 1;
        }

        int maxScroll = Math.max(0, suggestions.size() - MAX_VISIBLE_SUGGESTIONS);
        suggestionScroll = Mth.clamp(suggestionScroll, 0, maxScroll);
    }

    private void cycleSuggestion(boolean backwards) {
        if (frozenSuggestions == null) {
            if (suggestions.isEmpty()) return;
            frozenSuggestions = List.copyOf(suggestions);

            int hovered = hoveredIndex();
            if (hovered >= 0) {
                cycleIndex = hovered;
            } else if (backwards) {
                cycleIndex = Math.min(
                        suggestionScroll + MAX_VISIBLE_SUGGESTIONS - 1,
                        frozenSuggestions.size() - 1);
            } else {
                cycleIndex = suggestionScroll;
            }
        } else {
            int delta = backwards ? -1 : 1;
            cycleIndex = Math.floorMod(cycleIndex + delta, frozenSuggestions.size());
        }

        String picked = frozenSuggestions.get(cycleIndex);

        suppressResponder = true;
        try {
            input.setValue(picked);
            input.setCursorPosition(picked.length());
        } finally {
            suppressResponder = false;
        }

        suggestions = frozenSuggestions;
        ensureHighlightVisible();
    }

    private void submit() {
        String value = input.getValue().trim();
        if (value.isEmpty()) return;
        if (onAccept != null) onAccept.accept(value);
    }

    private void cancel() {
        if (onAccept != null) onAccept.accept(null);
    }

    // ---- Input events ----

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN) {
            if (frozenSuggestions == null) {
                if (suggestions.isEmpty()) return true;
                frozenSuggestions = List.copyOf(suggestions);

                if (keyCode == GLFW.GLFW_KEY_DOWN) {
                    cycleIndex = suggestionScroll;
                } else {
                    cycleIndex = Math.min(
                            suggestionScroll + MAX_VISIBLE_SUGGESTIONS - 1,
                            frozenSuggestions.size() - 1);
                }
            } else {
                int delta = (keyCode == GLFW.GLFW_KEY_UP) ? -1 : 1;
                cycleIndex = Math.floorMod(cycleIndex + delta, frozenSuggestions.size());
            }
            ensureHighlightVisible();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_TAB) {
            cycleSuggestion((modifiers & GLFW.GLFW_MOD_SHIFT) != 0);
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (frozenSuggestions != null && cycleIndex >= 0
                    && cycleIndex < frozenSuggestions.size()) {
                suppressResponder = true;
                try {
                    input.setValue(frozenSuggestions.get(cycleIndex));
                } finally {
                    suppressResponder = false;
                }
            }
            submit();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            cancel();
            return true;
        }

        switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT,
                 GLFW.GLFW_KEY_HOME, GLFW.GLFW_KEY_END,
                 GLFW.GLFW_KEY_PAGE_UP, GLFW.GLFW_KEY_PAGE_DOWN,
                 GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT,
                 GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL,
                 GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT,
                 GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER -> {
            }
            default -> {
                frozenSuggestions = null;
                cycleIndex = -1;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);

        lastMouseX = mx;
        lastMouseY = my;

        int index = hoveredIndex();
        if (index < 0) return super.mouseClicked(mx, my, button);

        String picked = suggestions.get(index);

        suppressResponder = true;
        try {
            input.setValue(picked);
            input.setCursorPosition(picked.length());
        } finally {
            suppressResponder = false;
        }

        frozenSuggestions = null;
        cycleIndex = -1;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (suggestions.size() <= MAX_VISIBLE_SUGGESTIONS) return false;

        suggestionScroll = Mth.clamp(
                suggestionScroll - (int) Math.signum(sy),
                0, suggestions.size() - MAX_VISIBLE_SUGGESTIONS);

        frozenSuggestions = null;
        cycleIndex = -1;

        return true;
    }

    // ---- Rendering ----

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.render(gfx, mouseX, mouseY, partialTick);

        gfx.drawString(this.font, this.title,
                (this.width - this.font.width(this.title)) / 2,
                this.height / 2 - 65, 0xFFAA00, true);

        int suggestionTop = this.input.getY() + INPUT_HEIGHT + 6;
        int visible = Math.min(suggestions.size() - suggestionScroll, MAX_VISIBLE_SUGGESTIONS);
        int hovered = hoveredIndex();

        for (int i = 0; i < visible; i++) {
            int index = suggestionScroll + i;
            if (index >= suggestions.size()) break;

            String suggestion = suggestions.get(index);
            int y = suggestionTop + i * SUGGESTION_ROW_HEIGHT;
            int x = this.input.getX() + 4;

            boolean highlighted;
            if (frozenSuggestions != null) {
                highlighted = (index == cycleIndex);
            } else {
                highlighted = suggestion.equalsIgnoreCase(input.getValue())
                        || index == hovered;
            }

            int color = highlighted ? 0xFFFFAA00 : 0xFFAAAAAA;
            String prefix = highlighted ? "▸ " : "  ";

            gfx.drawString(this.font, prefix + suggestion, x, y, color, false);

            if (markedOptions.contains(suggestion)) {
                int suffixX = x + this.font.width(prefix + suggestion);
                gfx.drawString(this.font, " (taken)", suffixX, y, 0xFF808080, false);
            }
        }

        if (suggestions.size() > MAX_VISIBLE_SUGGESTIONS) {
            int indicatorX = this.input.getX() + INPUT_WIDTH - 8;
            int indicatorTop = suggestionTop;
            int indicatorHeight = MAX_VISIBLE_SUGGESTIONS * SUGGESTION_ROW_HEIGHT;

            gfx.fill(indicatorX, indicatorTop, indicatorX + 3,
                    indicatorTop + indicatorHeight, 0x40FFFFFF);

            int thumbHeight = Math.max(6,
                    indicatorHeight * MAX_VISIBLE_SUGGESTIONS / suggestions.size());

            int maxScroll = Math.max(1, suggestions.size() - MAX_VISIBLE_SUGGESTIONS);
            int thumbY = indicatorTop
                    + (indicatorHeight - thumbHeight) * suggestionScroll / maxScroll;

            gfx.fill(indicatorX, thumbY, indicatorX + 3, thumbY + thumbHeight, 0xFFFFAA00);
        }

        if (suggestions.isEmpty() && !input.getValue().isEmpty()) {
            gfx.drawString(this.font,
                    Component.translatable("screwyourmobs.screen.input.no_matches"),
                    this.input.getX() + 4, suggestionTop, 0xFF5555, false);
        }
    }

    @Override
    public void onClose() {
        cancel();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}