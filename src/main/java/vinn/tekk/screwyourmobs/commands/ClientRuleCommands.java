package vinn.tekk.screwyourmobs.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import vinn.tekk.screwyourmobs.ScrewYourMobsMod;
import vinn.tekk.screwyourmobs.client.bench.SuggestionBenchmark;
import vinn.tekk.screwyourmobs.client.gui.RuleEditorScreen;
import vinn.tekk.screwyourmobs.config.EntityRemovalConfig;

@EventBusSubscriber(modid = ScrewYourMobsMod.MODID, value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.GAME)
public class ClientRuleCommands {

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("symrules")
                        .executes(ctx -> {
                            Minecraft mc = Minecraft.getInstance();
                            if (mc.player == null) return 0;

                            if (!mc.player.hasPermissions(2)) {
                                mc.player.sendSystemMessage(Component.literal(
                                        "§cYou need operator permissions to view or edit rules."));
                                return 0;
                            }

                            mc.execute(() -> mc.setScreen(
                                    new RuleEditorScreen(mc.screen)));
                            return 1;
                        })
        );

        dispatcher.register(
                Commands.literal("sym")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("bench")
                                .executes(ctx -> {
                                    if (!SuggestionBenchmark.isAvailable()) {
                                        ctx.getSource().sendFailure(Component.literal(
                                                "§cBenchmark is dev-only."));
                                        return 0;
                                    }
                                    SuggestionBenchmark.run();
                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal("§7Running benchmark... check the console."),
                                            false);
                                    return 1;
                                }))
                        .then(Commands.literal("accent")
                                .then(Commands.literal("reset")
                                        .executes(ctx -> {
                                            String stored = "#" + vinn.tekk.screwyourmobs.client.AccentColor.defaultHex();
                                            EntityRemovalConfig.DISPLAY_ACCENT_COLOR.set(stored);
                                            vinn.tekk.screwyourmobs.client.AccentColor.invalidateCache();
                                            EntityRemovalConfig.SPECIFICATION.save();

                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal(
                                                            "§aAccent reset to default ("
                                                                    + stored + ")."),
                                                    false);
                                            return 1;
                                        }))
                                .then(Commands.argument("color", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String raw = StringArgumentType.getString(ctx, "color");

                                            String cleaned = raw.startsWith("#") ? raw.substring(1) : raw;
                                            cleaned = cleaned.toUpperCase(java.util.Locale.ROOT);

                                            if (!cleaned.matches("[0-9A-F]{6}([0-9A-F]{2})?")) {
                                                ctx.getSource().sendFailure(Component.literal(
                                                        "§cInvalid hex color '" + raw + "'."
                                                                + " §7Expected 6 or 8 hex digits (e.g. §fFFAA00§7, §f#FFAA00§7, §fAAFFAA00§7)."));
                                                return 0;
                                            }

                                            String stored = "#" + cleaned;
                                            EntityRemovalConfig.DISPLAY_ACCENT_COLOR.set(stored);
                                            vinn.tekk.screwyourmobs.client.AccentColor.invalidateCache();
                                            EntityRemovalConfig.SPECIFICATION.save();

                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal(
                                                            "§aAccent set to §f" + stored
                                                                    + "§7. Reopen the editor to see it."),
                                                    false);
                                            return 1;
                                        })))
        );
    }
}