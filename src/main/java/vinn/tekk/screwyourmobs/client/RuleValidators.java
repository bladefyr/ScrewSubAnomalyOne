package vinn.tekk.screwyourmobs.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import vinn.tekk.screwyourmobs.helpers.Levenshtein;
import vinn.tekk.screwyourmobs.rules.RuleManager;

import java.util.ArrayList;
import java.util.List;

public final class RuleValidators {

    private RuleValidators() {}

    private static final int MAX_SUGGESTION_DISTANCE = 5;

    // ---- Option lists ----

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

    public static List<String> dimensionOptions() {
        List<String> options = new ArrayList<>(RuleManager.getKnownDimensions());
        options.sort(String::compareToIgnoreCase);
        return options;
    }

    // ---- Validators ----

    public static InputFlow.ValidationResult validateEntity(String input) {
        if (input == null || input.isEmpty()) {
            return InputFlow.ValidationResult.bad(null, "Entity can't be empty");
        }

        boolean isTag = input.startsWith("#");
        String bare = isTag ? input.substring(1) : input;
        ResourceLocation rl = ResourceLocation.tryParse(bare);

        if (rl == null) {
            return InputFlow.ValidationResult.bad(null,
                    "Not a valid ID (expected namespace:id)");
        }

        if (isTag) {
            boolean exists = BuiltInRegistries.ENTITY_TYPE.getTags()
                    .anyMatch(pair -> pair.getFirst().location().equals(rl));
            if (exists) return InputFlow.ValidationResult.ok();

            String suggestion = suggestClosestTag(rl);
            return InputFlow.ValidationResult.bad(
                    suggestion != null ? "#" + suggestion : null,
                    "Tag #" + rl + " doesn't exist");
        }

        if (BuiltInRegistries.ENTITY_TYPE.containsKey(rl)) {
            return InputFlow.ValidationResult.ok();
        }

        String suggestion = suggestClosestEntity(rl);
        return InputFlow.ValidationResult.bad(
                suggestion,
                "Entity '" + rl + "' doesn't exist");
    }

    public static InputFlow.ValidationResult validateDimension(String input) {
        if (input == null || input.isEmpty()) {
            return InputFlow.ValidationResult.bad(null, "Dimension can't be empty");
        }

        ResourceLocation rl = ResourceLocation.tryParse(input);
        if (rl == null) {
            return InputFlow.ValidationResult.bad(null,
                    "Not a valid ID (expected namespace:dimension)");
        }

        String inputStr = rl.toString();
        List<String> known = RuleManager.getKnownDimensions();

        if (known.contains(inputStr)) {
            return InputFlow.ValidationResult.ok();
        }

        String best = null;
        int bestDist = Integer.MAX_VALUE;
        for (String candidate : known) {
            int dist = Levenshtein.distance(inputStr, candidate);
            if (dist < bestDist) {
                bestDist = dist;
                best = candidate;
            }
        }

        String suggestion = (best != null && bestDist <= MAX_SUGGESTION_DISTANCE) ? best : null;
        return InputFlow.ValidationResult.bad(
                suggestion,
                "Dimension '" + inputStr + "' doesn't exist");
    }

    public static InputFlow.ValidationResult validateRuleName(String input) {
        if (input == null || input.trim().isEmpty()) {
            return InputFlow.ValidationResult.bad(null, "Name can't be empty");
        }

        String working = input.trim();
        List<String> reasons = new ArrayList<>();

        if (!working.equals(input)) {
            reasons.add("Name had leading or trailing whitespace");
        }

        // Strip leading underscores
        if (working.startsWith("_")) {
            working = working.replaceAll("^_+", "");
            reasons.add("Names can't start with '_'");
        }

        // Strip filesystem-unsafe characters
        if (working.matches(".*[<>:\"/\\\\|?*\\x00-\\x1f].*")) {
            working = working.replaceAll("[<>:\"/\\\\|?*\\x00-\\x1f]", "");
            reasons.add("Names can't contain < > : \" / \\ | ? *");
        }

        // Strip trailing periods
        if (working.endsWith(".")) {
            working = working.replaceAll("\\.+$", "");
            reasons.add("Names can't end with a period");
        }

        // Truncate to 100 chars
        if (working.length() > 100) {
            working = working.substring(0, 100);
            reasons.add("Names can be at most 100 characters");
        }

        // Collision check runs on the cleaned name
        if (!working.isEmpty()
                && (RuleManager.getRuleNames().contains(working)
                || RuleManager.getDisabledRules().containsKey(working))) {
            working = suggestUniqueName(working);
            reasons.add("A rule named '" + input.trim() + "' already exists");
        }

        if (reasons.isEmpty()) {
            return InputFlow.ValidationResult.ok();
        }

        if (working.isEmpty()) {
            return InputFlow.ValidationResult.bad(null,
                    String.join(". ", reasons) + " — nothing left after cleanup");
        }

        return InputFlow.ValidationResult.bad(working, String.join(". ", reasons));
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