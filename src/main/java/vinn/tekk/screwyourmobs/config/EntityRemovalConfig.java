package vinn.tekk.screwyourmobs.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EntityRemovalConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // --- General ---
    public static final ModConfigSpec.ConfigValue<String> ENTITIES_TO_KEEP;
    public static final ModConfigSpec.ConfigValue<String> DISPLAY_ACCENT_COLOR;

    // --- Rules ---
    public static final ModConfigSpec.BooleanValue RULES_VALIDATE_ON_LOAD;
    public static final ModConfigSpec.IntValue MAX_SUGGESTIONS;

    // --- Debug ---
    public static final ModConfigSpec.BooleanValue DEBUG_ENABLED;
    public static final ModConfigSpec.BooleanValue DEBUG_VALIDATE;
    public static final ModConfigSpec.BooleanValue DEBUG_REMOVALS;
    public static final ModConfigSpec.BooleanValue DEBUG_RULE_ERRORS;
    public static final ModConfigSpec.BooleanValue DEBUG_RELOADS;
    public static final ModConfigSpec.BooleanValue DEBUG_TO_CONSOLE;
    public static final ModConfigSpec.BooleanValue DEBUG_TO_CHAT;
    public static final ModConfigSpec.BooleanValue DEBUG_TO_FILE;

    // --- DEV ---
    public static final ModConfigSpec.BooleanValue DEBUG_DEV_MODE;

    static {
        BUILDER.comment("General settings.")
                .push("general");

        ENTITIES_TO_KEEP = BUILDER
                .comment("Entities that will NEVER be removed, even if a rule matches them.",
                        "Comma-separated list of entity IDs (format: namespace:entity_id).",
                        "Useful for exempting named bosses or specific variants.",
                        "Example: minecraft:wither, minecraft:ender_dragon")
                .translation("screwyourmobs.config.general.entitiesToKeep")
                .define("entitiesToKeep", "");

        DISPLAY_ACCENT_COLOR = BUILDER
                .comment("Accent color used throughout the rule editor UI.",
                        "Hex format: RRGGBB or AARRGGBB.",
                        "'#FFAA00' is the default color, amber.")
                .translation("screwyourmobs.config.general.accentColor")
                .define("accentColor", "FFAA00");

        BUILDER.pop();

        BUILDER.comment("Rule loading and validation.")
                .push("rules");

        RULES_VALIDATE_ON_LOAD = BUILDER
                .comment("Check rule entity/dimension IDs against the registry on load.",
                        "Warnings are shown through the debug channels below.")
                .translation("screwyourmobs.config.rules.validateOnLoad")
                .define("validateOnLoad", true);

        MAX_SUGGESTIONS = BUILDER
                .comment("Maximum number of entries shown in the editor's autocomplete lists.",
                        "Higher values mean longer lists but slightly more work per keystroke.",
                        "Performance impact SHOULD be negligible.")
                .translation("screwyourmobs.config.rules.maxSuggestions")
                .defineInRange("maxSuggestions", 250, 10, 10000);

        BUILDER.pop();

        BUILDER.comment("Debug output controls.",
                        "The master switch must be true for ANY of the channels below to fire.")
                .push("debug");

        DEBUG_ENABLED = BUILDER
                .comment("Master switch. When false, all debug output is silenced.")
                .translation("screwyourmobs.config.debug.enabled")
                .define("enabled", false);

        DEBUG_VALIDATE = BUILDER
                .comment("Warn when a rule references an unknown entity or dimension.")
                .translation("screwyourmobs.config.debug.validateRules")
                .define("validateRules", true);

        DEBUG_REMOVALS = BUILDER
                .comment("Log every entity removal. Noisy on busy servers.")
                .translation("screwyourmobs.config.debug.logRemovals")
                .define("logRemovals", false);

        DEBUG_RULE_ERRORS = BUILDER
                .comment("Log rule files that fail to parse or are skipped.")
                .translation("screwyourmobs.config.debug.logRuleErrors")
                .define("logRuleErrors", true);

        DEBUG_RELOADS = BUILDER
                .comment("Log each rule reload with counts.")
                .translation("screwyourmobs.config.debug.logReloads")
                .define("logReloads", true);

        DEBUG_TO_CONSOLE = BUILDER
                .comment("Send debug output to the server/client log.")
                .translation("screwyourmobs.config.debug.outputConsole")
                .define("outputConsole", true);

        DEBUG_TO_CHAT = BUILDER
                .comment("Send debug output to chat when triggered by a command.")
                .translation("screwyourmobs.config.debug.outputChat")
                .define("outputChat", false);

        DEBUG_TO_FILE = BUILDER
                .comment("Append debug output to config/sym/debug.log with timestamps.")
                .translation("screwyourmobs.config.debug.outputFile")
                .define("outputFile", false);

        DEBUG_DEV_MODE = BUILDER
                .comment("Enable experimental or developer-only features.",
                        "Not intended for regular use. Some features may be unstable.")
                .translation("screwyourmobs.config.debug.devMode")
                .define("devMode", false);

        BUILDER.pop();
    }

    public static final ModConfigSpec SPECIFICATION = BUILDER.build();

    public static List<String> getEntitiesToKeep() {
        return parseCsv(ENTITIES_TO_KEEP.get());
    }

    public static int getMaxSuggestions() {
        return MAX_SUGGESTIONS.get();
    }

    public static String getAccentColor() {
        return DISPLAY_ACCENT_COLOR.get();
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