package vinn.tekk.screwyourmobs.rules;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import vinn.tekk.screwyourmobs.config.EntityRemovalConfig;
import vinn.tekk.screwyourmobs.debug.DebugLog;

import java.nio.file.Path;
import java.util.*;

public final class RuleManager {

    private RuleManager() {}

    private static volatile Map<String, RemovalRule> RULES = Map.of();
    private static volatile Map<ResourceLocation, List<RemovalRule>> GLOBAL_INDEX = Map.of();
    private static volatile Map<ResourceLocation, Map<ResourceLocation, List<RemovalRule>>> DIMENSION_INDEX = Map.of();
    private static volatile List<String> LAST_WARNINGS = List.of();
    private static volatile int totalRules = 0;
    private static volatile int totalEntityEntries = 0;

    public static void reload() {
        Path worldRulesDir = null;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        Set<ResourceLocation> knownDimensions = new HashSet<>();

        if (server != null) {
            worldRulesDir = server.getWorldPath(LevelResource.ROOT)
                    .resolve(RuleLoader.WORLD_RULES_FOLDER);

            server.levelKeys().forEach(key -> knownDimensions.add(key.location()));
        }

        Map<String, RemovalRule> loaded = RuleLoader.loadAll(worldRulesDir);
        RULES = Collections.unmodifiableMap(loaded);

        // Validate against registries
        if (server != null && EntityRemovalConfig.RULES_VALIDATE_ON_LOAD.get()) {
            LAST_WARNINGS = RuleValidator.validate(loaded, knownDimensions);
            for (String warning : LAST_WARNINGS) {
                DebugLog.log(DebugLog.Channel.VALIDATE, "%s", warning);
            }
        } else {
            LAST_WARNINGS = List.of();
        }

        // Build index
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

        DebugLog.log(DebugLog.Channel.RELOAD,
                "Loaded %d rules covering %d entity entries (%d warnings).",
                totalRules, totalEntityEntries, LAST_WARNINGS.size());
    }

    public static RemovalRule findMatch(ResourceLocation entityId, ResourceLocation dimensionId) {
        List<RemovalRule> globalMatches = GLOBAL_INDEX.get(entityId);
        if (globalMatches != null && !globalMatches.isEmpty()) {
            return globalMatches.get(0);
        }

        Map<ResourceLocation, List<RemovalRule>> dimMap = DIMENSION_INDEX.get(dimensionId);
        if (dimMap != null) {
            List<RemovalRule> dimMatches = dimMap.get(entityId);
            if (dimMatches != null && !dimMatches.isEmpty()) {
                return dimMatches.get(0);
            }
        }

        return null;
    }

    public static boolean hasAnyRules() { return totalRules > 0; }
    public static int getTotalRules() { return totalRules; }
    public static int getTotalEntityEntries() { return totalEntityEntries; }
    public static Set<String> getRuleNames() { return RULES.keySet(); }
    public static RemovalRule getRule(String name) { return RULES.get(name); }
    public static Map<String, RemovalRule> getAllRules() { return RULES; }
    public static List<String> getLastWarnings() { return LAST_WARNINGS; }
}