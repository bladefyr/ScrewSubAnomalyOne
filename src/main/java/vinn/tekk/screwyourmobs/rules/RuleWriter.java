package vinn.tekk.screwyourmobs.rules;

import com.google.gson.*;
import vinn.tekk.screwyourmobs.ScrewYourMobsMod;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Serializes mutations back to rule JSON files on disk.
 *
 * Design:
 *  - Load the raw JsonObject, mutate only the "entities" / "dimensions" arrays,
 *    write it back. Unknown fields (comments, future metadata) survive round-trips.
 *  - Write to a temp file, then atomic-rename over the target. A crash mid-write
 *    leaves the original intact.
 *  - Validate the mutated object before writing. Malformed output aborts the write.
 */
public final class RuleWriter {

    private RuleWriter() {}

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    // ---- Public API ----

    public static boolean addEntity(String ruleName, String entityId) {
        return mutateArray(ruleName, "entities", arr -> {
            if (arrayContains(arr, entityId)) return false;
            arr.add(entityId);
            return true;
        });
    }

    public static boolean removeEntity(String ruleName, String entityId) {
        return mutateArray(ruleName, "entities", arr -> removeFromArray(arr, entityId));
    }

    public static boolean addDimension(String ruleName, String dimensionId) {
        return mutateArray(ruleName, "dimensions", arr -> {
            if (arrayContains(arr, dimensionId)) return false;
            arr.add(dimensionId);
            return true;
        });
    }

    public static boolean removeDimension(String ruleName, String dimensionId) {
        return mutateArray(ruleName, "dimensions", arr -> removeFromArray(arr, dimensionId));
    }

    public static boolean createRule(String name, Path targetDir) {
        Path target = targetDir.resolve(name + ".json");
        if (Files.exists(target)) return false;

        JsonObject fresh = new JsonObject();
        fresh.add("entities", new JsonArray());
        fresh.add("dimensions", new JsonArray());

        return writeJson(target, fresh);
    }

    public static boolean renameRule(String oldName, String newName) {
        RuleSource source = RuleManager.getSource(oldName);
        if (source == null) return false;

        Path oldPath = source.path();
        String fileName = oldPath.getFileName().toString();
        boolean disabled = fileName.startsWith("_");

        // Preserve the disabled prefix (if any) so renaming doesn't silently enable the rule
        String prefix = disabled ? "_" : "";
        Path newPath = oldPath.resolveSibling(prefix + newName + ".json");

        if (Files.exists(newPath)) return false;

        try {
            Files.move(oldPath, newPath);
            return true;
        } catch (IOException e) {
            ScrewYourMobsMod.LOGGER.error(
                    "[ScrewYourMobs!] Could not rename rule '{}' to '{}': {}",
                    oldName, newName, e.getMessage());
            return false;
        }
    }

    // ---- Internal ----

    private static boolean mutateArray(String ruleName, String arrayField,
                                       java.util.function.Function<JsonArray, Boolean> editor) {
        RuleSource source = RuleManager.getSource(ruleName);
        if (source == null) return false;

        JsonObject raw = loadRaw(source.path());
        if (raw == null) return false;

        JsonArray arr = raw.getAsJsonArray(arrayField);
        if (arr == null) {
            arr = new JsonArray();
            raw.add(arrayField, arr);
        }

        Boolean changed = editor.apply(arr);
        if (!Boolean.TRUE.equals(changed)) return true;

        return writeJson(source.path(), raw);
    }

    private static JsonObject loadRaw(Path path) {
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) return null;
            return root.getAsJsonObject();
        } catch (IOException e) {
            ScrewYourMobsMod.LOGGER.error(
                    "[ScrewYourMobs!] Could not read rule '{}': {}", path, e.getMessage());
            return null;
        }
    }

    private static boolean writeJson(Path target, JsonObject obj) {
        String serialized = GSON.toJson(obj);

        try {
            JsonParser.parseString(serialized);
        } catch (Exception e) {
            ScrewYourMobsMod.LOGGER.error(
                    "[ScrewYourMobs!] Refusing to write malformed JSON to '{}': {}",
                    target, e.getMessage());
            return false;
        }

        Path dir = target.getParent();
        Path temp = dir.resolve("." + target.getFileName() + "." + UUID.randomUUID() + ".tmp");

        try {
            Files.createDirectories(dir);
            try (Writer w = Files.newBufferedWriter(temp)) {
                w.write(serialized);
            }
            Files.move(temp, target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            ScrewYourMobsMod.LOGGER.error(
                    "[ScrewYourMobs!] Could not write rule '{}': {}", target, e.getMessage());
            try {
                Files.deleteIfExists(temp);
            } catch (IOException ignored) {}
            return false;
        }
    }

    private static boolean arrayContains(JsonArray arr, String value) {
        for (JsonElement el : arr) {
            if (el.isJsonPrimitive() && el.getAsString().equals(value)) {
                return true;
            }
        }
        return false;
    }

    private static boolean removeFromArray(JsonArray arr, String value) {
        for (int i = 0; i < arr.size(); i++) {
            JsonElement el = arr.get(i);
            if (el.isJsonPrimitive() && el.getAsString().equals(value)) {
                arr.remove(i);
                return true;
            }
        }
        return false;
    }
}