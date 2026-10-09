package vinn.tekk.screwyourmobs.rules;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import vinn.tekk.screwyourmobs.ScrewYourMobsMod;
import vinn.tekk.screwyourmobs.config.EntityRemovalConfig;
import vinn.tekk.screwyourmobs.debug.DebugLog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public final class RuleManager {

    private RuleManager() {}

    private static volatile Map<String, RemovalRule> RULES = Map.of();
    private static volatile Map<String, JsonObject> RAW_RULES = Map.of();
    private static volatile Map<String, RuleSource> RULE_SOURCES = Map.of();
    private static volatile Map<String, RemovalRule> DISABLED_RULES = Map.of();
    private static volatile Map<ResourceLocation, List<RemovalRule>> GLOBAL_INDEX = Map.of();
    private static volatile Map<ResourceLocation, Map<ResourceLocation, List<RemovalRule>>> DIMENSION_INDEX = Map.of();
    private static volatile Map<TagKey<EntityType<?>>, List<RemovalRule>> GLOBAL_TAG_INDEX = Map.of();
    private static volatile Map<ResourceLocation, Map<TagKey<EntityType<?>>, List<RemovalRule>>> DIMENSION_TAG_INDEX = Map.of();
    private static volatile List<String> KNOWN_DIMENSIONS = List.of();
    private static volatile List<String> LAST_WARNINGS = List.of();
    private static volatile int totalRules = 0;
    private static volatile int totalEntityEntries = 0;

    private static final List<Runnable> SYNC_LISTENERS = new CopyOnWriteArrayList<>();

    public static void addSyncListener(Runnable listener) {
        SYNC_LISTENERS.add(listener);
    }

    public static void removeSyncListener(Runnable listener) {
        SYNC_LISTENERS.remove(listener);
    }

    public static void reload() {
        Path worldRulesDir = null;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        Set<ResourceLocation> knownDimensions = new HashSet<>();

        if (server != null) {
            worldRulesDir = server.getWorldPath(LevelResource.ROOT)
                    .resolve(RuleLoader.WORLD_RULES_FOLDER);

            server.levelKeys().forEach(key -> knownDimensions.add(key.location()));
        }

        RuleLoader.LoadResult result = RuleLoader.loadAll(worldRulesDir);
        RAW_RULES = Collections.unmodifiableMap(result.rawRules());
        Map<String, RemovalRule> loaded = result.activeRules();

        RULES = Collections.unmodifiableMap(loaded);
        DISABLED_RULES = Collections.unmodifiableMap(result.disabledRules());
        RULE_SOURCES = Collections.unmodifiableMap(result.sources());

        if (server != null && EntityRemovalConfig.RULES_VALIDATE_ON_LOAD.get()) {
            LAST_WARNINGS = RuleValidator.validate(loaded, knownDimensions);
            for (String warning : LAST_WARNINGS) {
                DebugLog.log(DebugLog.Channel.VALIDATE, "%s", warning);
            }
        } else {
            LAST_WARNINGS = List.of();
        }

        List<String> dims = new ArrayList<>();
        if (server != null) {
            server.levelKeys().forEach(key -> dims.add(key.location().toString()));
        }
        dims.sort(String::compareToIgnoreCase);
        KNOWN_DIMENSIONS = Collections.unmodifiableList(dims);

        rebuildIndexes(loaded);

        DebugLog.log(DebugLog.Channel.RELOAD,
                "Loaded %d rules covering %d entity entries (%d warnings).",
                totalRules, totalEntityEntries, LAST_WARNINGS.size());
    }

    public static RemovalRule findMatch(ResourceLocation entityId, ResourceLocation dimensionId,
                                        EntityType<?> entityType) {
        // 1. Global entity ID match
        List<RemovalRule> globalMatches = GLOBAL_INDEX.get(entityId);
        if (globalMatches != null && !globalMatches.isEmpty()) {
            return globalMatches.getFirst();
        }

        // 2. Global tag match
        for (Map.Entry<TagKey<EntityType<?>>, List<RemovalRule>> entry : GLOBAL_TAG_INDEX.entrySet()) {
            if (BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(entityType).is(entry.getKey())) {
                return entry.getValue().getFirst();
            }
        }

        // 3. Dimension-specific entity ID match
        Map<ResourceLocation, List<RemovalRule>> dimMap = DIMENSION_INDEX.get(dimensionId);
        if (dimMap != null) {
            List<RemovalRule> dimMatches = dimMap.get(entityId);
            if (dimMatches != null && !dimMatches.isEmpty()) {
                return dimMatches.getFirst();
            }
        }

        // 4. Dimension-specific tag match
        Map<TagKey<EntityType<?>>, List<RemovalRule>> dimTagMap = DIMENSION_TAG_INDEX.get(dimensionId);
        if (dimTagMap != null) {
            for (Map.Entry<TagKey<EntityType<?>>, List<RemovalRule>> entry : dimTagMap.entrySet()) {
                if (BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(entityType).is(entry.getKey())) {
                    return entry.getValue().getFirst();
                }
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
    public static List<String> getKnownDimensions() { return KNOWN_DIMENSIONS; }

    public static RuleSource getSource(String name) {
        return RULE_SOURCES.get(name);
    }

    public static Map<String, RuleSource> getAllSources() {
        return RULE_SOURCES;
    }

    public static Map<String, RemovalRule> getDisabledRules() {
        return DISABLED_RULES;
    }

    public static JsonObject getRawRule(String name) {
        return RAW_RULES.get(name);
    }

    public static RemovalRule getRuleIncludingDisabled(String name) {
        RemovalRule r = RULES.get(name);
        return r != null ? r : DISABLED_RULES.get(name);
    }

    public static boolean isDisabled(String name) {
        return DISABLED_RULES.containsKey(name);
    }

    public static boolean toggleDisabled(String name) {
        RuleSource source = RULE_SOURCES.get(name);
        if (source == null) return false;

        Path path = source.path();
        String fileName = path.getFileName().toString();
        Path target;

        if (fileName.startsWith("_")) {
            target = path.resolveSibling(fileName.substring(1));
        } else {
            target = path.resolveSibling("_" + fileName);
        }

        try {
            Files.move(path, target);
            return true;
        } catch (IOException e) {
            ScrewYourMobsMod.LOGGER.error(
                    "[ScrewYourMobs!] Could not toggle rule '{}': {}", name, e.getMessage());
            return false;
        }
    }

    public static void applySync(RuleSetSnapshot snapshot) {
        Map<String, RemovalRule> active = new HashMap<>();
        Map<String, RemovalRule> disabled = new HashMap<>();
        Map<String, RuleSource> sources = new HashMap<>();

        for (RuleSetSnapshot.RuleEntry e : snapshot.rules()) {
            Set<ResourceLocation> entityIds = new HashSet<>();
            Set<TagKey<EntityType<?>>> entityTags = new HashSet<>();
            for (String s : e.entities()) {
                if (s.startsWith("#")) {
                    ResourceLocation rl = ResourceLocation.tryParse(s.substring(1));
                    if (rl != null) entityTags.add(TagKey.create(Registries.ENTITY_TYPE, rl));
                } else {
                    ResourceLocation rl = ResourceLocation.tryParse(s);
                    if (rl != null) entityIds.add(rl);
                }
            }

            Set<ResourceLocation> dims = new HashSet<>();
            for (String s : e.dimensions()) {
                ResourceLocation rl = ResourceLocation.tryParse(s);
                if (rl != null) dims.add(rl);
            }

            RemovalRule rule = new RemovalRule(
                    e.name(), entityIds, entityTags, dims, e.disabled());

            if (e.disabled()) disabled.put(e.name(), rule);
            else active.put(e.name(), rule);

            sources.put(e.name(), new RuleSource(
                    e.name(),
                    java.nio.file.Path.of(e.displayPath()),
                    e.isWorldRule()));
        }

        RULES = Collections.unmodifiableMap(active);
        DISABLED_RULES = Collections.unmodifiableMap(disabled);
        RULE_SOURCES = Collections.unmodifiableMap(sources);
        RAW_RULES = Map.of();
        LAST_WARNINGS = List.copyOf(snapshot.warnings());

        List<String> syncedDims = new ArrayList<>(snapshot.knownDimensions());
        syncedDims.sort(String::compareToIgnoreCase);
        KNOWN_DIMENSIONS = Collections.unmodifiableList(syncedDims);

        rebuildIndexes(active);

        for (Runnable listener : SYNC_LISTENERS) {
            try {
                listener.run();
            } catch (Exception e) {
                ScrewYourMobsMod.LOGGER.error(
                        "[ScrewYourMobs!] Sync listener failed: {}", e.getMessage());
            }
        }
    }

    private static void rebuildIndexes(Map<String, RemovalRule> loaded) {
        Map<ResourceLocation, List<RemovalRule>> global = new HashMap<>();
        Map<ResourceLocation, Map<ResourceLocation, List<RemovalRule>>> perDim = new HashMap<>();
        Map<TagKey<EntityType<?>>, List<RemovalRule>> globalTags = new HashMap<>();
        Map<ResourceLocation, Map<TagKey<EntityType<?>>, List<RemovalRule>>> perDimTags = new HashMap<>();

        int entityCount = 0;

        for (RemovalRule rule : loaded.values()) {
            entityCount += rule.entities().size() + rule.entityTags().size();

            if (rule.isGlobal()) {
                for (ResourceLocation entity : rule.entities()) {
                    global.computeIfAbsent(entity, k -> new ArrayList<>()).add(rule);
                }
                for (TagKey<EntityType<?>> tag : rule.entityTags()) {
                    globalTags.computeIfAbsent(tag, k -> new ArrayList<>()).add(rule);
                }
            } else {
                for (ResourceLocation dim : rule.dimensions()) {
                    Map<ResourceLocation, List<RemovalRule>> dimMap =
                            perDim.computeIfAbsent(dim, k -> new HashMap<>());
                    for (ResourceLocation entity : rule.entities()) {
                        dimMap.computeIfAbsent(entity, k -> new ArrayList<>()).add(rule);
                    }

                    Map<TagKey<EntityType<?>>, List<RemovalRule>> dimTagMap =
                            perDimTags.computeIfAbsent(dim, k -> new HashMap<>());
                    for (TagKey<EntityType<?>> tag : rule.entityTags()) {
                        dimTagMap.computeIfAbsent(tag, k -> new ArrayList<>()).add(rule);
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

        Map<TagKey<EntityType<?>>, List<RemovalRule>> frozenGlobalTags = new HashMap<>();
        globalTags.forEach((k, v) -> frozenGlobalTags.put(k, List.copyOf(v)));

        Map<ResourceLocation, Map<TagKey<EntityType<?>>, List<RemovalRule>>> frozenPerDimTags = new HashMap<>();
        perDimTags.forEach((dim, tagMap) -> {
            Map<TagKey<EntityType<?>>, List<RemovalRule>> inner = new HashMap<>();
            tagMap.forEach((tag, list) -> inner.put(tag, List.copyOf(list)));
            frozenPerDimTags.put(dim, Collections.unmodifiableMap(inner));
        });

        GLOBAL_INDEX = Collections.unmodifiableMap(frozenGlobal);
        DIMENSION_INDEX = Collections.unmodifiableMap(frozenPerDim);
        GLOBAL_TAG_INDEX = Collections.unmodifiableMap(frozenGlobalTags);
        DIMENSION_TAG_INDEX = Collections.unmodifiableMap(frozenPerDimTags);

        totalRules = loaded.size();
        totalEntityEntries = entityCount;
    }

    public static boolean deleteRule(String name) {
        RuleSource source = RULE_SOURCES.get(name);
        if (source == null) return false;

        try {
            Files.delete(source.path());
            return true;
        } catch (IOException e) {
            ScrewYourMobsMod.LOGGER.error(
                    "[ScrewYourMobs!] Could not delete rule '{}': {}", name, e.getMessage());
            return false;
        }
    }

    public static void clearClientCache() {
        RULES = Map.of();
        RAW_RULES = Map.of();
        RULE_SOURCES = Map.of();
        DISABLED_RULES = Map.of();
        GLOBAL_INDEX = Map.of();
        DIMENSION_INDEX = Map.of();
        GLOBAL_TAG_INDEX = Map.of();
        DIMENSION_TAG_INDEX = Map.of();
        KNOWN_DIMENSIONS = List.of();
        LAST_WARNINGS = List.of();
        totalRules = 0;
        totalEntityEntries = 0;
    }
}