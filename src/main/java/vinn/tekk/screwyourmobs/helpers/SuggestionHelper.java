package vinn.tekk.screwyourmobs.helpers;

import vinn.tekk.screwyourmobs.config.EntityRemovalConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SuggestionHelper {

    private SuggestionHelper() {}

    private static final int FUZZY_MAX_DISTANCE = 5;

    /** Explicit-cap version, used by the benchmark and any code that wants to override the config. */
    public static List<String> filter(List<String> options, String input, int maxOverride) {
        if (input == null || input.isEmpty()) {
            return options.size() <= maxOverride
                    ? options
                    : options.subList(0, maxOverride);
        }

        String lower = input.toLowerCase();
        List<Scored> matches = new ArrayList<>();

        for (String candidate : options) {
            String cl = candidate.toLowerCase();

            int score;
            if (cl.equals(lower)) {
                score = 0;
            } else if (cl.startsWith(lower)) {
                score = 1;
            } else if (cl.contains(lower)) {
                score = 2;
            } else {
                int dist = Levenshtein.distance(lower, cl);
                if (dist > FUZZY_MAX_DISTANCE) continue;
                score = 10 + dist;
            }
            matches.add(new Scored(candidate, score));
        }

        matches.sort(Comparator
                .comparingInt(Scored::score)
                .thenComparing(s -> s.text().toLowerCase()));

        return matches.stream()
                .limit(maxOverride)
                .map(Scored::text)
                .toList();
    }

    /** Reads the cap from config. */
    public static List<String> filter(List<String> options, String input) {
        return filter(options, input, EntityRemovalConfig.getMaxSuggestions());
    }

    private record Scored(String text, int score) {}
}