package vinn.tekk.screwyourmobs.rules;

import net.neoforged.neoforgespi.language.IModInfo;
import vinn.tekk.screwyourmobs.debug.DebugLog;
import vinn.tekk.screwyourmobs.debug.Levenshtein;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates a loaded rule set against live registries. Produces human-readable
 * warnings for unknown entities and dimensions, plus "did you mean ...?"
 * suggestions when a close match exists.
 */
public final class RuleValidator {

    private RuleValidator() {}

    /** Max edit distance to still suggest a candidate. */
    private static final int SUGGESTION_MAX_DISTANCE = 4;

    public static List<String> validate(Map<String, RemovalRule> rules,
                                        Set<ResourceLocation> knownDimensions) {
        List<String> warnings = new ArrayList<>();

        Set<ResourceLocation> knownEntities = BuiltInRegistries.ENTITY_TYPE.keySet();
        Set<String> loadedNamespaces = FMLLoader.getLoadingModList()
                .getMods().stream()
                .map(IModInfo::getModId)
                .collect(Collectors.toSet());

        for (RemovalRule rule : rules.values()) {
            for (ResourceLocation dim : rule.dimensions()) {
                if (!knownDimensions.contains(dim)) {
                    warnings.add(formatUnknownDimension(rule.name(), dim, knownDimensions));
                }
            }
            for (ResourceLocation entity : rule.entities()) {
                if (!knownEntities.contains(entity)) {
                    warnings.add(formatUnknownEntity(rule.name(), entity, knownEntities, loadedNamespaces));
                }
            }
        }

        return warnings;
    }

    private static String formatUnknownDimension(String ruleName,
                                                 ResourceLocation unknown,
                                                 Set<ResourceLocation> known) {
        String suggestion = suggestClosest(unknown, known);
        String suffix = suggestion != null
                ? " Did you mean '" + suggestion + "'?"
                : "";
        return "Rule '" + ruleName + "' references unknown dimension '"
                + unknown + "'." + suffix;
    }

    private static String formatUnknownEntity(String ruleName,
                                              ResourceLocation unknown,
                                              Set<ResourceLocation> knownEntities,
                                              Set<String> loadedNamespaces) {
        String namespace = unknown.getNamespace();

        if (!loadedNamespaces.contains(namespace) && !namespace.equals("minecraft")) {
            String nsSuggestion = suggestClosestString(namespace, loadedNamespaces, 2);
            if (nsSuggestion != null) {
                return "Rule '" + ruleName + "' references entity '" + unknown
                        + "' but mod namespace '" + namespace + "' is not installed."
                        + " Did you mean '" + nsSuggestion + ":" + unknown.getPath() + "'?";
            }
            return "Rule '" + ruleName + "' references entity '" + unknown
                    + "' but mod '" + namespace + "' is not installed.";
        }

        String suggestion = suggestClosest(unknown, knownEntities);
        String suffix = suggestion != null
                ? " Did you mean '" + suggestion + "'?"
                : "";
        return "Rule '" + ruleName + "' references unknown entity '"
                + unknown + "'." + suffix;
    }

    private static String suggestClosest(ResourceLocation unknown,
                                         Set<ResourceLocation> candidates) {
        String target = unknown.toString();
        String best = null;
        int bestDist = Integer.MAX_VALUE;

        for (ResourceLocation candidate : candidates) {
            // Fast reject: if namespaces differ, dist is at least 4 (namespace len + colon)
            // but we still want cross-namespace suggestions when it's close, so don't prune hard... :bladelooking:
            int dist = Levenshtein.distance(target, candidate.toString());
            if (dist < bestDist) {
                bestDist = dist;
                best = candidate.toString();
            }
        }

        return (best != null && bestDist <= SUGGESTION_MAX_DISTANCE) ? best : null;
    }

    private static String suggestClosestString(String target, Set<String> candidates, int maxDistance) {
        String best = null;
        int bestDist = Integer.MAX_VALUE;
        for (String candidate : candidates) {
            int dist = Levenshtein.distance(target, candidate);
            if (dist < bestDist) {
                bestDist = dist;
                best = candidate;
            }
        }
        return (best != null && bestDist <= maxDistance) ? best : null;
    }

    /** Convenience used by command output. */
    public static Map<String, List<String>> groupByRule(List<String> warnings) {
        Map<String, List<String>> grouped = new HashMap<>();
        for (String w : warnings) {
            int start = w.indexOf('\'');
            int end = w.indexOf('\'', start + 1);
            String ruleName = (start >= 0 && end > start) ? w.substring(start + 1, end) : "unknown";
            grouped.computeIfAbsent(ruleName, k -> new ArrayList<>()).add(w);
        }
        return grouped;
    }
}