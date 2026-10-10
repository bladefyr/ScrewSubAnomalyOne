package vinn.tekk.screwyourmobs.client.bench;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLLoader;
import vinn.tekk.screwyourmobs.config.EntityRemovalConfig;
import vinn.tekk.screwyourmobs.helpers.SuggestionHelper;

import java.util.ArrayList;
import java.util.List;

public final class SuggestionBenchmark {

    private SuggestionBenchmark() {}

    public static boolean isAvailable() {
        if (!FMLLoader.isProduction()) return true;
        return EntityRemovalConfig.DEBUG_DEV_MODE.get();
    }

    public static void run() {
        if (!isAvailable()) {
            System.out.println("[SymBench] Not available. Enable debug.devMode in the config, or run in dev.");
            return;
        }

        List<String> options = buildOptions();
        System.out.println("[SymBench] Options: " + options.size());

        String[] inputs = { "", "c", "cr", "cre", "creep", "creeper", "minecraft:", "minecraft:cre" };
        int[] caps = { 10, 100, 250, 1000, 5000, 10000 };

        System.out.println("[SymBench] Warming up JIT...");
        for (int w = 0; w < 5000; w++) {
            SuggestionHelper.filter(options, "cre", 250);
        }

        for (int cap : caps) {
            System.out.println("[SymBench] ---- MAX_SUGGESTIONS = " + cap + " ----");
            for (String input : inputs) {
                runOne(options, input, cap);
            }
        }

        System.out.println("[SymBench] Done.");
    }

    private static void runOne(List<String> options, String input, int cap) {
        int iters = 500;
        long total = 0;
        long min = Long.MAX_VALUE;
        long max = 0;

        for (int i = 0; i < iters; i++) {
            long t0 = System.nanoTime();
            SuggestionHelper.filter(options, input, cap);
            long dt = System.nanoTime() - t0;

            total += dt;
            if (dt < min) min = dt;
            if (dt > max) max = dt;
        }

        double avgUs = (total / (double) iters) / 1000.0;
        System.out.printf(
                "[SymBench] input=%-18s avg=%8.2fµs  min=%8.2fµs  max=%8.2fµs%n",
                "\"" + input + "\"",
                avgUs,
                min / 1000.0,
                max / 1000.0);
    }

    private static List<String> buildOptions() {
        List<String> options = new ArrayList<>();
        for (ResourceLocation id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            options.add(id.toString());
        }
        BuiltInRegistries.ENTITY_TYPE.getTags()
                .forEach(pair -> options.add("#" + pair.getFirst().location()));
        options.sort(String::compareToIgnoreCase);
        return options;
    }
}