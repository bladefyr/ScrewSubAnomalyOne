package vinn.tekk.screwyourmobs.rules;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.neoforged.fml.loading.FMLPaths;
import vinn.tekk.screwyourmobs.ScrewYourMobsMod;
import vinn.tekk.screwyourmobs.debug.DebugLog;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public final class RuleLoader {

    private RuleLoader() {}

    public static final Path GLOBAL_RULES_DIR =
            FMLPaths.CONFIGDIR.get().resolve("sym").resolve("rules");

    public static final String WORLD_RULES_FOLDER = "serverconfig/sym_rules";
    private static final String SEED_MARKER = ".s1_seeded";
    private static final String DISABLED_PREFIX = "_";

    private record DefaultFile(String name, String content, boolean seedOnce) {}
    private static final String EXAMPLE_FILE = "_example.json";

    private static final DefaultFile EXAMPLE = new DefaultFile(
            "_example.json",
            """
            {
              "_comment": "Copy this file and rename it. Delete the _comment fields if you want to.",
              "_comment2": "Empty 'dimensions' list means the rule applies in ALL dimensions.",
              "_comment3": "Tags like '#minecraft:raiders' are also supported.",
              "entities": [
                "minecraft:creeper",
                "minecraft:skeleton",
                "#minecraft:raiders"
              ],
              "dimensions": [
                "minecraft:overworld"
              ]
            }
            """,
            false);

    private static final DefaultFile EASTER_EGG = new DefaultFile(
            "_Screw_SubanomalyOne.json",
            """
            {
              "_comment": "in memory of Screw_SubAnomalyOne (2025-2026)",
              "_comment2": "rename this file to remove the underscore if you want him gone for good.",
              "entities": [
                "thebrokenscript:sub_anomaly_1"
              ],
              "dimensions": []
            }
            """,
            true);

    private static final DefaultFile[] DEFAULT_FILES = { EXAMPLE, EASTER_EGG };

    public record LoadResult(
            Map<String, RemovalRule> activeRules,
            Map<String, RemovalRule> disabledRules,
            Map<String, RuleSource> sources,
            Map<String, JsonObject> rawRules
    ) {}

    public static LoadResult loadAll(Path worldRulesDir) {
        Map<String, RemovalRule> active = new HashMap<>();
        Map<String, RemovalRule> disabled = new HashMap<>();
        Map<String, RuleSource> sources = new HashMap<>();
        Map<String, JsonObject> raws = new HashMap<>();

        ensureDirectoryExists(GLOBAL_RULES_DIR);
        writeDefaultsIfMissing(GLOBAL_RULES_DIR);
        readInto(GLOBAL_RULES_DIR, false, active, disabled, sources, raws);

        if (worldRulesDir != null) {
            ensureDirectoryExists(worldRulesDir);
            readInto(worldRulesDir, true, active, disabled, sources, raws);
        }

        return new LoadResult(active, disabled, sources, raws);
    }

    private static void readInto(Path dir, boolean isWorld,
                                 Map<String, RemovalRule> outActive,
                                 Map<String, RemovalRule> outDisabled,
                                 Map<String, RuleSource> outSources,
                                 Map<String, JsonObject> outRaws) {
        try (Stream<Path> paths = Files.list(dir)) {
            paths.filter(p -> p.toString().endsWith(".json"))
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        if (fileName.equals(EXAMPLE_FILE)) return;   // hide template

                        String ruleName = fileName.substring(0, fileName.length() - 5);
                        if (ruleName.isEmpty()) return;

                        boolean isDisabled = ruleName.startsWith(DISABLED_PREFIX);
                        String cleanName = isDisabled ? ruleName.substring(1) : ruleName;
                        if (cleanName.isEmpty()) return;

                        try {
                            JsonObject raw = readRawObject(path);
                            if (raw == null) return;

                            RemovalRule rule = parseRule(cleanName, raw, isDisabled);
                            if (rule == null) return;

                            if (isDisabled) outDisabled.put(cleanName, rule);
                            else outActive.put(cleanName, rule);

                            outSources.put(cleanName, new RuleSource(cleanName, path, isWorld));
                            outRaws.put(cleanName, raw);
                        } catch (Exception e) {
                            DebugLog.log(DebugLog.Channel.RULE_ERROR,
                                    "Failed to parse %s rule '%s': %s",
                                    isWorld ? "world" : "global", fileName, e.getMessage());
                        }
                    });
        } catch (IOException e) {
            DebugLog.log(DebugLog.Channel.RULE_ERROR,
                    "Failed to list %s rules dir: %s",
                    isWorld ? "world" : "global", e.getMessage());
        }
    }

    /** Read the raw JSON object from disk, or null if malformed. */
    private static JsonObject readRawObject(Path path) {
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) return null;
            return root.getAsJsonObject();
        } catch (IOException e) {
            return null;
        }
    }

    /** Parse a rule from a pre-read JsonObject. */
    private static RemovalRule parseRule(String name, JsonObject obj, boolean disabled) {
        Set<ResourceLocation> entities = new HashSet<>();
        Set<TagKey<EntityType<?>>> entityTags = new HashSet<>();
        parseEntityEntries(obj.getAsJsonArray("entities"), name, entities, entityTags);

        Set<ResourceLocation> dimensions = parseIdSet(obj.getAsJsonArray("dimensions"), name, "dimensions");

        if (entities.isEmpty() && entityTags.isEmpty()) {
            DebugLog.log(DebugLog.Channel.RULE_ERROR,
                    "Rule '%s' has no entities or tags - skipped.", name);
            return null;
        }

        return new RemovalRule(name, entities, entityTags, dimensions, disabled);
    }

    private static RemovalRule parseRule(String name, Path path, boolean disabled) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                DebugLog.log(DebugLog.Channel.RULE_ERROR,
                        "Rule '%s' is not a JSON object - skipped.", name);
                return null;
            }

            JsonObject obj = root.getAsJsonObject();

            Set<ResourceLocation> entities = new HashSet<>();
            Set<TagKey<EntityType<?>>> entityTags = new HashSet<>();
            parseEntityEntries(obj.getAsJsonArray("entities"), name, entities, entityTags);

            Set<ResourceLocation> dimensions = parseIdSet(obj.getAsJsonArray("dimensions"), name, "dimensions");

            if (entities.isEmpty() && entityTags.isEmpty()) {
                DebugLog.log(DebugLog.Channel.RULE_ERROR,
                        "Rule '%s' has no entities or tags - skipped.", name);
                return null;
            }

            return new RemovalRule(name, entities, entityTags, dimensions, disabled);
        }
    }

    private static void parseEntityEntries(JsonArray array, String ruleName,
                                           Set<ResourceLocation> outIds,
                                           Set<TagKey<EntityType<?>>> outTags) {
        if (array == null) return;

        for (JsonElement el : array) {
            if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
                DebugLog.log(DebugLog.Channel.RULE_ERROR,
                        "Rule '%s' field 'entities' has non-string entry - skipped.", ruleName);
                continue;
            }
            String raw = el.getAsString().trim();
            if (raw.isEmpty()) continue;

            if (raw.startsWith("#")) {
                String tagId = raw.substring(1);
                ResourceLocation rl = ResourceLocation.tryParse(tagId);
                if (rl == null) {
                    DebugLog.log(DebugLog.Channel.RULE_ERROR,
                            "Rule '%s' has invalid entity tag '%s' - skipped.", ruleName, raw);
                    continue;
                }
                outTags.add(TagKey.create(Registries.ENTITY_TYPE, rl));
            } else {
                ResourceLocation rl = ResourceLocation.tryParse(raw);
                if (rl == null) {
                    DebugLog.log(DebugLog.Channel.RULE_ERROR,
                            "Rule '%s' has invalid entity ID '%s' - skipped.", ruleName, raw);
                    continue;
                }
                outIds.add(rl);
            }
        }
    }

    private static Set<ResourceLocation> parseIdSet(JsonArray array, String ruleName, String field) {
        Set<ResourceLocation> out = new HashSet<>();
        if (array == null) return out;

        for (JsonElement el : array) {
            if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
                DebugLog.log(DebugLog.Channel.RULE_ERROR,
                        "Rule '%s' field '%s' has non-string entry - skipped.",
                        ruleName, field);
                continue;
            }
            String raw = el.getAsString().trim();
            if (raw.isEmpty()) continue;

            ResourceLocation rl = ResourceLocation.tryParse(raw);
            if (rl == null) {
                DebugLog.log(DebugLog.Channel.RULE_ERROR,
                        "Rule '%s' field '%s' has invalid ID '%s' - skipped.",
                        ruleName, field, raw);
                continue;
            }
            out.add(rl);
        }
        return out;
    }

    private static void ensureDirectoryExists(Path dir) {
        if (dir == null) return;
        try {
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
                DebugLog.log(DebugLog.Channel.RELOAD,
                        "Created rules directory: %s", dir);
            }
        } catch (IOException e) {
            ScrewYourMobsMod.LOGGER.error(
                    "[ScrewYourMobs!] Could not create rules directory '{}': {}",
                    dir, e.getMessage());
        }
    }

    private static void writeDefaultsIfMissing(Path dir) {
        if (dir == null) return;

        for (DefaultFile file : DEFAULT_FILES) {
            Path target = dir.resolve(file.name());

            if (file.seedOnce()) {
                Path marker = dir.resolve(SEED_MARKER);
                if (Files.exists(marker)) continue;
                if (Files.exists(target)) continue;
            } else {
                if (Files.exists(target)) continue;
            }

            try (Writer w = Files.newBufferedWriter(target)) {
                w.write(file.content());
                DebugLog.log(DebugLog.Channel.RELOAD,
                        "Wrote default file: %s", target);

                if (file.seedOnce()) {
                    Path marker = dir.resolve(SEED_MARKER);
                    try (Writer m = Files.newBufferedWriter(marker)) {
                        m.write("This folder has already been seeded with the easter egg.\n");
                        m.write("Delete this file if you want it back on next launch.\n");
                    }
                }
            } catch (IOException e) {
                ScrewYourMobsMod.LOGGER.error(
                        "[ScrewYourMobs!] Could not write default file '{}': {}",
                        target, e.getMessage());
            }
        }
    }
}