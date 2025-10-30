package com.bladelicious.screwsubanomalyone.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EntityRemovalConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final Spec SPEC = new Spec();

    public static class Spec {
        public final ModConfigSpec.ConfigValue<String> entitiesToRemove;
        public final ModConfigSpec.BooleanValue enableLogging;
        public final ModConfigSpec.ConfigValue<String> logMessage;

        public Spec() {
            BUILDER.push("entityRemovalSettings");

            entitiesToRemove = BUILDER
                    .comment("Entities to remove when spawned (comma-separated, format: modid:entity_id)",
                            "Example: thebrokenscript:sub_anomaly_1, minecraft:creeper, minecraft:zombie")
                    .translation("screwsubanomalyone.config.entitiesToRemove")
                    .define("entitiesToRemove", "thebrokenscript:sub_anomaly_1");

            enableLogging = BUILDER
                    .comment("Enable logging when entities are removed (spams the logs if you add common entities)")
                    .translation("screwsubanomalyone.config.enableLogging")
                    .define("enableLogging", false);

            logMessage = BUILDER
                    .comment("Log message when entities are removed (use %s for entity type)")
                    .translation("screwsubanomalyone.config.logMessage")
                    .define("logMessage", "§6Removed entity: %s");

            BUILDER.pop();
        }
    }

    public static final ModConfigSpec SPECIFICATION = BUILDER.build();

    public static List<String> getEntityList() {
        String value = SPEC.entitiesToRemove.get();
        if (value == null || value.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}