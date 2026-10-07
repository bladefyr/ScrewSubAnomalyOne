package vinn.tekk.screwyourmobs.rules;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforgespi.language.IModInfo;
import vinn.tekk.screwyourmobs.debug.Levenshtein;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class RuleValidator {

    private RuleValidator() {}

    // Max edit distance to still suggest a candidate
    private static final int SUGGESTION_MAX_DISTANCE = 4;

    public static List<String> validate(Map<String, RemovalRule> rules,
                                        Set<ResourceLocation> knownDimensions) {
        List<String> warnings = new ArrayList<>();

        Set<ResourceLocation> knownEntities = BuiltInRegistries.ENTITY_TYPE.keySet();
        Set<String> loadedNamespaces = FMLLoader.getLoadingModList()
                .getMods().stream()
                .map(IModInfo::getModId)
                .collect(Collectors.toSet());

        // Cache tag keys + string forms once, outside the rule loop
        Set<TagKey<EntityType<?>>> knownTagKeys = new HashSet<>();
        Set<String> knownTagNames = new HashSet<>();
        BuiltInRegistries.ENTITY_TYPE.getTags().forEach(pair -> {
            TagKey<EntityType<?>> key = pair.getFirst();
            knownTagKeys.add(key);
            knownTagNames.add(key.location().toString());
        });

        for (RemovalRule rule : rules.values()) {
            // Dimensions
            for (ResourceLocation dim : rule.dimensions()) {
                if (!knownDimensions.contains(dim)) {
                    warnings.add(formatUnknownDimension(rule.name(), dim, knownDimensions));
                }
            }

            // Entity IDs
            for (ResourceLocation entity : rule.entities()) {
                if (!knownEntities.contains(entity)) {
                    warnings.add(formatUnknownEntity(rule.name(), entity, knownEntities, loadedNamespaces));
                }
            }

            // Entity tags
            for (TagKey<EntityType<?>> tag : rule.entityTags()) {
                if (!knownTagKeys.contains(tag)) {
                    warnings.add(formatUnknownTag(rule.name(), tag, knownTagNames));
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

    private static String formatUnknownTag(String ruleName,
                                           TagKey<EntityType<?>> unknown,
                                           Set<String> knownTagNames) {
        String target = unknown.location().toString();
        String suggestion = suggestClosestString(target, knownTagNames, SUGGESTION_MAX_DISTANCE);
        String suffix = suggestion != null
                ? " Did you mean '#" + suggestion + "'?"
                : "";
        return "Rule '" + ruleName + "' references unknown entity tag '"
                + "#" + target + "'." + suffix;
    }

    private static String suggestClosest(ResourceLocation unknown,
                                         Set<ResourceLocation> candidates) {
        String target = unknown.toString();
        String best = null;
        int bestDist = Integer.MAX_VALUE;

        for (ResourceLocation candidate : candidates) {
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
}