package vinn.tekk.screwyourmobs.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import vinn.tekk.screwyourmobs.ScrewYourMobsMod;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

// Version should be changed in mod AND in ScrewYourMobsMod.java!!!
public final class UpdateChecker {

    private UpdateChecker() {}

    private static final String MODRINTH_URL =
            "https://api.modrinth.com/v2/project/screwyourmobs/version"
                    + "?loaders=[\"neoforge\"]&game_versions=[\"1.21.1\"]";

    private static volatile boolean checked = false;

    public static void checkOnce() {
        if (checked) return;
        checked = true;

        Thread t = new Thread(UpdateChecker::runCheck, "ScrewYourMobs-UpdateCheck");
        t.setDaemon(true);
        t.start();
    }

    private static void runCheck() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(MODRINTH_URL))
                    .header("User-Agent", "screwyourmobs/4.0 (github.com/vinntekk/ScrewYourMobs)")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                ScrewYourMobsMod.LOGGER.debug(
                        "[ScrewYourMobs!] Update check returned HTTP {}",
                        response.statusCode());
                return;
            }

            JsonArray versions = JsonParser.parseString(response.body()).getAsJsonArray();
            if (versions.isEmpty()) return;

            JsonElement latest = versions.get(0);
            String latestVersion = latest.getAsJsonObject()
                    .get("version_number").getAsString();

            String current = ScrewYourMobsMod.VERSION;

            if (isNewer(current, latestVersion)) {
                Minecraft.getInstance().execute(() ->
                        notifyPlayer(current, latestVersion));
            }
        } catch (Exception e) {
            ScrewYourMobsMod.LOGGER.debug(
                    "[ScrewYourMobs!] Update check failed: {}", e.getMessage());
        }
    }

    /**
     * Simple semver-ish comparison. Returns true if {@code candidate} is a
     * higher version than {@code current}.
     */
    private static boolean isNewer(String current, String candidate) {
        String[] a = current.split("\\.");
        String[] b = candidate.split("\\.");
        int len = Math.max(a.length, b.length);

        for (int i = 0; i < len; i++) {
            int av = i < a.length ? safeParse(a[i]) : 0;
            int bv = i < b.length ? safeParse(b[i]) : 0;
            if (bv > av) return true;
            if (bv < av) return false;
        }
        return false;
    }

    private static int safeParse(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static void notifyPlayer(String current, String latest) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        mc.player.sendSystemMessage(Component.literal(
                "§a[ScrewYourMobs!] §eUpdate available! §7Current: §f"
                        + current + " §7→ §a" + latest));

        Component link = Component.literal("§a[Click here to update]")
                .withStyle(style -> style
                        .withClickEvent(new ClickEvent(
                                ClickEvent.Action.OPEN_URL,
                                "https://modrinth.com/mod/screwyourmobs"))
                        .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Component.literal("Open Modrinth page"))));

        mc.player.sendSystemMessage(link);
    }
}