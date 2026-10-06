package vinn.tekk.screwyourmobs.commands;

import vinn.tekk.screwyourmobs.ScrewYourMobsMod;
import vinn.tekk.screwyourmobs.procedures.EntityPurger;
import vinn.tekk.screwyourmobs.rules.RemovalRule;
import vinn.tekk.screwyourmobs.rules.RuleManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

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
                        .then(Commands.literal("purge").executes(EntityRemovalCommands::purge))
                        .then(Commands.literal("list").executes(EntityRemovalCommands::list))
                        .then(Commands.literal("stats").executes(EntityRemovalCommands::stats))
        );
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        RuleManager.reload();
        int purged = EntityPurger.purgeAll();
        int ruleCount = RuleManager.getTotalRules();
        ctx.getSource().sendSuccess(
                () -> Component.literal("§a[ScrewYourMobs!] Reloaded " + ruleCount
                        + " rules. Purged " + purged + " existing entities."),
                true);
        return 1;
    }

    private static int purge(CommandContext<CommandSourceStack> ctx) {
        int purged = EntityPurger.purgeAll();
        ctx.getSource().sendSuccess(
                () -> Component.literal("§a[ScrewYourMobs!] Purged " + purged + " existing entities."),
                true);
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
                        + RuleManager.getTotalEntityEntries()),
                false);
        return 1;
    }
}