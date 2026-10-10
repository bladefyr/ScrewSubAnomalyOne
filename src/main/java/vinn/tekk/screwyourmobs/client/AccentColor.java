package vinn.tekk.screwyourmobs.client;

import vinn.tekk.screwyourmobs.ScrewYourMobsMod;
import vinn.tekk.screwyourmobs.config.EntityRemovalConfig;

public final class AccentColor {

    private AccentColor() {}

    private static final int DEFAULT = 0xFFAA00;
    private static volatile int cachedSolid = DEFAULT;
    private static volatile boolean cacheValid = false;

    public static void invalidateCache() {
        cacheValid = false;
    }

    public static String defaultHex() {
        return String.format("%06X", DEFAULT & 0x00FFFFFF);
    }

    public static int solid() {
        if (!cacheValid) {
            cachedSolid = parse(EntityRemovalConfig.getAccentColor());
            cacheValid = true;
        }
        return cachedSolid;
    }

    public static int dim() {
        return (solid() & 0x00FFFFFF) | 0x40000000;
    }

    public static int faint() {
        return (solid() & 0x00FFFFFF) | 0x20000000;
    }

    public static int rgb() {
        return solid() & 0x00FFFFFF;
    }

    private static int parse(String hex) {
        if (hex == null || hex.isBlank()) return DEFAULT;

        String cleaned = hex.trim();
        if (cleaned.startsWith("#")) cleaned = cleaned.substring(1);

        if (!cleaned.matches("[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?")) {
            ScrewYourMobsMod.LOGGER.warn(
                    "[ScrewYourMobs!] Invalid accent color '{}' in config, using default.",
                    hex);
            return DEFAULT;
        }

        try {
            long value = Long.parseLong(cleaned, 16);
            if (cleaned.length() == 6) {
                return (int) (0xFF000000L | value);
            } else {
                return (int) value;
            }
        } catch (NumberFormatException ignored) {
            return DEFAULT;
        }
    }
}