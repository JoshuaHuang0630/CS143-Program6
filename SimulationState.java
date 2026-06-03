import java.util.ArrayList;
import java.util.List;

/**
 * Holds the mutable state of one Josephus simulation run.
 *
 * The GUI reads this object to know what to draw; the controller
 * writes to it as the simulation progresses.
 *
 * All fields are package-private for simplicity — wrap in getters
 * if you prefer strict encapsulation.
 */
public class SimulationState {

    // ── Static setup ────────────────────────────────────────────────────────
    /** Original names in circle order (never changes after init). */
    final String[] originalNames;
    /** Elimination count k. */
    final int k;

    // ── Live circle ─────────────────────────────────────────────────────────
    /**
     * Names still alive, in current circle order.
     * Index 0 is always the person about to be counted from next.
     */
    List<String> alive = new ArrayList<>();

    /** Index in {@code alive} that the arrow currently points at. */
    int arrowIndex = 0;

    /**
     * Within the current elimination round, how many steps have been counted
     * so far (0 = haven't started counting yet for this round).
     */
    int stepCount = 0;

    // ── Elimination tracking ─────────────────────────────────────────────────
    /**
     * Names that have been eliminated, in elimination order.
     * Each entry is "#{round}  {name}".
     */
    List<String> eliminatedLog = new ArrayList<>();

    /**
     * Index in {@code alive} of the person currently being eliminated
     * (fading out). -1 when nobody is being eliminated.
     */
    int eliminatingIndex = -1;

    /** Opacity of the eliminating node (1.0 = fully visible, 0.0 = gone). */
    float eliminatingAlpha = 1.0f;

    // ── Phase flags ──────────────────────────────────────────────────────────
    /** True once a winner has been determined. */
    boolean finished = false;

    /** Name of the sole survivor (set when finished = true). */
    String survivorName = null;

    /** Name that the maths formula predicts as survivor. */
    String mathSurvivorName = null;

    // ── Constructor ──────────────────────────────────────────────────────────
    SimulationState(String[] names, int k) {
        this.originalNames = names.clone();
        this.k = k;

        for (String n : names) alive.add(n);

        // Pre-compute the mathematical answer
        int idx = JosephusMath.survivorIndex(names.length, k);
        this.mathSurvivorName = names[idx];
    }
}
