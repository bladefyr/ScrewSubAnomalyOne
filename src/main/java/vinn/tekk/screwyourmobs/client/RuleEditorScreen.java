package vinn.tekk.screwyourmobs.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import vinn.tekk.screwyourmobs.ScrewYourMobsMod;
import vinn.tekk.screwyourmobs.rules.RuleManager;

public class RuleEditorScreen extends Screen {

    private static final int LIST_WIDTH = 160;
    private static final int PADDING = 8;
    private static final int BOTTOM_BAR_HEIGHT = 30;

    private final Screen parent;

    private RuleListWidget ruleList;
    private RuleDetailWidget ruleDetail;

    private Button deleteButton;
    private Button toggleButton;

    private static final String MOD_VERSION = ModList.get()
            .getModContainerById(ScrewYourMobsMod.MODID)
            .map(c -> c.getModInfo().getVersion().toString())
            .orElse("?");

    public RuleEditorScreen(Screen parent) {
        super(Component.translatable("screwyourmobs.screen.title", worldName()));
        this.parent = parent;
    }

    private static Component worldName() {
        Minecraft mc = Minecraft.getInstance();

        // Singleplayer
        if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) {
            return Component.literal(
                    mc.getSingleplayerServer().getWorldData().getLevelName());
        }

        // Multiplayer
        if (mc.getCurrentServer() != null) {
            return Component.literal(mc.getCurrentServer().name);
        }

        // No world
        return Component.translatable("screwyourmobs.screen.title.no_world");
    }

    @Override
    protected void init() {
        super.init();

        int top = 28;
        int bottomBarY = this.height - BOTTOM_BAR_HEIGHT;
        int paneHeight = bottomBarY - top;

        int listX = PADDING;
        this.ruleList = new RuleListWidget(
                listX, top, LIST_WIDTH, paneHeight,
                this::onRuleSelected);

        int detailX = PADDING + LIST_WIDTH + PADDING;
        int detailWidth = this.width - detailX - PADDING;
        this.ruleDetail = new RuleDetailWidget(
                detailX, top, detailWidth, paneHeight);

        addRenderableWidget(this.ruleList);
        addRenderableWidget(this.ruleDetail);

        // Bottom bar
        int buttonY = this.height - 24;
        int cx = PADDING;

        this.deleteButton = Button.builder(
                        Component.translatable("screwyourmobs.screen.button.delete"),
                        b -> onDelete())
                .bounds(cx, buttonY, 70, 20).build();
        addRenderableWidget(this.deleteButton);
        cx += 74;

        this.toggleButton = Button.builder(
                        Component.translatable("screwyourmobs.screen.button.disable"),
                        b -> onToggle())
                .bounds(cx, buttonY, 80, 20).build();
        addRenderableWidget(this.toggleButton);
        cx += 84;

        addRenderableWidget(Button.builder(
                        Component.translatable("screwyourmobs.screen.button.reload"),
                        b -> onReload())
                .bounds(cx, buttonY, 70, 20).build());

        // Done — right aligned
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.done"),
                        b -> onClose())
                .bounds(this.width - PADDING - 70, buttonY, 70, 20).build());

        updateButtons();
    }

    private void onRuleSelected(RuleListWidget.RuleKey key) {
        ruleDetail.setRule(key);
        updateButtons();
    }

    private void updateButtons() {
        var selected = ruleList.getSelectedKey();
        boolean hasSelection = selected != null;

        deleteButton.active = hasSelection;
        toggleButton.active = hasSelection;

        if (!hasSelection) return;

        boolean disabled = RuleManager.isDisabled(selected.name());
        toggleButton.setMessage(Component.translatable(disabled
                ? "screwyourmobs.screen.button.enable"
                : "screwyourmobs.screen.button.disable"));
    }

    private void onDelete() {
        var selected = ruleList.getSelectedKey();
        if (selected == null) return;

        Minecraft.getInstance().setScreen(new ConfirmScreen(
                confirmed -> {
                    if (confirmed) {
                        RuleManager.deleteRule(selected.name());
                        RuleManager.reload();
                    }
                    Minecraft.getInstance().setScreen(new RuleEditorScreen(parent));
                },
                Component.translatable("screwyourmobs.screen.delete.title"),
                Component.translatable("screwyourmobs.screen.delete.message", selected.name())
        ));
    }

    private void onToggle() {
        var selected = ruleList.getSelectedKey();
        if (selected == null) return;

        if (RuleManager.toggleDisabled(selected.name())) {
            RuleManager.reload();
            ruleList.refresh();
            updateButtons();
        }
    }

    private void onReload() {
        RuleManager.reload();
        ruleList.refresh();
        ruleDetail.setRule(ruleList.getSelectedKey());
        updateButtons();
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.render(gfx, mouseX, mouseY, partialTick);

        gfx.drawString(this.font, this.title, PADDING, 10, 0xFFAA00, true);

        String versionStr = "v" + MOD_VERSION;
        gfx.drawString(this.font, versionStr,
                this.width - PADDING - this.font.width(versionStr), 10,
                0xFFAA00, true);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}