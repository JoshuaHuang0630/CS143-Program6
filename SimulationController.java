import javax.swing.*;
import java.awt.event.ActionListener;

/**
 * Drives the Josephus simulation step-by-step using a {@link javax.swing.Timer}.
 *
 * <h3>Animation model</h3>
 * Each elimination is broken into two phases:
 * <ol>
 *   <li><b>Count phase</b> — the arrow advances one position per tick until
 *       it has moved {@code k} steps.  Each tick increments
 *       {@code state.stepCount} and updates {@code state.arrowIndex}.</li>
 *   <li><b>Eliminate phase</b> — the target node fades out over several ticks
 *       by decrementing {@code state.eliminatingAlpha}, then the node is
 *       spliced from the alive list.</li>
 * </ol>
 * After each tick the controller calls {@code panel.repaint()} so the
 * {@link CirclePanel} picks up the new state.
 *
 * <p>Speed is controlled via {@link #setSpeed(int)} (1 = slow, 5 = fast).</p>
 */
public class SimulationController {

    // Speed → timer delay in ms for the count phase
    private static final int[] DELAYS   = {700, 420, 240, 130, 55};
    // Speed → number of fade ticks for the eliminate phase
    private static final int[] FADE_TICKS = {10, 8, 6, 4, 2};

    // ── Wiring ────────────────────────────────────────────────────────────────
    private final SimulationState state;
    private final CirclePanel     panel;
    private final Runnable        onElimination;   // called after each removal
    private final Runnable        onFinished;       // called when only 1 remains

    // ── Timer ─────────────────────────────────────────────────────────────────
    private Timer timer;
    private int   speed = 3;           // 1–5

    // ── Phase tracking ────────────────────────────────────────────────────────
    private int stepsLeft   = 0;       // count-phase steps remaining this round
    private int fadeTicksLeft = 0;     // fade-phase ticks remaining this round
    private boolean inFade  = false;   // true while in the fade phase

    // ── Constructor ──────────────────────────────────────────────────────────
    /**
     * @param state         shared simulation state
     * @param panel         the circle panel (repainted each tick)
     * @param onElimination called (on EDT) right after a name is removed
     * @param onFinished    called (on EDT) when the simulation is complete
     */
    public SimulationController(SimulationState state,
                                CirclePanel panel,
                                Runnable onElimination,
                                Runnable onFinished) {
        this.state          = state;
        this.panel          = panel;
        this.onElimination  = onElimination;
        this.onFinished     = onFinished;

        startNewRound();
        timer = new Timer(getDelay(), e -> tick());
        timer.setInitialDelay(0);
    }

    // ── Public API ────────────────────────────────────────────────────────────
    /** Start or resume auto-play. */
    public void play()  { if (!state.finished) timer.start(); }

    /** Pause auto-play. */
    public void pause() { timer.stop(); }

    /** Returns true when the timer is running (auto-play mode). */
    public boolean isRunning() { return timer.isRunning(); }

    /**
     * Advance exactly one tick manually (used by the Step button).
     * Stops the timer first so play and step don't conflict.
     */
    public void step() {
        timer.stop();
        tick();
    }

    /**
     * Set playback speed (1 = slowest, 5 = fastest).
     * Takes effect on the next tick.
     */
    public void setSpeed(int speed) {
        this.speed = Math.max(1, Math.min(5, speed));
        timer.setDelay(getDelay());
    }

    /** Stops the timer. Call when the window is closing. */
    public void dispose() { timer.stop(); }

    // ── Internal tick logic ───────────────────────────────────────────────────
    private void tick() {
        if (state.finished) { timer.stop(); return; }

        if (!inFade) {
            // ── Count phase ───────────────────────────────────────────────────
            countStep();
        } else {
            // ── Fade phase ────────────────────────────────────────────────────
            fadeStep();
        }

        panel.repaint();
    }

    private void countStep() {
        // Advance arrow to next person
        state.arrowIndex = (state.arrowIndex + 1) % state.alive.size();
        state.stepCount++;
        stepsLeft--;

        if (stepsLeft == 0) {
            // Reached the k-th person — begin fade
            state.eliminatingIndex = state.arrowIndex;
            state.eliminatingAlpha = 1.0f;
            inFade        = true;
            fadeTicksLeft = FADE_TICKS[speed - 1];
            timer.setDelay(getDelay()); // recalc in case speed changed
        }
    }

    private void fadeStep() {
        fadeTicksLeft--;
        state.eliminatingAlpha = Math.max(0f, (float) fadeTicksLeft / FADE_TICKS[speed - 1]);

        if (fadeTicksLeft <= 0) {
            // Commit the elimination
            commitElimination();
        }
    }

    private void commitElimination() {
        int idx   = state.eliminatingIndex;
        String name = state.alive.remove(idx);
        state.eliminatedLog.add("#" + (state.originalNames.length - state.alive.size())
                + "  " + name);
        state.eliminatingIndex = -1;
        state.eliminatingAlpha = 1.0f;
        inFade = false;

        onElimination.run();

        if (state.alive.size() == 1) {
            // Simulation complete
            state.survivorName = state.alive.get(0);
            state.arrowIndex   = 0;
            state.stepCount    = 0;
            state.finished     = true;
            timer.stop();
            onFinished.run();
            return;
        }

        // Wrap arrow index if needed
        if (state.arrowIndex >= state.alive.size()) {
            state.arrowIndex = 0;
        }
        // Start next round — arrow is already at the first person to count from;
        // we want to count k steps starting FROM this person (inclusive), so we
        // back up one so that the first countStep() tick lands on arrowIndex.
        state.arrowIndex = Math.floorMod(state.arrowIndex - 1, state.alive.size());
        startNewRound();
        timer.setDelay(getDelay());
    }

    private void startNewRound() {
        stepsLeft      = state.k;
        state.stepCount = 0;
        inFade         = false;
    }

    private int getDelay() { return DELAYS[speed - 1]; }
}
