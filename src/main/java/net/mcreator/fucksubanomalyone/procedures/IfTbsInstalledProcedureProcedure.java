package net.mcreator.fucksubanomalyone.procedures;

import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.ModList;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;

import net.mcreator.fucksubanomalyone.network.FuckSubanomalyoneModVariables;

import javax.annotation.Nullable;

@EventBusSubscriber
public class IfTbsInstalledProcedureProcedure {
	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		execute(event, event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), event.getEntity());
	}

	public static boolean execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		return execute(null, world, x, y, z, entity);
	}

	private static boolean execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return false;
		if (ModList.get().isLoaded("thebrokenscript")) {
			if (world instanceof Level _level) {
				if (!_level.isClientSide()) {
					_level.playSound(null, BlockPos.containing(x, y, z), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.experience_orb.pickup")), SoundSource.NEUTRAL, (float) 0.8, 1);
				} else {
					_level.playLocalSound(x, y, z, BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.experience_orb.pickup")), SoundSource.NEUTRAL, (float) 0.8, 1, false);
				}
			}
			if (!world.isClientSide() && world.getServer() != null)
				world.getServer().getPlayerList().broadcastSystemMessage(Component.literal("\u00A72TheBrokenScript has been found, \"Fuck SubAnomalyOne\" will work."), false);
			{
				FuckSubanomalyoneModVariables.PlayerVariables _vars = entity.getData(FuckSubanomalyoneModVariables.PLAYER_VARIABLES);
				_vars.isTBSInstalled = true;
				_vars.syncPlayerVariables(entity);
			}
			return true;
		}
		if (world instanceof Level _level) {
			if (!_level.isClientSide()) {
				_level.playSound(null, BlockPos.containing(x, y, z), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.redstone_torch.burnout")), SoundSource.NEUTRAL, (float) 0.8, (float) 1.1);
			} else {
				_level.playLocalSound(x, y, z, BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.redstone_torch.burnout")), SoundSource.NEUTRAL, (float) 0.8, (float) 1.1, false);
			}
		}
		if (!world.isClientSide() && world.getServer() != null)
			world.getServer().getPlayerList().broadcastSystemMessage(Component.literal("\u00A74TheBrokenScript is not installed! \"Fuck SubAnomalyOne\" won't be doing anything."), false);
		{
			FuckSubanomalyoneModVariables.PlayerVariables _vars = entity.getData(FuckSubanomalyoneModVariables.PLAYER_VARIABLES);
			_vars.isTBSInstalled = false;
			_vars.syncPlayerVariables(entity);
		}
		return false;
	}
}
