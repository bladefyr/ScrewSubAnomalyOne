package vinn.tekk.screwyourmobs.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import vinn.tekk.screwyourmobs.helpers.MarqueeText;
import vinn.tekk.screwyourmobs.rules.RemovalRule;
import vinn.tekk.screwyourmobs.rules.RuleManager;
import vinn.tekk.screwyourmobs.rules.RuleSource;

import java.util.ArrayList;
import java.util.List;

public class RuleDetailWidget extends AbstractWidget {

    private static final int ROW_HEIGHT = 12;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int ACTION_ZONE_WIDTH = 80;

    public interface Callbacks {
        void onNewRule();

        void onAddEntity(String ruleName);
        void onAddDimension(String ruleName);
        void onRemoveEntity(String ruleName, String entityId);
        void onRemoveDimension(String ruleName, String dimensionId);
        void onRename(String ruleName);
    }

    private enum ActionType { RENAME, REMOVE_ENTITY, REMOVE_DIMENSION }

    private enum AddType { ENTITY, DIMENSION }


    private sealed interface Line permits Text, Header, Spacer, ActionRow, AddRow {}
    private record Text(Component text, int color) implements Line {}
    private record Header(Component text) implements Line {}
    private record Spacer() implements Line {}
    private record ActionRow(Component content, Component action,
                             ActionType actionType, String payload) implements Line {}
    private record AddRow(Component label, AddType addType) implements Line {}

    private final List<Line> lines = new ArrayList<>();
    private double scrollOffset = 0;

    private RuleListWidget.RuleKey currentKey;
    private final Callbacks callbacks;

    public RuleDetailWidget(int x, int y, int width, int height, Callbacks callbacks) {
        super(x, y, width, height, Component.empty());
        this.callbacks = callbacks;
        rebuild(null);
    }

    public void setRule(RuleListWidget.RuleKey key) {
        rebuild(key);
    }


    private void rebuild(RuleListWidget.RuleKey key) {
        this.currentKey = key;
        lines.clear();
        scrollOffset = 0;

        if (key == null) {
            lines.add(new Text(Component.translatable(
                    "screwyourmobs.screen.detail.no_selection"), 0x888888));
            return;
        }

        RemovalRule rule = RuleManager.getRuleIncludingDisabled(key.name());
        if (rule == null) {
            lines.add(new Text(Component.literal("Rule not found"), 0xFF5555));
            return;
        }

        String ruleName = key.name();

        // --- Header: rule name + [Rename] ---
        lines.add(new ActionRow(
                Component.literal(rule.name()),
                Component.translatable("screwyourmobs.screen.detail.rename"),
                ActionType.RENAME,
                ruleName));

        RuleSource src = RuleManager.getSource(ruleName);
        if (src != null) {
            lines.add(new Text(Component.literal("§7" + displayPath(src.path())), 0x808080));
        }

        if (rule.disabled()) {
            lines.add(new Text(Component.translatable(
                    "screwyourmobs.screen.detail.disabled_banner"), 0xFF5555));
        }

        for (String w : RuleManager.getLastWarnings()) {
            if (w.contains("'" + ruleName + "'")) {
                lines.add(new Text(Component.literal("§e⚠ " + w), 0xFFFF55));
            }
        }

        lines.add(new Spacer());

        // --- Entities ---
        if (!rule.entities().isEmpty()) {
            lines.add(new Header(Component.translatable(
                    "screwyourmobs.screen.detail.entities", rule.entities().size())));
            for (ResourceLocation id : rule.entities()) {
                lines.add(new ActionRow(
                        Component.literal("§7▪ §f" + id),
                        Component.literal("§c[×]"),
                        ActionType.REMOVE_ENTITY,
                        id.toString()));
            }
        }
        lines.add(new AddRow(
                Component.translatable("screwyourmobs.screen.detail.add_entity"),
                AddType.ENTITY));
        lines.add(new Spacer());

        // --- Tags ---
        if (!rule.entityTags().isEmpty()) {
            lines.add(new Header(Component.translatable(
                    "screwyourmobs.screen.detail.tags", rule.entityTags().size())));
            for (TagKey<EntityType<?>> tag : rule.entityTags()) {
                lines.add(new ActionRow(
                        Component.literal("§7▪ §b#" + tag.location()),
                        Component.literal("§c[×]"),
                        ActionType.REMOVE_ENTITY,
                        "#" + tag.location()));
            }
            lines.add(new Spacer());
        }

        // --- Dimensions ---
        if (rule.dimensions().isEmpty()) {
            lines.add(new Header(Component.translatable(
                    "screwyourmobs.screen.detail.dimensions.all")));
        } else {
            lines.add(new Header(Component.translatable(
                    "screwyourmobs.screen.detail.dimensions", rule.dimensions().size())));
            for (ResourceLocation id : rule.dimensions()) {
                lines.add(new ActionRow(
                        Component.literal("§7▪ §d" + id),
                        Component.literal("§c[×]"),
                        ActionType.REMOVE_DIMENSION,
                        id.toString()));
            }
        }
        lines.add(new AddRow(
                Component.translatable("screwyourmobs.screen.detail.add_dimension"),
                AddType.DIMENSION));
    }

    // ---- Helpers ----

    private int contentHeight() {
        return lines.size() * ROW_HEIGHT;
    }

    private int maxScroll() {
        return Math.max(0, contentHeight() - height);
    }

    private boolean scrollbarVisible() {
        return contentHeight() > height;
    }

    private static String displayPath(java.nio.file.Path path) {
        try {
            java.nio.file.Path gameDir = net.neoforged.fml.loading.FMLPaths.GAMEDIR.get()
                    .toAbsolutePath().normalize();
            java.nio.file.Path abs = path.toAbsolutePath().normalize();

            if (abs.startsWith(gameDir)) {
                String rootName = gameDir.getFileName() != null
                        ? gameDir.getFileName().toString()
                        : ".";
                String relative = gameDir.relativize(abs).toString().replace('\\', '/');
                return rootName + "/" + relative;
            }
            return abs.toString();
        } catch (Exception e) {
            return path.toString();
        }
    }

    private int rowIndexAt(double mx, double my) {
        if (!isMouseOver(mx, my)) return -1;

        if (mx >= getX() + width - SCROLLBAR_WIDTH) return -1;

        int visibleTop = (int) scrollOffset;
        int localY = (int) my - getY() + visibleTop;
        int index = localY / ROW_HEIGHT;
        return (index >= 0 && index < lines.size()) ? index : -1;
    }

    private boolean isInActionZone(double mx) {
        return mx >= getX() + (double) width / 2;
    }

    // ---- Rendering ----

    @Override
    protected void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        gfx.fill(getX(), getY(), getX() + width, getY() + height, 0x40000000);

        gfx.enableScissor(getX(), getY(), getX() + width, getY() + height);
        try {
            int visibleTop = (int) scrollOffset;
            int visibleBottom = visibleTop + height;

            int hoveredIndex = rowIndexAt(mouseX, mouseY);
            boolean hoveredActionZone = isInActionZone(mouseX);
            int contentWidth = width - 4 - SCROLLBAR_WIDTH - 4;

            for (int i = 0; i < lines.size(); i++) {
                int lineTop = i * ROW_HEIGHT;
                int lineBottom = lineTop + ROW_HEIGHT;
                if (lineBottom <= visibleTop || lineTop >= visibleBottom) continue;

                int drawY = getY() + lineTop - visibleTop;
                Line line = lines.get(i);
                boolean rowHovered = (i == hoveredIndex);

                switch (line) {
                    case Text(Component text, int color) ->
                            MarqueeText.draw(gfx, text,
                                    getX() + 4, drawY + 1, contentWidth,
                                    color, rowHovered);

                    case Header(Component text) ->
                            MarqueeText.draw(gfx, text, getX() + 4, drawY + 1, contentWidth,
                                    AccentColor.solid(), rowHovered);

                    case Spacer() -> { /* no-op */ }

                    case ActionRow(Component content, Component action,
                                   ActionType actionType, String payload) -> {
                        var font = Minecraft.getInstance().font;
                        int actionWidth = font.width(action);
                        int actionX = getX() + width - SCROLLBAR_WIDTH - 4 - actionWidth;
                        int actionY = drawY + 1;

                        int contentMaxWidth = actionX - (getX() + 4) - 4;
                        MarqueeText.draw(gfx, content,
                                getX() + 4, drawY + 1, contentMaxWidth,
                                0xFFFFFF, rowHovered);

                        boolean actionHovered = rowHovered && hoveredActionZone;
                        if (actionHovered) {
                            int pad = 2;
                            int bgColor = (actionType == ActionType.RENAME)
                                    ? 0x40FFAA00
                                    : 0x40FF5555;
                            gfx.fill(actionX - pad, actionY - pad,
                                    actionX + actionWidth + pad, actionY + font.lineHeight + pad,
                                    bgColor);
                        }

                        int actionColor;
                        if (actionType == ActionType.RENAME) {
                            actionColor = actionHovered ? AccentColor.solid() : (AccentColor.rgb() | 0x88000000);
                        } else {
                            actionColor = actionHovered ? 0xFFFF5555 : 0xFFFFAAAA;
                        }
                        gfx.drawString(font, action, actionX, actionY, actionColor, false);
                    }

                    case AddRow(Component label, AddType addType) -> {
                        if (rowHovered) {
                            gfx.fill(getX(), drawY, getX() + width - SCROLLBAR_WIDTH,
                                    drawY + ROW_HEIGHT, 0x3055FF55);
                        }
                        int color = rowHovered ? 0xFF55FF55 : 0xFF55AA55;
                        gfx.drawString(Minecraft.getInstance().font,
                                label, getX() + 4, drawY + 1, color, false);
                    }
                }
            }
        } finally {
            gfx.disableScissor();
        }

        if (scrollbarVisible()) {
            int barX = getX() + width - SCROLLBAR_WIDTH;
            int barHeight = Math.max(16, height * height / contentHeight());
            int barY = getY() + (int) ((height - barHeight) * (scrollOffset / maxScroll()));
            gfx.fill(barX, getY(), barX + SCROLLBAR_WIDTH, getY() + height, 0x40000000);
            gfx.fill(barX, barY, barX + SCROLLBAR_WIDTH, barY + barHeight, 0xFFAAAAAA);
        }
    }

    // ---- Clicks ----

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;
        if (currentKey == null) return false;

        int index = rowIndexAt(mx, my);
        if (index < 0) return false;

        Line line = lines.get(index);
        String ruleName = currentKey.name();

        switch (line) {
            case ActionRow(Component content, Component action,
                           ActionType actionType, String payload) -> {
                if (!isInActionZone(mx)) return false;

                switch (actionType) {
                    case RENAME -> callbacks.onRename(ruleName);
                    case REMOVE_ENTITY -> callbacks.onRemoveEntity(ruleName, payload);
                    case REMOVE_DIMENSION -> callbacks.onRemoveDimension(ruleName, payload);
                }
                return true;
            }

            case AddRow(Component label, AddType addType) -> {
                switch (addType) {
                    case ENTITY -> callbacks.onAddEntity(ruleName);
                    case DIMENSION -> callbacks.onAddDimension(ruleName);
                }
                return true;
            }

            default -> { /* non-clickable */ }
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