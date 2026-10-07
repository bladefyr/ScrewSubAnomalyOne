package vinn.tekk.screwyourmobs.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EntityRemovalConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<String> ENTITIES_TO_KEEP;

    // Debug section
    public static final ModConfigSpec.BooleanValue DEBUG_ENABLED;
    public static final ModConfigSpec.BooleanValue DEBUG_VALIDATE;
    public static final ModConfigSpec.BooleanValue DEBUG_REMOVALS;
    public static final ModConfigSpec.BooleanValue DEBUG_RULE_ERRORS;
    public static final ModConfigSpec.BooleanValue DEBUG_RELOADS;
    public static final ModConfigSpec.BooleanValue DEBUG_TO_CONSOLE;
    public static final ModConfigSpec.BooleanValue DEBUG_TO_CHAT;
    public static final ModConfigSpec.BooleanValue DEBUG_TO_FILE;

    static {
        BUILDER.push("entityRemoval");

        ENTITIES_TO_KEEP = BUILDER
                .comment("Entities that should NEVER be removed, even if they match an ID or tag above.",
                        "Comma-separated list of entity IDs (format: namespace:entity_id).",
                        "Useful for exempting named bosses or specific variants, but really probably useless otherwise.",
                        "Example: minecraft:wither, minecraft:ender_dragon")
                .translation("entityremoval.config.entitiesToKeep")
                .define("entitiesToKeep", "");

        BUILDER.pop();

        BUILDER.comment("Debug output controls.",
                        "The master switch must be true for ANY of the channels below to fire.")
                .push("debug");

        DEBUG_ENABLED = BUILDER
                .comment("Master switch. When false, all debug output is silenced.")
                .define("enabled", false);

        DEBUG_VALIDATE = BUILDER
                .comment("Warn when a rule references an unknown entity or dimension.")
                .define("validateRules", true);

        DEBUG_REMOVALS = BUILDER
                .comment("Log every entity removal. Noisy on busy servers.")
                .define("logRemovals", false);

        DEBUG_RULE_ERRORS = BUILDER
                .comment("Log rule files that fail to parse or are skipped.")
                .define("logRuleErrors", true);

        DEBUG_RELOADS = BUILDER
                .comment("Log each rule reload with counts.")
                .define("logReloads", true);

        DEBUG_TO_CONSOLE = BUILDER
                .comment("Send debug output to the server/client log.")
                .define("outputConsole", true);

        DEBUG_TO_CHAT = BUILDER
                .comment("Send debug output to chat when triggered by a command.")
                .define("outputChat", false);

        DEBUG_TO_FILE = BUILDER
                .comment("Append debug output to config/sym/debug.log with timestamps.")
                .define("outputFile", false);

        BUILDER.pop();
    }

    public static final ModConfigSpec SPECIFICATION = BUILDER.build();

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