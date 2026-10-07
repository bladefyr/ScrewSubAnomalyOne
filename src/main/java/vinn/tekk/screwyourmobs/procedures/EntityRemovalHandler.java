package vinn.tekk.screwyourmobs.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import vinn.tekk.screwyourmobs.ScrewYourMobsMod;
import vinn.tekk.screwyourmobs.config.EntityRemovalConfig;
import vinn.tekk.screwyourmobs.debug.DebugLog;
import vinn.tekk.screwyourmobs.rules.RemovalRule;
import vinn.tekk.screwyourmobs.rules.RuleManager;

import java.util.HashSet;
import java.util.Set;

@EventBusSubscriber(modid = ScrewYourMobsMod.MODID)
public class EntityRemovalHandler {

    private static final Set<ResourceLocation> KEEP_IDS = new HashSet<>();
    private static boolean keepListLoaded = false;

    public static void markConfigForReload() {
        keepListLoaded = false;
        RuleManager.reload();
    }

    private static void loadKeepList() {
        KEEP_IDS.clear();
        for (String id : EntityRemovalConfig.getEntitiesToKeep()) {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl == null) {
                ScrewYourMobsMod.LOGGER.warn(
                        "[ScrewYourMobs!] Invalid keep-entity ID: {}", id);
                continue;
            }
            KEEP_IDS.add(rl);
        }
        keepListLoaded = true;
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;

        if (!keepListLoaded) loadKeepList();
        if (!RuleManager.hasAnyRules()) return;

        Entity entity = event.getEntity();
        EntityType<?> type = entity.getType();
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (typeId == null) return;

        // Keep-list overrides everything
        if (KEEP_IDS.contains(typeId)) return;

        ResourceLocation dimId = level.dimension().location();
        RemovalRule match = RuleManager.findMatch(typeId, dimId, type);
        if (match == null) return;

        if (EntityRemovalConfig.DEBUG_ENABLED.get()) {
            DebugLog.log(DebugLog.Channel.REMOVAL,
                    "Removed: %s (rule: %s)", typeId, match.name());
        }
        event.setCanceled(true);
    }
}