package vinn.tekk.screwyourmobs.client;

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

    private sealed interface Line permits Text, Header, Spacer {}
    private record Text(Component text, int color) implements Line {}
    private record Header(Component text) implements Line {}
    private record Spacer() implements Line {}

    private final List<Line> lines = new ArrayList<>();
    private double scrollOffset = 0;

    public RuleDetailWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
        rebuild(null);
    }

    public void setRule(RuleListWidget.RuleKey key) {
        rebuild(key);
    }

    private void rebuild(RuleListWidget.RuleKey key) {
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

        lines.add(new Text(Component.literal(rule.name()), 0xFFFFFF));

        RuleSource src = RuleManager.getSource(key.name());
        if (src != null) {
            lines.add(new Text(Component.literal("§7" + displayPath(src.path())), 0x808080));
        }

        if (rule.disabled()) {
            lines.add(new Text(Component.translatable(
                    "screwyourmobs.screen.detail.disabled_banner"), 0xFF5555));
        }

        for (String w : RuleManager.getLastWarnings()) {
            if (w.contains("'" + key.name() + "'")) {
                lines.add(new Text(Component.literal("§e⚠ " + w), 0xFFFF55));
            }
        }

        lines.add(new Spacer());

        if (!rule.entities().isEmpty()) {
            lines.add(new Header(Component.translatable(
                    "screwyourmobs.screen.detail.entities", rule.entities().size())));
            for (ResourceLocation id : rule.entities()) {
                lines.add(new Text(Component.literal("§7▪ §f" + id), 0xFFFFFF));
            }
            lines.add(new Spacer());
        }

        if (!rule.entityTags().isEmpty()) {
            lines.add(new Header(Component.translatable(
                    "screwyourmobs.screen.detail.tags", rule.entityTags().size())));
            for (TagKey<EntityType<?>> tag : rule.entityTags()) {
                lines.add(new Text(
                        Component.literal("§7▪ §b#" + tag.location()), 0xFFFFFF));
            }
            lines.add(new Spacer());
        }

        if (rule.dimensions().isEmpty()) {
            lines.add(new Header(Component.translatable(
                    "screwyourmobs.screen.detail.dimensions.all")));
        } else {
            lines.add(new Header(Component.translatable(
                    "screwyourmobs.screen.detail.dimensions",
                    rule.dimensions().size())));
            for (ResourceLocation id : rule.dimensions()) {
                lines.add(new Text(Component.literal("§7▪ §d" + id), 0xFFFFFF));
            }
        }
    }

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

    @Override
    protected void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        gfx.fill(getX(), getY(), getX() + width, getY() + height, 0x40000000);

        gfx.enableScissor(getX(), getY(), getX() + width, getY() + height);

        int visibleTop = (int) scrollOffset;
        int visibleBottom = visibleTop + height;

        for (int i = 0; i < lines.size(); i++) {
            int lineTop = i * ROW_HEIGHT;
            int lineBottom = lineTop + ROW_HEIGHT;
            if (lineBottom <= visibleTop || lineTop >= visibleBottom) continue;

            int drawY = getY() + lineTop - visibleTop;
            Line line = lines.get(i);

            boolean rowHovered = mouseX >= getX() && mouseX <= getX() + width
                    && mouseY >= drawY && mouseY <= drawY + ROW_HEIGHT;

            if (line instanceof Text(Component text, int color)) {
                MarqueeText.draw(gfx, text,
                        getX() + 4, drawY + 1,
                        width - 4 - SCROLLBAR_WIDTH - 4,
                        color,
                        rowHovered);
            } else if (line instanceof Header(Component text)) {
                MarqueeText.draw(gfx, text,
                        getX() + 4, drawY + 1,
                        width - 4 - SCROLLBAR_WIDTH - 4,
                        0xFFAA00,
                        rowHovered);
            }
        }

        if (scrollbarVisible()) {
            int barX = getX() + width - SCROLLBAR_WIDTH;
            int barHeight = Math.max(16, height * height / contentHeight());
            int barY = getY() + (int) ((height - barHeight) * (scrollOffset / maxScroll()));
            gfx.fill(barX, getY(), barX + SCROLLBAR_WIDTH, getY() + height, 0x40000000);
            gfx.fill(barX, barY, barX + SCROLLBAR_WIDTH, barY + barHeight, 0xFFAAAAAA);
        }
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