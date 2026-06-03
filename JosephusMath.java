/**
 * Provides a mathematical / recursive solution to the Josephus problem.
 *
 * Classic recurrence (0-indexed survivor position):
 *   J(1, k) = 0
 *   J(n, k) = (J(n-1, k) + k) % n
 *
 * The survivor's 0-based position is then mapped to the original name list.
 */
public class JosephusMath {

    /**
     * Computes the 0-based index of the survivor given n people and step k.
     * Uses the iterative form of the classic recurrence for efficiency.
     */
    public static int survivorIndex(int n, int k) {
        int pos = 0; // position of survivor with 1 person
        for (int i = 2; i <= n; i++) {
            pos = (pos + k) % i;
        }
        return pos; // 0-based index in the original ordering
    }

    /**
     * Returns the name of the mathematical survivor given the ordered name list
     * and elimination count k.
     *
     * @param names ordered array of names as they appeared in the circle
     * @param k     elimination count
     * @return the name of the predicted survivor
     */
    public static String survivorName(String[] names, int k) {
        int index = survivorIndex(names.length, k);
        return names[index];
    }
}
