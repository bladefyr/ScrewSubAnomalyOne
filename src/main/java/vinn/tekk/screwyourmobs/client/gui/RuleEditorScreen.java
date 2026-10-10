package vinn.tekk.screwyourmobs.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import vinn.tekk.screwyourmobs.ScrewYourMobsMod;
import vinn.tekk.screwyourmobs.client.*;
import vinn.tekk.screwyourmobs.config.EntityRemovalConfig;
import vinn.tekk.screwyourmobs.network.MutateRulePacket;
import vinn.tekk.screwyourmobs.network.RequestReloadPacket;
import vinn.tekk.screwyourmobs.procedures.EntityPurger;
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

    private RuleListWidget.RuleKey lastSelected;

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

        RuleManager.addSyncListener(this.syncListener);

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

        int buttonY = this.height - 24;
        int cx = PADDING;

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

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.done"),
                        b -> onClose())
                .bounds(this.width - PADDING - 70, buttonY, 70, 20).build());

        if (lastSelected != null) {
            ruleList.selectByName(lastSelected.name(), lastSelected.isWorld());
        }

        updateButtons();
    }

    // ---- Selection ----

    private void onRuleSelected(RuleListWidget.RuleKey key) {
        this.lastSelected = key;
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
                name -> mutate(MutateRulePacket.Operation.CREATE, "", name)
        );
    }

    private void onDelete() {
        var selected = ruleList.getSelectedKey();
        if (selected == null) return;

        Minecraft.getInstance().setScreen(new ConfirmScreen(
                confirmed -> {
                    if (confirmed) {
                        lastSelected = null;
                        mutate(MutateRulePacket.Operation.DELETE, selected.name(), "");
                    }
                    Minecraft.getInstance().setScreen(this);
                },
                Component.translatable("screwyourmobs.screen.delete.title"),
                Component.translatable("screwyourmobs.screen.delete.message", selected.name())
        ));
    }

    private void onToggle() {
        var selected = ruleList.getSelectedKey();
        if (selected == null) return;
        mutate(MutateRulePacket.Operation.TOGGLE_DISABLED, selected.name(), "");
    }

    private void onReload() {
        if (isLocalServer()) {
            ClientChat.actionBar(Component.literal("§7Reloading rules..."));

            RuleManager.reload();
            EntityPurger.purgeAll();

            int rules = RuleManager.getTotalRules();
            int warnings = RuleManager.getLastWarnings().size();

            ClientChat.send(Component.literal(
                    "§a[ScrewYourMobs!] §fReloaded §a" + rules + " §frules, §a"
                            + warnings + " §fvalidation warning(s)."));

            if (EntityRemovalConfig.DEBUG_ENABLED.get()
                    && EntityRemovalConfig.DEBUG_VALIDATE.get()
                    && EntityRemovalConfig.DEBUG_TO_CHAT.get()) {
                for (String w : RuleManager.getLastWarnings()) {
                    ClientChat.send(Component.literal("§7[§bDEBUG/validate§7] §f" + w));
                }
            }

            refreshAll();
        } else {
            ClientChat.actionBar(Component.literal("§7Reloading rules on server..."));
            PacketDistributor.sendToServer(RequestReloadPacket.INSTANCE);
        }
    }

    // ---- RuleDetailWidget.Callbacks ----

    @Override
    public void onAddEntity(String ruleName) {
        InputFlow.promptAndStay(
                Minecraft.getInstance(),
                this,
                Component.translatable("screwyourmobs.screen.add_entity.title", ruleName),
                RuleValidators.entityOptions(),
                "",
                java.util.Set.of(),
                RuleValidators::validateEntity,
                value -> mutate(MutateRulePacket.Operation.ADD_ENTITY, ruleName, value)
        );
    }

    @Override
    public void onAddDimension(String ruleName) {
        InputFlow.promptAndStay(
                Minecraft.getInstance(),
                this,
                Component.translatable("screwyourmobs.screen.add_dimension.title", ruleName),
                RuleValidators.dimensionOptions(),
                "",
                java.util.Set.of(),
                RuleValidators::validateDimension,
                value -> mutate(MutateRulePacket.Operation.ADD_DIMENSION, ruleName, value)
        );
    }

    @Override
    public void onRemoveEntity(String ruleName, String entityId) {
        Minecraft.getInstance().setScreen(new ConfirmScreen(
                confirmed -> {
                    if (confirmed) {
                        mutate(MutateRulePacket.Operation.REMOVE_ENTITY, ruleName, entityId);
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
                        mutate(MutateRulePacket.Operation.REMOVE_DIMENSION, ruleName, dimensionId);
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
        List<String> existingRules = new java.util.ArrayList<>();
        existingRules.addAll(RuleManager.getRuleNames());
        existingRules.addAll(RuleManager.getDisabledRules().keySet());
        existingRules.sort(String::compareToIgnoreCase);

        java.util.Set<String> marked = new java.util.HashSet<>(existingRules);
        marked.remove(ruleName);

        InputFlow.prompt(
                Minecraft.getInstance(),
                this,
                Component.translatable("screwyourmobs.screen.rename.title", ruleName),
                existingRules,
                ruleName,
                marked,
                RuleValidators::validateRuleName,
                newName -> {
                    if (lastSelected != null && lastSelected.name().equals(ruleName)) {
                        lastSelected = new RuleListWidget.RuleKey(newName, lastSelected.isWorld());
                    }
                    mutate(MutateRulePacket.Operation.RENAME, ruleName, newName);
                }
        );
    }

    private void mutate(MutateRulePacket.Operation op, String ruleName, String payload) {
        if (isLocalServer()) {
            boolean ok = switch (op) {
                case ADD_ENTITY -> RuleWriter.addEntity(ruleName, payload);
                case REMOVE_ENTITY -> RuleWriter.removeEntity(ruleName, payload);
                case ADD_DIMENSION -> RuleWriter.addDimension(ruleName, payload);
                case REMOVE_DIMENSION -> RuleWriter.removeDimension(ruleName, payload);
                case RENAME -> RuleWriter.renameRule(ruleName, payload);
                case DELETE -> RuleManager.deleteRule(ruleName);
                case TOGGLE_DISABLED -> RuleManager.toggleDisabled(ruleName);
                case CREATE -> {
                    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                    if (server == null) yield false;
                    Path worldRulesDir = server.getWorldPath(LevelResource.ROOT)
                            .resolve(RuleLoader.WORLD_RULES_FOLDER);
                    yield RuleWriter.createRule(payload, worldRulesDir);
                }
            };
            if (ok) {
                RuleManager.reload();
                refreshAll();
            }
        } else {
            PacketDistributor.sendToServer(new MutateRulePacket(op, ruleName, payload));
        }
    }

    // ---- Refresh ----

    private void refreshAll() {
        var current = ruleList.getSelectedKey();
        if (current != null) lastSelected = current;

        ruleList.refresh();

        if (lastSelected != null) {
            ruleList.selectByName(lastSelected.name(), lastSelected.isWorld());
        }

        ruleDetail.setRule(ruleList.getSelectedKey());
        updateButtons();
    }

    // ---- Rendering ----

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.render(gfx, mouseX, mouseY, partialTick);

        gfx.drawString(this.font, this.title, PADDING, 10, AccentColor.solid(), true);

        String versionStr = "v" + MOD_VERSION;
        gfx.drawString(this.font, versionStr,
                this.width - PADDING - this.font.width(versionStr), 10,
                AccentColor.solid(), true);
    }

    private final Runnable syncListener = () -> {
        Minecraft.getInstance().execute(this::refreshAll);
    };

    @Override
    public void onClose() {
        RuleManager.removeSyncListener(this.syncListener);
        Minecraft.getInstance().setScreen(parent);
    }

    private boolean isLocalServer() {
        return Minecraft.getInstance().hasSingleplayerServer();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}