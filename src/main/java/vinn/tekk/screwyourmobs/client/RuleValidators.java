package vinn.tekk.screwyourmobs.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import vinn.tekk.screwyourmobs.helpers.Levenshtein;
import vinn.tekk.screwyourmobs.rules.RuleManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Validators and option-list builders for the three input types the editor
 * uses: entity/tag IDs, dimension IDs, and rule names.
 */
public final class RuleValidators {

    private RuleValidators() {}

    private static final int MAX_SUGGESTION_DISTANCE = 5;

    // ---- Option lists ----

    /** All entity IDs plus all entity tags (with a "#" prefix). */
    public static List<String> entityOptions() {
        List<String> options = new ArrayList<>();

        for (ResourceLocation id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            options.add(id.toString());
        }

        BuiltInRegistries.ENTITY_TYPE.getTags()
                .forEach(pair -> options.add("#" + pair.getFirst().location()));

        options.sort(String::compareToIgnoreCase);
        return options;
    }

    /** All currently-loaded dimension IDs. */
    public static List<String> dimensionOptions() {
        List<String> options = new ArrayList<>();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.levelKeys().forEach(key -> options.add(key.location().toString()));
        }
        options.sort(String::compareToIgnoreCase);
        return options;
    }

    // ---- Validators ----

    /**
     * Validates an entity ID or an entity tag. Accepts either
     * "namespace:id" or "#namespace:tag".
     */
    public static InputFlow.ValidationResult validateEntity(String input) {
        if (input == null || input.isEmpty()) {
            return InputFlow.ValidationResult.bad(null);
        }

        boolean isTag = input.startsWith("#");
        String bare = isTag ? input.substring(1) : input;
        ResourceLocation rl = ResourceLocation.tryParse(bare);

        if (rl == null) {
            return InputFlow.ValidationResult.bad(null);
        }

        if (isTag) {
            boolean exists = BuiltInRegistries.ENTITY_TYPE.getTags()
                    .anyMatch(pair -> pair.getFirst().location().equals(rl));
            if (exists) return InputFlow.ValidationResult.ok();

            String suggestion = suggestClosestTag(rl);
            return InputFlow.ValidationResult.bad(
                    suggestion != null ? "#" + suggestion : null);
        }

        if (BuiltInRegistries.ENTITY_TYPE.containsKey(rl)) {
            return InputFlow.ValidationResult.ok();
        }

        return InputFlow.ValidationResult.bad(suggestClosestEntity(rl));
    }

    /** Validates a dimension ID against the currently-loaded dimensions. */
    public static InputFlow.ValidationResult validateDimension(String input) {
        if (input == null || input.isEmpty()) {
            return InputFlow.ValidationResult.bad(null);
        }

        ResourceLocation rl = ResourceLocation.tryParse(input);
        if (rl == null) {
            return InputFlow.ValidationResult.bad(null);
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return InputFlow.ValidationResult.bad(null);
        }

        boolean exists = server.levelKeys().stream()
                .anyMatch(key -> key.location().equals(rl));

        if (exists) return InputFlow.ValidationResult.ok();

        return InputFlow.ValidationResult.bad(suggestClosestDimension(rl, server));
    }

    /**
     * Validates a new rule name. Rejects empty, non-alphanumeric names,
     * and names that already exist (suggesting name_2, name_3, ...).
     */
    public static InputFlow.ValidationResult validateRuleName(String input) {
        if (input == null || input.isEmpty()) {
            return InputFlow.ValidationResult.bad(null);
        }

        if (input.startsWith("_")) {
            // Leading underscore means "disabled" on disk; reject to avoid confusion
            return InputFlow.ValidationResult.bad(null);
        }

        if (!input.matches("[a-zA-Z0-9_\\-]+")) {
            return InputFlow.ValidationResult.bad(null);
        }

        // Check for name collision
        if (RuleManager.getRuleNames().contains(input)
                || RuleManager.getDisabledRules().containsKey(input)) {
            return InputFlow.ValidationResult.bad(suggestUniqueName(input));
        }

        return InputFlow.ValidationResult.ok();
    }

    // ---- Suggestions ----

    private static String suggestClosestEntity(ResourceLocation unknown) {
        String target = unknown.toString();
        String best = null;
        int bestDist = Integer.MAX_VALUE;

        for (ResourceLocation candidate : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            int dist = Levenshtein.distance(target, candidate.toString());
            if (dist < bestDist) {
                bestDist = dist;
                best = candidate.toString();
            }
        }

        return (best != null && bestDist <= MAX_SUGGESTION_DISTANCE) ? best : null;
    }

    private static String suggestClosestTag(ResourceLocation unknown) {
        String target = unknown.toString();
        String best = null;
        int bestDist = Integer.MAX_VALUE;

        var iter = BuiltInRegistries.ENTITY_TYPE.getTags().iterator();
        while (iter.hasNext()) {
            var pair = iter.next();
            String candidate = pair.getFirst().location().toString();
            int dist = Levenshtein.distance(target, candidate);
            if (dist < bestDist) {
                bestDist = dist;
                best = candidate;
            }
        }

        return (best != null && bestDist <= MAX_SUGGESTION_DISTANCE) ? best : null;
    }

    private static String suggestClosestDimension(ResourceLocation unknown, MinecraftServer server) {
        String target = unknown.toString();
        String best = null;
        int bestDist = Integer.MAX_VALUE;

        for (ResourceKey<Level> key : server.levelKeys()) {
            String candidate = key.location().toString();
            int dist = Levenshtein.distance(target, candidate);
            if (dist < bestDist) {
                bestDist = dist;
                best = candidate;
            }
        }

        return (best != null && bestDist <= MAX_SUGGESTION_DISTANCE) ? best : null;
    }

    private static String suggestUniqueName(String base) {
        int suffix = 2;
        String candidate = base + "_" + suffix;
        while (RuleManager.getRuleNames().contains(candidate)
                || RuleManager.getDisabledRules().containsKey(candidate)) {
            suffix++;
            candidate = base + "_" + suffix;
        }
        return candidate;
    }
}