package vinn.tekk.screwyourmobs.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class CorrectionScreen extends Screen {

    public enum Result { ACCEPT_SUGGESTION, EDIT, CANCEL }

    private final String originalInput;
    private final String suggestion;
    private final Consumer<Result> callback;

    public CorrectionScreen(Screen parent,
                            Component title,
                            String originalInput,
                            String suggestion,
                            Consumer<Result> callback) {
        super(title);
        this.originalInput = originalInput;
        this.suggestion = suggestion;
        this.callback = callback;
    }

    @Override
    protected void init() {
        super.init();

        int buttonY = this.height / 2 + 30;
        int gap = 8;

        if (suggestion != null) {
            int acceptWidth = 160;
            addRenderableWidget(Button.builder(
                            Component.translatable("screwyourmobs.screen.correction.use",
                                    suggestion),
                            b -> callback.accept(Result.ACCEPT_SUGGESTION))
                    .bounds(this.width / 2 - acceptWidth / 2, buttonY,
                            acceptWidth, 20).build());
            buttonY += 24;
        }

        int editWidth = 100;
        int cancelWidth = 100;
        int totalWidth = editWidth + gap + cancelWidth;
        int startX = (this.width - totalWidth) / 2;

        addRenderableWidget(Button.builder(
                        Component.translatable("screwyourmobs.screen.correction.edit"),
                        b -> callback.accept(Result.EDIT))
                .bounds(startX, buttonY, editWidth, 20).build());

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.cancel"),
                        b -> callback.accept(Result.CANCEL))
                .bounds(startX + editWidth + gap, buttonY, cancelWidth, 20).build());
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.render(gfx, mouseX, mouseY, partialTick);

        int centerY = this.height / 2;

        gfx.drawString(this.font, this.title,
                (this.width - this.font.width(this.title)) / 2,
                centerY - 40, 0xFF5555, true);

        Component invalidLine = Component.translatable(
                "screwyourmobs.screen.correction.invalid", originalInput);
        gfx.drawString(this.font, invalidLine,
                (this.width - this.font.width(invalidLine)) / 2,
                centerY - 22, 0xFFFFFF, false);

        if (suggestion != null) {
            Component suggestLine = Component.translatable(
                    "screwyourmobs.screen.correction.suggest", suggestion);
            gfx.drawString(this.font, suggestLine,
                    (this.width - this.font.width(suggestLine)) / 2,
                    centerY - 4, 0xFFAA00, true);
        } else {
            Component noMatch = Component.translatable(
                    "screwyourmobs.screen.correction.no_match");
            gfx.drawString(this.font, noMatch,
                    (this.width - this.font.width(noMatch)) / 2,
                    centerY - 4, 0xFFAA00, false);
        }
    }

    @Override
    public void onClose() {
        callback.accept(Result.CANCEL);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}