package vinn.tekk.screwyourmobs.debug;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLPaths;
import vinn.tekk.screwyourmobs.ScrewYourMobsMod;
import vinn.tekk.screwyourmobs.config.EntityRemovalConfig;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DebugLog {

    private DebugLog() {}

    public enum Channel {
        VALIDATE,
        REMOVAL,
        RULE_ERROR,
        RELOAD
    }

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Path DEBUG_FILE =
            FMLPaths.CONFIGDIR.get().resolve("sym").resolve("debug.log");

    public static void log(Channel channel, String message, Object... args) {
        if (!EntityRemovalConfig.DEBUG_ENABLED.get()) return;
        if (!isChannelEnabled(channel)) return;

        String formatted = format(message, args);

        if (EntityRemovalConfig.DEBUG_TO_CONSOLE.get()) {
            ScrewYourMobsMod.LOGGER.info(
                    "[DEBUG/{}] {}", channel.name().toLowerCase(), formatted);
        }

        if (EntityRemovalConfig.DEBUG_TO_FILE.get()) {
            appendToFile(channel, formatted);
        }
    }

    public static void chatOnly(CommandSourceStack source, Channel channel,
                                String message, Object... args) {
        if (!EntityRemovalConfig.DEBUG_ENABLED.get()) return;
        if (!isChannelEnabled(channel)) return;
        if (!EntityRemovalConfig.DEBUG_TO_CHAT.get()) return;

        String formatted = format(message, args);
        source.sendSuccess(
                () -> Component.literal("§7[§bDEBUG/" + channel.name().toLowerCase()
                        + "§7] §f" + formatted),
                false);
    }

    public static void logToSource(CommandSourceStack source, Channel channel,
                                   String message, Object... args) {
        if (!EntityRemovalConfig.DEBUG_ENABLED.get()) return;
        if (!isChannelEnabled(channel)) return;

        String formatted = format(message, args);

        if (EntityRemovalConfig.DEBUG_TO_CHAT.get()) {
            source.sendSuccess(
                    () -> Component.literal("§7[§bDEBUG/" + channel.name().toLowerCase()
                            + "§7] §f" + formatted),
                    false);
        }

        if (EntityRemovalConfig.DEBUG_TO_CONSOLE.get()) {
            ScrewYourMobsMod.LOGGER.info(
                    "[DEBUG/{}] {}", channel.name().toLowerCase(), formatted);
        }

        if (EntityRemovalConfig.DEBUG_TO_FILE.get()) {
            appendToFile(channel, formatted);
        }
    }

    private static boolean isChannelEnabled(Channel channel) {
        return switch (channel) {
            case VALIDATE   -> EntityRemovalConfig.DEBUG_VALIDATE.get();
            case REMOVAL    -> EntityRemovalConfig.DEBUG_REMOVALS.get();
            case RULE_ERROR -> EntityRemovalConfig.DEBUG_RULE_ERRORS.get();
            case RELOAD     -> EntityRemovalConfig.DEBUG_RELOADS.get();
        };
    }

    private static String format(String message, Object... args) {
        if (args == null || args.length == 0) return message;
        try {
            return String.format(message, args);
        } catch (Exception e) {
            return message + " [format error: " + e.getMessage() + "]";
        }
    }

    private static synchronized void appendToFile(Channel channel, String formatted) {
        try {
            Path parent = DEBUG_FILE.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            String line = "[" + LocalDateTime.now().format(TIMESTAMP) + "] ["
                    + channel.name().toLowerCase() + "] " + formatted
                    + System.lineSeparator();

            try (Writer w = Files.newBufferedWriter(
                    DEBUG_FILE,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND)) {
                w.write(line);
            }
        } catch (IOException e) {
            ScrewYourMobsMod.LOGGER.error(
                    "[ScrewYourMobs!] Could not write to debug file '{}': {}",
                    DEBUG_FILE, e.getMessage());
        }
    }

    public static Path getDebugFilePath() {
        return DEBUG_FILE;
    }
}