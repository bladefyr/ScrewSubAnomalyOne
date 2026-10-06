package vinn.tekk.screwyourmobs.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EntityRemovalConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<String> ENTITIES_TO_KEEP;
    public static final ModConfigSpec.BooleanValue ENABLE_LOGGING;
    public static final ModConfigSpec.ConfigValue<String> LOG_MESSAGE;

    static {
        BUILDER.push("entityRemoval");

        ENTITIES_TO_KEEP = BUILDER
                .comment("Entities that should NEVER be removed, even if they match an ID or tag above.",
                        "Comma-separated list of entity IDs (format: namespace:entity_id).",
                        "Useful for exempting named bosses or specific variants, but really probably useless otherwise.",
                        "Example: minecraft:wither, minecraft:ender_dragon")
                .translation("entityremoval.config.entitiesToKeep")
                .define("entitiesToKeep", "");

        ENABLE_LOGGING = BUILDER
                .comment("Log each removal. Useful for debugging, but spams logs with common entities.")
                .translation("entityremoval.config.enableLogging")
                .define("enableLogging", false);

        LOG_MESSAGE = BUILDER
                .comment("Message format used when logging removals. %s is replaced with the entity ID.")
                .translation("entityremoval.config.logMessage")
                .define("logMessage", "[ScrewYourMobs!] Removed: %s");

        BUILDER.pop();
    }

    public static final ModConfigSpec SPECIFICATION = BUILDER.build();

    // ----- Helpers -----

    public static List<String> getEntitiesToKeep() {
        return parseCsv(ENTITIES_TO_KEEP.get());
    }

    private static List<String> parseCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}