package vinn.tekk.screwyourmobs.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import vinn.tekk.screwyourmobs.ScrewYourMobsMod;
import vinn.tekk.screwyourmobs.rules.RuleLoader;
import vinn.tekk.screwyourmobs.rules.RuleManager;
import vinn.tekk.screwyourmobs.rules.RuleWriter;

import java.nio.file.Path;
import java.util.List;

public class RuleEditorScreen extends Screen implements RuleDetailWidget.Callbacks {

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

        if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) {
            return Component.literal(
                    mc.getSingleplayerServer().getWorldData().getLevelName());
        }

        if (mc.getCurrentServer() != null) {
            return Component.literal(mc.getCurrentServer().name);
        }

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
                detailX, top, detailWidth, paneHeight,
                this);

        addRenderableWidget(this.ruleList);
        addRenderableWidget(this.ruleDetail);

        // Bottom bar
        int buttonY = this.height - 24;
        int cx = PADDING;

        // [+ New Rule]
        addRenderableWidget(Button.builder(
                        Component.translatable("screwyourmobs.screen.button.new_rule"),
                        b -> onNewRule())
                .bounds(cx, buttonY, 90, 20).build());
        cx += 94;

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

    // ---- Selection ----

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

    // ---- Bottom-bar actions ----

    @Override
    public void onNewRule() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        Path worldRulesDir = server.getWorldPath(LevelResource.ROOT)
                .resolve(RuleLoader.WORLD_RULES_FOLDER);

        // Options = existing rule names (sorted). All are "taken" by definition.
        List<String> existingRules = new java.util.ArrayList<>();
        existingRules.addAll(RuleManager.getRuleNames());
        existingRules.addAll(RuleManager.getDisabledRules().keySet());
        existingRules.sort(String::compareToIgnoreCase);

        java.util.Set<String> marked = new java.util.HashSet<>(existingRules);

        InputFlow.prompt(
                Minecraft.getInstance(),
                this,
                Component.translatable("screwyourmobs.screen.new_rule.title"),
                existingRules,
                "",
                marked,
                RuleValidators::validateRuleName,
                name -> {
                    if (RuleWriter.createRule(name, worldRulesDir)) {
                        RuleManager.reload();
                        refreshAll();
                    }
                }
        );
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

    // ---- RuleDetailWidget.Callbacks ----

    @Override
    public void onAddEntity(String ruleName) {
        InputFlow.prompt(
                Minecraft.getInstance(),
                this,
                Component.translatable("screwyourmobs.screen.add_entity.title", ruleName),
                RuleValidators.entityOptions(),
                "",
                RuleValidators::validateEntity,
                value -> {
                    if (RuleWriter.addEntity(ruleName, value)) {
                        RuleManager.reload();
                        refreshAll();
                    }
                }
        );
    }

    @Override
    public void onAddDimension(String ruleName) {
        InputFlow.prompt(
                Minecraft.getInstance(),
                this,
                Component.translatable("screwyourmobs.screen.add_dimension.title", ruleName),
                RuleValidators.dimensionOptions(),
                "",
                RuleValidators::validateDimension,
                value -> {
                    if (RuleWriter.addDimension(ruleName, value)) {
                        RuleManager.reload();
                        refreshAll();
                    }
                }
        );
    }

    @Override
    public void onRemoveEntity(String ruleName, String entityId) {
        Minecraft.getInstance().setScreen(new ConfirmScreen(
                confirmed -> {
                    if (confirmed) {
                        if (RuleWriter.removeEntity(ruleName, entityId)) {
                            RuleManager.reload();
                            refreshAll();
                        }
                    }
                    Minecraft.getInstance().setScreen(this);
                },
                Component.translatable("screwyourmobs.screen.remove_entity.title"),
                Component.translatable("screwyourmobs.screen.remove_entity.message",
                        entityId, ruleName)
        ));
    }

    @Override
    public void onRemoveDimension(String ruleName, String dimensionId) {
        Minecraft.getInstance().setScreen(new ConfirmScreen(
                confirmed -> {
                    if (confirmed) {
                        if (RuleWriter.removeDimension(ruleName, dimensionId)) {
                            RuleManager.reload();
                            refreshAll();
                        }
                    }
                    Minecraft.getInstance().setScreen(this);
                },
                Component.translatable("screwyourmobs.screen.remove_dimension.title"),
                Component.translatable("screwyourmobs.screen.remove_dimension.message",
                        dimensionId, ruleName)
        ));
    }

    @Override
    public void onRename(String ruleName) {
        // Exclude the rule's own name from the "taken" set
        List<String> existingRules = new java.util.ArrayList<>();
        existingRules.addAll(RuleManager.getRuleNames());
        existingRules.addAll(RuleManager.getDisabledRules().keySet());
        existingRules.sort(String::compareToIgnoreCase);

        java.util.Set<String> marked = new java.util.HashSet<>(existingRules);
        marked.remove(ruleName);   // renaming a rule to itself isn't a collision

        InputFlow.prompt(
                Minecraft.getInstance(),
                this,
                Component.translatable("screwyourmobs.screen.rename.title", ruleName),
                existingRules,
                ruleName,
                marked,
                RuleValidators::validateRuleName,
                newName -> {
                    if (RuleWriter.renameRule(ruleName, newName)) {
                        RuleManager.reload();
                        refreshAll();
                    }
                }
        );
    }

    // ---- Refresh ----

    private void refreshAll() {
        // Preserve selection by name across the refresh
        var previous = ruleList.getSelectedKey();
        ruleList.refresh();

        if (previous != null) {
            // Try to reselect the same rule (by name, either scope)
            ruleList.selectByName(previous.name(), previous.isWorld());
        }

        ruleDetail.setRule(ruleList.getSelectedKey());
        updateButtons();
    }

    // ---- Rendering ----

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