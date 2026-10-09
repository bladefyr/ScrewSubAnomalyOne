package vinn.tekk.screwyourmobs.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import vinn.tekk.screwyourmobs.helpers.MarqueeText;
import vinn.tekk.screwyourmobs.rules.RemovalRule;
import vinn.tekk.screwyourmobs.rules.RuleManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class RuleListWidget extends AbstractWidget {

    public record RuleKey(String name, boolean isWorld) {}

    private static final int ROW_HEIGHT = 20;
    private static final int SCROLLBAR_WIDTH = 6;

    private final Consumer<RuleKey> onSelect;
    private final List<Row> rows = new ArrayList<>();
    private RuleKey selectedKey;
    private double scrollOffset = 0;

    private sealed interface Row permits SectionRow, RuleRow {}
    private record SectionRow(Component label) implements Row {}
    private record RuleRow(RemovalRule rule, boolean isWorld) implements Row {}

    public RuleListWidget(int x, int y, int width, int height,
                          Consumer<RuleKey> onSelect) {
        super(x, y, width, height, Component.empty());
        this.onSelect = onSelect;
        refresh();
    }

    public void refresh() {
        rows.clear();
        selectedKey = null;
        scrollOffset = 0;

        List<RemovalRule> global = new ArrayList<>();
        List<RemovalRule> world = new ArrayList<>();

        for (Map.Entry<String, RemovalRule> e : RuleManager.getAllRules().entrySet()) {
            var src = RuleManager.getSource(e.getKey());
            if (src != null && src.isWorldRule()) world.add(e.getValue());
            else global.add(e.getValue());
        }
        for (RemovalRule r : RuleManager.getDisabledRules().values()) {
            var src = RuleManager.getSource(r.name());
            if (src != null && src.isWorldRule()) world.add(r);
            else global.add(r);
        }

        global.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        world.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));

        if (!global.isEmpty()) {
            rows.add(new SectionRow(Component.translatable(
                    "screwyourmobs.screen.section.global")));
            for (RemovalRule r : global) rows.add(new RuleRow(r, false));
        }
        if (!world.isEmpty()) {
            rows.add(new SectionRow(Component.translatable(
                    "screwyourmobs.screen.section.world")));
            for (RemovalRule r : world) rows.add(new RuleRow(r, true));
        }
        if (rows.isEmpty()) {
            rows.add(new SectionRow(Component.translatable(
                    "screwyourmobs.screen.empty")));
        }
    }

    public RuleKey getSelectedKey() {
        return selectedKey;
    }

    private int contentHeight() {
        return rows.size() * ROW_HEIGHT;
    }

    private int maxScroll() {
        return Math.max(0, contentHeight() - height);
    }

    private boolean scrollbarVisible() {
        return contentHeight() > height;
    }

    @Override
    protected void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        gfx.fill(getX(), getY(), getX() + width, getY() + height, 0x40000000);

        gfx.enableScissor(getX(), getY(), getX() + width, getY() + height);

        int visibleTop = (int) scrollOffset;
        int visibleBottom = visibleTop + height;

        for (int i = 0; i < rows.size(); i++) {
            int rowTop = i * ROW_HEIGHT;
            int rowBottom = rowTop + ROW_HEIGHT;
            if (rowBottom <= visibleTop || rowTop >= visibleBottom) continue;

            int drawY = getY() + rowTop - visibleTop;
            Row row = rows.get(i);

            if (row instanceof SectionRow(Component label)) {
                gfx.drawString(Minecraft.getInstance().font,
                        label, getX() + 4, drawY + 6, 0xFFAA00, false);
            } else if (row instanceof RuleRow r) {
                renderRuleRow(gfx, r, drawY, mouseX, mouseY);
            }
        }

        gfx.disableScissor();

        if (scrollbarVisible()) {
            int barX = getX() + width - SCROLLBAR_WIDTH;
            int barHeight = Math.max(16, height * height / contentHeight());
            int barY = getY() + (int) ((height - barHeight)
                    * (scrollOffset / maxScroll()));
            gfx.fill(barX, getY(), barX + SCROLLBAR_WIDTH, getY() + height, 0x40000000);
            gfx.fill(barX, barY, barX + SCROLLBAR_WIDTH, barY + barHeight, 0xFFAAAAAA);
        }
    }

    private void renderRuleRow(GuiGraphics gfx, RuleRow r, int drawY,
                               int mouseX, int mouseY) {
        RuleKey key = new RuleKey(r.rule().name(), r.isWorld());
        boolean selected = Objects.equals(key, selectedKey);

        int textColor = r.rule().disabled() ? 0x888888 : 0xFFFFFF;

        RuleStatus status = statusOf(r.rule());
        gfx.blit(status.texture(), getX() + 4, drawY + 2, 0, 0, 16, 16, 16, 16);

        int nameX = getX() + 24;
        int nameMaxWidth = width - 24 - SCROLLBAR_WIDTH - 4;

        boolean rowHovered = mouseX >= getX() && mouseX <= getX() + width
                && mouseY >= drawY && mouseY <= drawY + ROW_HEIGHT;

        MarqueeText.draw(gfx, Component.literal(r.rule().name()),
                nameX, drawY + 6, nameMaxWidth, textColor,
                rowHovered);

        if (selected) {
            gfx.fill(getX(), drawY, getX() + width, drawY + ROW_HEIGHT, 0x40FFAA00);
            gfx.fill(getX(), drawY, getX() + 2, drawY + ROW_HEIGHT, 0xFFFFAA00);
        }

        if (mouseX >= getX() + 4 && mouseX <= getX() + 20
                && mouseY >= drawY + 2 && mouseY <= drawY + 18) {
            gfx.renderTooltip(Minecraft.getInstance().font,
                    status.label(), mouseX, mouseY);
        }
    }

    private RuleStatus statusOf(RemovalRule r) {
        if (r.disabled()) return RuleStatus.BAD;
        for (String w : RuleManager.getLastWarnings()) {
            if (w.contains("'" + r.name() + "'")) return RuleStatus.PROBLEM;
        }
        return RuleStatus.GOOD;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!isMouseOver(mx, my)) return false;

        int visibleTop = (int) scrollOffset;
        int localY = (int) my - getY() + visibleTop;
        int index = localY / ROW_HEIGHT;

        if (index < 0 || index >= rows.size()) return false;

        Row row = rows.get(index);
        if (row instanceof RuleRow(RemovalRule rule, boolean isWorld)) {
            selectedKey = new RuleKey(rule.name(), isWorld);
            onSelect.accept(selectedKey);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (!isMouseOver(mx, my)) return false;
        scrollOffset -= scrollY * ROW_HEIGHT;
        scrollOffset = Math.clamp(scrollOffset, 0, maxScroll());
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {}
}