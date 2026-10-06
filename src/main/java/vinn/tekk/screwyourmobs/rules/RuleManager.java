package vinn.tekk.screwyourmobs.rules;

import vinn.tekk.screwyourmobs.ScrewYourMobsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Holds all loaded rules and a precomputed lookup index.
 *
 * Index structure:
 *   GLOBAL_INDEX: entityId -> rules that apply in all dimensions
 *   DIMENSION_INDEX: dimensionId -> (entityId -> rules that apply in that dimension)
 *
 * Spawn check is O(1) hashmap lookup in the worst case.
 */
public final class RuleManager {

    private RuleManager() {}

    private static volatile Map<String, RemovalRule> RULES = Map.of();

    // Precomputed lookup tables
    private static volatile Map<ResourceLocation, List<RemovalRule>> GLOBAL_INDEX = Map.of();
    private static volatile Map<ResourceLocation, Map<ResourceLocation, List<RemovalRule>>> DIMENSION_INDEX = Map.of();

    // Diagnostics
    private static volatile int totalRules = 0;
    private static volatile int totalEntityEntries = 0;

    public static void reload() {
        Path worldRulesDir = null;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            worldRulesDir = server.getWorldPath(LevelResource.ROOT)
                    .resolve(RuleLoader.WORLD_RULES_FOLDER);
        }

        Map<String, RemovalRule> loaded = RuleLoader.loadAll(worldRulesDir);
        RULES = Collections.unmodifiableMap(loaded);

        Map<ResourceLocation, List<RemovalRule>> global = new HashMap<>();
        Map<ResourceLocation, Map<ResourceLocation, List<RemovalRule>>> perDim = new HashMap<>();

        int entityCount = 0;

        for (RemovalRule rule : loaded.values()) {
            entityCount += rule.entities().size();

            if (rule.isGlobal()) {
                for (ResourceLocation entity : rule.entities()) {
                    global.computeIfAbsent(entity, k -> new ArrayList<>()).add(rule);
                }
            } else {
                for (ResourceLocation dim : rule.dimensions()) {
                    Map<ResourceLocation, List<RemovalRule>> dimMap =
                            perDim.computeIfAbsent(dim, k -> new HashMap<>());
                    for (ResourceLocation entity : rule.entities()) {
                        dimMap.computeIfAbsent(entity, k -> new ArrayList<>()).add(rule);
                    }
                }
            }
        }

        // Freeze for thread safety
        Map<ResourceLocation, List<RemovalRule>> frozenGlobal = new HashMap<>();
        global.forEach((k, v) -> frozenGlobal.put(k, List.copyOf(v)));

        Map<ResourceLocation, Map<ResourceLocation, List<RemovalRule>>> frozenPerDim = new HashMap<>();
        perDim.forEach((dim, entityMap) -> {
            Map<ResourceLocation, List<RemovalRule>> inner = new HashMap<>();
            entityMap.forEach((entity, list) -> inner.put(entity, List.copyOf(list)));
            frozenPerDim.put(dim, Collections.unmodifiableMap(inner));
        });

        GLOBAL_INDEX = Collections.unmodifiableMap(frozenGlobal);
        DIMENSION_INDEX = Collections.unmodifiableMap(frozenPerDim);
        totalRules = loaded.size();
        totalEntityEntries = entityCount;

        ScrewYourMobsMod.LOGGER.info(
                "[ScrewYourMobs!] Loaded {} rules covering {} entity entries.",
                totalRules, totalEntityEntries);
    }

    /**
     * @return the matching rule if the entity should be removed, or null otherwise.
     */
    public static RemovalRule findMatch(ResourceLocation entityId, ResourceLocation dimensionId) {
        // 1. Global rules
        List<RemovalRule> globalMatches = GLOBAL_INDEX.get(entityId);
        if (globalMatches != null && !globalMatches.isEmpty()) {
            return globalMatches.get(0);
        }

        // 2. Dimension-specific
        Map<ResourceLocation, List<RemovalRule>> dimMap = DIMENSION_INDEX.get(dimensionId);
        if (dimMap != null) {
            List<RemovalRule> dimMatches = dimMap.get(entityId);
            if (dimMatches != null && !dimMatches.isEmpty()) {
                return dimMatches.get(0);
            }
        }

        return null;
    }

    public static boolean hasAnyRules() {
        return totalRules > 0;
    }

    public static int getTotalRules() {
        return totalRules;
    }

    public static int getTotalEntityEntries() {
        return totalEntityEntries;
    }

    public static Set<String> getRuleNames() {
        return RULES.keySet();
    }

    public static RemovalRule getRule(String name) {
        return RULES.get(name);
    }

    public static Map<String, RemovalRule> getAllRules() {
        return RULES;
    }
}