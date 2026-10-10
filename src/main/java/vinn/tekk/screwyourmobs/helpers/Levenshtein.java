package vinn.tekk.screwyourmobs.helpers;

import java.util.Collection;

/**
 * Classic Levenshtein distance. O(n*m) time, O(min(n,m)) space.
 * Used to generate "did you mean ...?" suggestions.
 * This is probably my favourite thing I've ever done yet in terms of coding
 */
public final class Levenshtein {

    private Levenshtein() {}

    public static int distance(String a, String b) {
        if (a == null) return b == null ? 0 : b.length();
        if (b == null) return a.length();
        if (a.equals(b)) return 0;

        int n = a.length();
        int m = b.length();

        if (n == 0) return m;
        if (m == 0) return n;

        if (n > m) {
            String tmp = a; a = b; b = tmp;
            int t = n; n = m; m = t;
        }

        int[] prev = new int[n + 1];
        int[] curr = new int[n + 1];

        for (int i = 0; i <= n; i++) prev[i] = i;

        for (int j = 1; j <= m; j++) {
            curr[0] = j;
            char cb = b.charAt(j - 1);
            for (int i = 1; i <= n; i++) {
                int cost = (a.charAt(i - 1) == cb) ? 0 : 1;
                curr[i] = Math.min(
                        Math.min(curr[i - 1] + 1, prev[i] + 1),
                        prev[i - 1] + cost);
            }
            int[] swap = prev; prev = curr; curr = swap;
        }

        return prev[n];
    }

    /**
     * Picks the closest candidate to {@code target} from {@code candidates}.
     *
     * Ranking:
     *   1. Smallest Levenshtein distance wins.
     *   2. On ties, prefer the candidate with the longest common prefix with target.
     *   3. On further ties, prefer the alphabetically-earlier candidate.
     *
     * @param maxDistance reject candidates farther than this. Returns null
     *                    if nothing is close enough.
     */
    public static String closest(String target, Collection<String> candidates, int maxDistance) {
        if (target == null || candidates == null || candidates.isEmpty()) return null;

        String best = null;
        int bestDist = Integer.MAX_VALUE;
        int bestPrefix = -1;

        for (String candidate : candidates) {
            if (candidate == null) continue;

            int dist = distance(target, candidate);
            if (dist > maxDistance) continue;

            int prefix = commonPrefixLength(target, candidate);

            boolean better = false;
            if (dist < bestDist) {
                better = true;
            } else if (dist == bestDist) {
                if (prefix > bestPrefix) {
                    better = true;
                } else if (prefix == bestPrefix && (best == null || candidate.compareTo(best) < 0)) {
                    better = true;
                }
            }

            if (better) {
                best = candidate;
                bestDist = dist;
                bestPrefix = prefix;
            }
        }

        return best;
    }

    private static int commonPrefixLength(String a, String b) {
        int n = Math.min(a.length(), b.length());
        for (int i = 0; i < n; i++) {
            if (a.charAt(i) != b.charAt(i)) return i;
        }
        return n;
    }
}