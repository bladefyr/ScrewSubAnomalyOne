package vinn.tekk.screwyourmobs.helpers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;

public final class MarqueeText {

    private MarqueeText() {}

    private static final int HEAD_HOLD_MS = 500;
    private static final int HOLD_MS = 1500;
    private static final double SCROLL_PX_PER_SEC = 30.0;

    private record HoverState(long startMs) {}

    private static final Map<String, HoverState> HOVER_STATE = new HashMap<>();

    public static void draw(GuiGraphics gfx, Component text,
                            int x, int y, int maxWidth,
                            int color,
                            boolean hovered) {
        var font = Minecraft.getInstance().font;
        int textWidth = font.width(text);

        if (textWidth <= maxWidth) {
            gfx.drawString(font, text, x, y, color, false);
            return;
        }

        String key = text.getString();

        if (!hovered) {
            HOVER_STATE.remove(key);
            gfx.drawString(font, text, x, y, color, false);
            return;
        }

        HoverState state = HOVER_STATE.computeIfAbsent(key,
                k -> new HoverState(System.currentTimeMillis()));

        long elapsed = System.currentTimeMillis() - state.startMs();

        int overflow = textWidth - maxWidth;
        int oneWayMs = (int) (overflow * 1000.0 / SCROLL_PX_PER_SEC);
        int cycleMs = HEAD_HOLD_MS + oneWayMs + HOLD_MS + oneWayMs;

        long t = elapsed % cycleMs;

        int offset;
        if (t < HEAD_HOLD_MS) {
            offset = 0;
        } else if (t < HEAD_HOLD_MS + oneWayMs) {
            offset = (int) ((t - HEAD_HOLD_MS) * SCROLL_PX_PER_SEC / 1000.0);
        } else if (t < HEAD_HOLD_MS + oneWayMs + HOLD_MS) {
            offset = overflow;
        } else {
            offset = overflow - (int) ((t - HEAD_HOLD_MS - oneWayMs - HOLD_MS)
                    * SCROLL_PX_PER_SEC / 1000.0);
        }

        gfx.drawString(font, text, x - offset, y, color, false);
    }
}