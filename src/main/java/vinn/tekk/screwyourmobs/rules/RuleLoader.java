package vinn.tekk.screwyourmobs.rules;

import vinn.tekk.screwyourmobs.ScrewYourMobsMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;

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

    /** World-relative path — do NOT resolve until a server exists. */
    public static final String WORLD_RULES_FOLDER = "serverconfig/sym_rules";

    private static final String EXAMPLE_FILE = "_example.json";
    private static final String EXAMPLE_CONTENT = """
            {
              "_comment": "Copy this file and rename it. Delete the _comment field.",
              "_comment2": "Empty 'dimensions' list means the rule applies in ALL dimensions.",
              "entities": [
                "minecraft:creeper",
                "minecraft:skeleton",
                "minecraft:zombie"
              ],
              "dimensions": [
                "minecraft:overworld"
              ]
            }
            """;

    /**
     * Reads every *.json file from both the global rules directory and (if provided) the
     * per-world rules directory. World rules override global rules with the same name.
     *
     * @param worldRulesDir the per-world rules directory, or {@code null} if no server is running.
     * @return a merged map of ruleName -> RemovalRule.
     */
    public static Map<String, RemovalRule> loadAll(Path worldRulesDir) {
        Map<String, RemovalRule> result = new HashMap<>();

        // Global rules (always)
        ensureDirectoryExists(GLOBAL_RULES_DIR);
        writeExampleIfMissing(GLOBAL_RULES_DIR);
        result.putAll(readDirectory(GLOBAL_RULES_DIR, "global"));

        // World rules (only if server is up)
        if (worldRulesDir != null) {
            ensureDirectoryExists(worldRulesDir);
            // No example written here — global example is the single teaching file.
            result.putAll(readDirectory(worldRulesDir, "world"));
        }

        return result;
    }

    private static Map<String, RemovalRule> readDirectory(Path dir, String source) {
        Map<String, RemovalRule> out = new HashMap<>();
        try (Stream<Path> paths = Files.list(dir)) {
            paths.filter(p -> p.toString().endsWith(".json"))
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        String ruleName = fileName.substring(0, fileName.length() - 5);
                        if (ruleName.startsWith("_")) return; // skip template/disabled files
                        try {
                            RemovalRule rule = parseRule(ruleName, path);
                            if (rule != null) out.put(ruleName, rule);
                        } catch (Exception e) {
                            ScrewYourMobsMod.LOGGER.error(
                                    "[ScrewYourMobs!] Failed to parse {} rule '{}': {}",
                                    source, fileName, e.getMessage());
                        }
                    });
        } catch (IOException e) {
            ScrewYourMobsMod.LOGGER.error(
                    "[ScrewYourMobs!] Failed to list {} rules dir: {}", source, e.getMessage());
        }
        return out;
    }

    private static RemovalRule parseRule(String name, Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                ScrewYourMobsMod.LOGGER.warn(
                        "[ScrewYourMobs!] Rule '{}' is not a JSON object — skipped.", name);
                return null;
            }

            JsonObject obj = root.getAsJsonObject();
            Set<ResourceLocation> entities = parseIdSet(obj.getAsJsonArray("entities"), name, "entities");
            Set<ResourceLocation> dimensions = parseIdSet(obj.getAsJsonArray("dimensions"), name, "dimensions");

            if (entities.isEmpty()) {
                ScrewYourMobsMod.LOGGER.warn(
                        "[ScrewYourMobs!] Rule '{}' has no entities — skipped.", name);
                return null;
            }

            return new RemovalRule(name, entities, dimensions);
        }
    }

    private static Set<ResourceLocation> parseIdSet(JsonArray array, String ruleName, String field) {
        Set<ResourceLocation> out = new HashSet<>();
        if (array == null) return out;

        for (JsonElement el : array) {
            if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
                ScrewYourMobsMod.LOGGER.warn(
                        "[ScrewYourMobs!] Rule '{}' field '{}' has non-string entry — skipped.",
                        ruleName, field);
                continue;
            }
            String raw = el.getAsString().trim();
            if (raw.isEmpty()) continue;

            ResourceLocation rl = ResourceLocation.tryParse(raw);
            if (rl == null) {
                ScrewYourMobsMod.LOGGER.warn(
                        "[ScrewYourMobs!] Rule '{}' field '{}' has invalid ID '{}' — skipped.",
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
                ScrewYourMobsMod.LOGGER.info(
                        "[ScrewYourMobs!] Created rules directory: {}", dir);
            }
        } catch (IOException e) {
            ScrewYourMobsMod.LOGGER.error(
                    "[ScrewYourMobs!] Could not create rules directory '{}': {}",
                    dir, e.getMessage());
        }
    }

    private static void writeExampleIfMissing(Path dir) {
        if (dir == null) return;
        Path example = dir.resolve(EXAMPLE_FILE);
        if (Files.exists(example)) return;
        try (Writer w = Files.newBufferedWriter(example)) {
            w.write(EXAMPLE_CONTENT);
            ScrewYourMobsMod.LOGGER.info(
                    "[ScrewYourMobs!] Wrote example rule file: {}", example);
        } catch (IOException e) {
            ScrewYourMobsMod.LOGGER.error(
                    "[ScrewYourMobs!] Could not write example file '{}': {}",
                    example, e.getMessage());
        }
    }
}