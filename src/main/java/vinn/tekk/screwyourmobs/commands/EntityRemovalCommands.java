package vinn.tekk.screwyourmobs.commands;

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
import vinn.tekk.screwyourmobs.ScrewYourMobsMod;
import vinn.tekk.screwyourmobs.config.EntityRemovalConfig;
import vinn.tekk.screwyourmobs.debug.DebugLog;
import vinn.tekk.screwyourmobs.procedures.EntityPurger;
import vinn.tekk.screwyourmobs.rules.RemovalRule;
import vinn.tekk.screwyourmobs.rules.RuleManager;

import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = ScrewYourMobsMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public class EntityRemovalCommands {

    private static Component prefix() {
        return Component.literal("§a").append(
                        Component.translatable("screwyourmobs.prefix.success"))
                .append(Component.literal(" §r"));
    }

    private static Component prefixed(Component body) {
        return prefix().copy().append(body);
    }

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
                () -> prefixed(Component.translatable(
                        "screwyourmobs.command.reload.success",
                        ruleCount, purged, warnCount)),
                true);

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
                    () -> prefixed(Component.translatable(
                            "screwyourmobs.command.list.empty")),
                    false);
            return 0;
        }

        ctx.getSource().sendSuccess(
                () -> prefixed(Component.translatable(
                        "screwyourmobs.command.list.header")),
                false);

        for (RemovalRule rule : rules.values()) {
            String dims = rule.isGlobal()
                    ? "§a" + Component.translatable("screwyourmobs.command.list.dims.all").getString()
                    : rule.dimensions().stream()
                    .map(ResourceLocation::toString)
                    .reduce((a, b) -> a + ", " + b).orElse("");

            ctx.getSource().sendSuccess(
                    () -> Component.literal("§7").append(Component.translatable(
                            "screwyourmobs.command.list.entry",
                            rule.name(), rule.entities().size(), dims)),
                    false);
        }
        return 1;
    }

    private static int stats(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(
                () -> prefixed(Component.translatable(
                        "screwyourmobs.command.stats.summary",
                        RuleManager.getTotalRules(),
                        RuleManager.getTotalEntityEntries(),
                        RuleManager.getLastWarnings().size())),
                false);
        return 1;
    }

    private static int validate(CommandContext<CommandSourceStack> ctx) {
        List<String> warnings = RuleManager.getLastWarnings();
        if (warnings.isEmpty()) {
            ctx.getSource().sendSuccess(
                    () -> prefixed(Component.translatable(
                            "screwyourmobs.command.validate.none")),
                    false);
            return 1;
        }

        ctx.getSource().sendSuccess(
                () -> prefixed(Component.translatable(
                        "screwyourmobs.command.validate.header", warnings.size())),
                false);

        for (String w : warnings) {
            ctx.getSource().sendSuccess(
                    () -> Component.literal("§7").append(Component.translatable(
                            "screwyourmobs.command.validate.entry", w)),
                    false);
        }
        return 1;
    }

    private static int purge(CommandContext<CommandSourceStack> ctx) {
        int purged = EntityPurger.purgeAll();
        ctx.getSource().sendSuccess(
                () -> prefixed(Component.translatable(
                        "screwyourmobs.command.purge.success", purged)),
                true);
        return 1;
    }

    private static int debugStatus(CommandContext<CommandSourceStack> ctx) {
        boolean enabled = EntityRemovalConfig.DEBUG_ENABLED.get();
        Component onOff = Component.translatable(enabled
                ? "screwyourmobs.command.debug.on"
                : "screwyourmobs.command.debug.off");

        ctx.getSource().sendSuccess(
                () -> prefixed(Component.translatable(
                        "screwyourmobs.command.debug.status",
                        onOff,
                        EntityRemovalConfig.DEBUG_VALIDATE.get(),
                        EntityRemovalConfig.DEBUG_REMOVALS.get(),
                        EntityRemovalConfig.DEBUG_RULE_ERRORS.get(),
                        EntityRemovalConfig.DEBUG_RELOADS.get(),
                        EntityRemovalConfig.DEBUG_TO_CONSOLE.get(),
                        EntityRemovalConfig.DEBUG_TO_CHAT.get(),
                        EntityRemovalConfig.DEBUG_TO_FILE.get())),
                false);

        ctx.getSource().sendSuccess(
                () -> Component.literal("§7").append(Component.translatable(
                        "screwyourmobs.command.debug.file",
                        DebugLog.getDebugFilePath().toString())),
                false);
        return 1;
    }

    private static int setDebug(CommandContext<CommandSourceStack> ctx, boolean value) {
        EntityRemovalConfig.DEBUG_ENABLED.set(value);
        ctx.getSource().sendSuccess(
                () -> prefixed(Component.translatable(
                        "screwyourmobs.command.debug.master",
                        Component.translatable(value
                                ? "screwyourmobs.command.debug.on"
                                : "screwyourmobs.command.debug.off"))),
                true);
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
            ctx.getSource().sendFailure(prefixed(Component.translatable(
                    "screwyourmobs.command.debug.channel.unknown", channel)));
            return 0;
        }

        ctx.getSource().sendSuccess(
                () -> prefixed(Component.translatable(
                        "screwyourmobs.command.debug.channel.set", channel, value)),
                true);
        return 1;
    }
}