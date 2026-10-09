package vinn.tekk.screwyourmobs.client;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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
                            Minecraft.getInstance().execute(() ->
                                    Minecraft.getInstance().setScreen(
                                            new RuleEditorScreen(Minecraft.getInstance().screen)));
                            return 1;
                        })
        );
    }
}