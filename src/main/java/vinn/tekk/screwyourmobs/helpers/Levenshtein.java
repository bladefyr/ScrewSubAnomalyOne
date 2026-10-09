package vinn.tekk.screwyourmobs.helpers;

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

        // Two-row rolling array - keep the shorter side on the inner loop for cache friendliness
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
}