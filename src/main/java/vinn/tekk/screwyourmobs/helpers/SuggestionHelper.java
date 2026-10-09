package vinn.tekk.screwyourmobs.helpers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SuggestionHelper {

    private SuggestionHelper() {}

    private static final int MAX_SUGGESTIONS = 100;
    private static final int FUZZY_MAX_DISTANCE = 5;

    public static List<String> filter(List<String> options, String input) {
        if (input == null || input.isEmpty()) {
            return options.size() <= MAX_SUGGESTIONS
                    ? options
                    : options.subList(0, MAX_SUGGESTIONS);
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
                .limit(MAX_SUGGESTIONS)
                .map(Scored::text)
                .toList();
    }

    private record Scored(String text, int score) {}
}