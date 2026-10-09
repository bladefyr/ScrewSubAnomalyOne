package vinn.tekk.screwyourmobs.client;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import vinn.tekk.screwyourmobs.ScrewYourMobsMod;

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
    }
}