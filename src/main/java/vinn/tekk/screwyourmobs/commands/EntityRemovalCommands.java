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
import vinn.tekk.screwyourmobs.rules.RuleWriter;

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

    private static final Map<String, java.util.function.Consumer<Boolean>> DEBUG_CHANNEL_SETTERS = Map.ofEntries(
            Map.entry("debugMaster", EntityRemovalConfig.DEBUG_ENABLED::set),
            Map.entry("validateRules", EntityRemovalConfig.DEBUG_VALIDATE::set),
            Map.entry("logRemovals", EntityRemovalConfig.DEBUG_REMOVALS::set),
            Map.entry("logRuleErrors", EntityRemovalConfig.DEBUG_RULE_ERRORS::set),
            Map.entry("logReloads", EntityRemovalConfig.DEBUG_RELOADS::set),
            Map.entry("outputConsole", EntityRemovalConfig.DEBUG_TO_CONSOLE::set),
            Map.entry("outputChat", EntityRemovalConfig.DEBUG_TO_CHAT::set),
            Map.entry("outputFile", EntityRemovalConfig.DEBUG_TO_FILE::set)
    );

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions>
    suggestChannels(CommandContext<CommandSourceStack> ctx,
                    com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase();
        for (String channel : DEBUG_CHANNEL_SETTERS.keySet()) {
            if (channel.toLowerCase().startsWith(remaining)) {
                builder.suggest(channel);
            }
        }
        return builder.buildFuture();
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("sym")
                        .requires(src -> src.hasPermission(2))
                        .executes(EntityRemovalCommands::openScreen)
                        .then(Commands.literal("reload").executes(EntityRemovalCommands::reload))
                        .then(Commands.literal("list").executes(EntityRemovalCommands::list))
                        .then(Commands.literal("stats").executes(EntityRemovalCommands::stats))
                        .then(Commands.literal("validate").executes(EntityRemovalCommands::validate))
                        .then(Commands.literal("purge").executes(EntityRemovalCommands::purge))
                        .then(Commands.literal("debug")
                                .executes(EntityRemovalCommands::debugStatus)
                                .then(Commands.argument("channel", StringArgumentType.word())
                                        .suggests(EntityRemovalCommands::suggestChannels)
                                        .then(Commands.argument("value", BoolArgumentType.bool())
                                                .executes(EntityRemovalCommands::setDebugChannel))))

                        // For debugging, removing later
                        .then(Commands.literal("testadd")
                                .then(Commands.argument("rule", StringArgumentType.word())
                                        .then(Commands.argument("id", StringArgumentType.greedyString())
                                                .executes(ctx -> {
                                                    String rule = StringArgumentType.getString(ctx, "rule");
                                                    String id = StringArgumentType.getString(ctx, "id");
                                                    if (RuleWriter.addEntity(rule, id)) {
                                                        RuleManager.reload();
                                                        ctx.getSource().sendSuccess(
                                                                () -> Component.literal("Added " + id + " to " + rule),
                                                                false);
                                                    } else {
                                                        ctx.getSource().sendFailure(
                                                                Component.literal("Failed"));
                                                    }
                                                    return 1;
                                                })))
                                .then(Commands.literal("testinput")
                                        .executes(ctx -> openTestInput(ctx, false))
                                        .then(Commands.literal("tag")
                                                .executes(ctx -> openTestInput(ctx, true)))))
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

            int idCount = rule.entities().size();
            int tagCount = rule.entityTags().size();

            String countLabel = String.valueOf(idCount);
            if (tagCount > 0) {
                countLabel += " + " + tagCount + " tag" + (tagCount == 1 ? "" : "s");
            }

            final String finalCount = countLabel;

            ctx.getSource().sendSuccess(
                    () -> Component.literal("§7").append(Component.translatable(
                            "screwyourmobs.command.list.entry",
                            rule.name(), finalCount, dims)),
                    false);
        }
        return 1;
    }

    private static int openScreen(CommandContext<CommandSourceStack> ctx) {
        if (net.neoforged.fml.loading.FMLLoader.getDist() == net.neoforged.api.distmarker.Dist.DEDICATED_SERVER) {
            ctx.getSource().sendFailure(Component.literal(
                    "§cThe rule screen is client-only. Use /symrules or open it in singleplayer."));
            return 0;
        }

        net.minecraft.client.Minecraft.getInstance().execute(() ->
                net.minecraft.client.Minecraft.getInstance().setScreen(
                        new vinn.tekk.screwyourmobs.client.RuleEditorScreen(
                                net.minecraft.client.Minecraft.getInstance().screen)));

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

    private static int setDebugMaster(CommandContext<CommandSourceStack> ctx, boolean value) {
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

        var setter = DEBUG_CHANNEL_SETTERS.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(channel))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);

        if (setter == null) {
            ctx.getSource().sendFailure(prefixed(Component.translatable(
                    "screwyourmobs.command.debug.channel.unknown", channel)));
            return 0;
        }

        setter.accept(value);
        ctx.getSource().sendSuccess(
                () -> prefixed(Component.translatable(
                        "screwyourmobs.command.debug.channel.set", channel, value)),
                true);
        return 1;
    }

    private static int openTestInput(CommandContext<CommandSourceStack> ctx, boolean tagMode) {
        if (net.neoforged.fml.loading.FMLLoader.getDist() == net.neoforged.api.distmarker.Dist.DEDICATED_SERVER) {
            ctx.getSource().sendFailure(Component.literal("§cClient-only command."));
            return 0;
        }

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        mc.execute(() -> {
            // Build the option list — all entity IDs, plus tags with "#"
            List<String> options = new java.util.ArrayList<>();
            for (ResourceLocation id : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.keySet()) {
                options.add(id.toString());
            }
            net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getTags()
                    .forEach(pair -> options.add("#" + pair.getFirst().location()));
            options.sort(String::compareToIgnoreCase);

            vinn.tekk.screwyourmobs.client.InputFlow.prompt(
                    mc,
                    mc.screen,
                    Component.literal(tagMode
                            ? "Test Input — Tag Mode"
                            : "Test Input — Entity Mode"),
                    options,
                    tagMode ? "#" : "",
                    input -> validateTestInput(input, tagMode),
                    accepted -> {
                        if (mc.player != null) {
                            mc.player.sendSystemMessage(Component.literal(
                                    "§a[ScrewYourMobs!] Accepted: §f" + accepted));
                        }
                    }
            );
        });

        ctx.getSource().sendSuccess(
                () -> Component.literal("§7Opening test input screen..."), false);
        return 1;
    }

    private static vinn.tekk.screwyourmobs.client.InputFlow.ValidationResult
    validateTestInput(String input, boolean tagMode) {
        boolean isTag = input.startsWith("#");
        String bare = isTag ? input.substring(1) : input;

        ResourceLocation rl = ResourceLocation.tryParse(bare);
        if (rl == null) {
            return vinn.tekk.screwyourmobs.client.InputFlow.ValidationResult.bad(null);
        }

        if (isTag) {
            // Check the tag exists in the registry
            boolean exists = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getTags()
                    .anyMatch(p -> p.getFirst().location().equals(rl));
            if (exists) {
                return vinn.tekk.screwyourmobs.client.InputFlow.ValidationResult.ok();
            }

            // Suggest the closest tag
            String suggestion = suggestClosestTag(rl);
            return vinn.tekk.screwyourmobs.client.InputFlow.ValidationResult.bad(
                    suggestion != null ? "#" + suggestion : null);
        }

        // Entity ID mode
        if (net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(rl)) {
            return vinn.tekk.screwyourmobs.client.InputFlow.ValidationResult.ok();
        }

        String suggestion = suggestClosestEntity(rl);
        return vinn.tekk.screwyourmobs.client.InputFlow.ValidationResult.bad(suggestion);
    }

    private static String suggestClosestEntity(ResourceLocation unknown) {
        String target = unknown.toString();
        String best = null;
        int bestDist = Integer.MAX_VALUE;
        for (ResourceLocation candidate : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.keySet()) {
            int dist = vinn.tekk.screwyourmobs.helpers.Levenshtein.distance(target, candidate.toString());
            if (dist < bestDist) {
                bestDist = dist;
                best = candidate.toString();
            }
        }
        return (best != null && bestDist <= 5) ? best : null;
    }

    private static String suggestClosestTag(ResourceLocation unknown) {
        String target = unknown.toString();
        String best = null;
        int bestDist = Integer.MAX_VALUE;
        var iter = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getTags().iterator();
        while (iter.hasNext()) {
            var pair = iter.next();
            String candidate = pair.getFirst().location().toString();
            int dist = vinn.tekk.screwyourmobs.helpers.Levenshtein.distance(target, candidate);
            if (dist < bestDist) {
                bestDist = dist;
                best = candidate;
            }
        }
        return (best != null && bestDist <= 5) ? best : null;
    }
}