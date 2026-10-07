package vinn.tekk.screwyourmobs.commands;

import vinn.tekk.screwyourmobs.ScrewYourMobsMod;
import vinn.tekk.screwyourmobs.config.EntityRemovalConfig;
import vinn.tekk.screwyourmobs.debug.DebugLog;
import vinn.tekk.screwyourmobs.procedures.EntityPurger;
import vinn.tekk.screwyourmobs.rules.RemovalRule;
import vinn.tekk.screwyourmobs.rules.RuleManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = ScrewYourMobsMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public class EntityRemovalCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("sym")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("reload").executes(EntityRemovalCommands::reload))
                        .then(Commands.literal("list").executes(EntityRemovalCommands::list))
                        .then(Commands.literal("stats").executes(EntityRemovalCommands::stats))
                        .then(Commands.literal("validate").executes(EntityRemovalCommands::validate))
                        .then(Commands.literal("purge").executes(EntityRemovalCommands::purge))
                        .then(Commands.literal("debug")
                                .executes(EntityRemovalCommands::debugStatus)
                                .then(Commands.literal("on").executes(ctx -> setDebug(ctx, true)))
                                .then(Commands.literal("off").executes(ctx -> setDebug(ctx, false)))
                                .then(Commands.argument("channel", StringArgumentType.word())
                                        .then(Commands.argument("value", BoolArgumentType.bool())
                                                .executes(EntityRemovalCommands::setDebugChannel))))
        );
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        RuleManager.reload();
        int purged = EntityPurger.purgeAll();
        int ruleCount = RuleManager.getTotalRules();
        int warnCount = RuleManager.getLastWarnings().size();

        ctx.getSource().sendSuccess(
                () -> Component.literal("§a[ScrewYourMobs!] Reloaded " + ruleCount
                        + " rules, purged " + purged + " entities, "
                        + warnCount + " validation warning(s)."),
                true);

        // Surface warnings in chat when debug chat is on
        if (!RuleManager.getLastWarnings().isEmpty()) {
            for (String w : RuleManager.getLastWarnings()) {
                DebugLog.chatOnly(ctx.getSource(), DebugLog.Channel.VALIDATE, "%s", w);
            }
        }
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        Map<String, RemovalRule> rules = RuleManager.getAllRules();
        if (rules.isEmpty()) {
            ctx.getSource().sendSuccess(
                    () -> Component.literal("§e[ScrewYourMobs!] No rules loaded."), false);
            return 0;
        }
        ctx.getSource().sendSuccess(
                () -> Component.literal("§6[ScrewYourMobs!] Loaded rules:"), false);

        for (RemovalRule rule : rules.values()) {
            String dims = rule.isGlobal()
                    ? "§aALL"
                    : rule.dimensions().stream()
                    .map(ResourceLocation::toString)
                    .reduce((a, b) -> a + ", " + b).orElse("");

            ctx.getSource().sendSuccess(
                    () -> Component.literal(
                            "§7- §f" + rule.name() + " §7[" + rule.entities().size()
                                    + " entities] §7dims: " + dims),
                    false);
        }
        return 1;
    }

    private static int stats(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(
                () -> Component.literal("§6[ScrewYourMobs!] §fRules: §a"
                        + RuleManager.getTotalRules()
                        + " §f| Entity entries: §a"
                        + RuleManager.getTotalEntityEntries()
                        + " §f| Warnings: §e"
                        + RuleManager.getLastWarnings().size()),
                false);
        return 1;
    }

    private static int validate(CommandContext<CommandSourceStack> ctx) {
        List<String> warnings = RuleManager.getLastWarnings();
        if (warnings.isEmpty()) {
            ctx.getSource().sendSuccess(
                    () -> Component.literal("§a[ScrewYourMobs!] No validation warnings."),
                    false);
            return 1;
        }
        ctx.getSource().sendSuccess(
                () -> Component.literal("§6[ScrewYourMobs!] " + warnings.size() + " warning(s):"),
                false);
        for (String w : warnings) {
            ctx.getSource().sendSuccess(
                    () -> Component.literal("§7- §e" + w), false);
        }
        return 1;
    }

    private static int purge(CommandContext<CommandSourceStack> ctx) {
        int purged = EntityPurger.purgeAll();
        ctx.getSource().sendSuccess(
                () -> Component.literal("§a[ScrewYourMobs!] Purged " + purged
                        + " existing entities."), true);
        return 1;
    }

    private static int debugStatus(CommandContext<CommandSourceStack> ctx) {
        boolean enabled = EntityRemovalConfig.DEBUG_ENABLED.get();
        ctx.getSource().sendSuccess(
                () -> Component.literal("§6[ScrewYourMobs!] Debug: "
                        + (enabled ? "§aON" : "§cOFF")
                        + " §7| validate=" + EntityRemovalConfig.DEBUG_VALIDATE.get()
                        + " removals=" + EntityRemovalConfig.DEBUG_REMOVALS.get()
                        + " ruleErrors=" + EntityRemovalConfig.DEBUG_RULE_ERRORS.get()
                        + " reloads=" + EntityRemovalConfig.DEBUG_RELOADS.get()
                        + " | console=" + EntityRemovalConfig.DEBUG_TO_CONSOLE.get()
                        + " chat=" + EntityRemovalConfig.DEBUG_TO_CHAT.get()
                        + " file=" + EntityRemovalConfig.DEBUG_TO_FILE.get()),
                false);
        ctx.getSource().sendSuccess(
                () -> Component.literal("§7Debug file: " + DebugLog.getDebugFilePath()),
                false);
        return 1;
    }

    private static int setDebug(CommandContext<CommandSourceStack> ctx, boolean value) {
        EntityRemovalConfig.DEBUG_ENABLED.set(value);
        ctx.getSource().sendSuccess(
                () -> Component.literal("§a[ScrewYourMobs!] Debug master switch: "
                        + (value ? "§aON" : "§cOFF")), true);
        return 1;
    }

    private static int setDebugChannel(CommandContext<CommandSourceStack> ctx) {
        String channel = StringArgumentType.getString(ctx, "channel");
        boolean value = BoolArgumentType.getBool(ctx, "value");
        boolean applied = true;

        switch (channel.toLowerCase()) {
            case "validaterules" -> EntityRemovalConfig.DEBUG_VALIDATE.set(value);
            case "logremovals" -> EntityRemovalConfig.DEBUG_REMOVALS.set(value);
            case "logruleerrors" -> EntityRemovalConfig.DEBUG_RULE_ERRORS.set(value);
            case "logreloads" -> EntityRemovalConfig.DEBUG_RELOADS.set(value);
            case "outputconsole" -> EntityRemovalConfig.DEBUG_TO_CONSOLE.set(value);
            case "outputchat" -> EntityRemovalConfig.DEBUG_TO_CHAT.set(value);
            case "outputfile" -> EntityRemovalConfig.DEBUG_TO_FILE.set(value);
            default -> applied = false;
        }

        if (!applied) {
            ctx.getSource().sendFailure(Component.literal(
                    "§cUnknown debug channel: " + channel));
            return 0;
        }

        ctx.getSource().sendSuccess(
                () -> Component.literal("§a[ScrewYourMobs!] " + channel + " = " + value),
                true);
        return 1;
    }
}