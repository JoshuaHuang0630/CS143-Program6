import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/**
 * Entry point for the Josephus Problem Simulation.
 *
 * Workflow:
 *   1. Load participant names from a text file.
 *   2. Prompt user for the elimination count k.
 *   3. Run the simulation round-by-round, printing each elimination
 *      and the list of survivors after each round.
 *   4. Continue until one survivor remains.
 *   5. Verify the simulation result against the mathematical solution.
 */
public class JosephusSimulation {

    // ── ANSI colour helpers ──────────────────────────────────────────────────
    private static final String RESET  = "\u001B[0m";
    private static final String BOLD   = "\u001B[1m";
    private static final String RED    = "\u001B[31m";
    private static final String GREEN  = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN   = "\u001B[36m";

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        // ── 1. Determine the names file ──────────────────────────────────────
        String filePath;
        if (args.length > 0) {
            filePath = args[0];
        } else {
            System.out.print(BOLD + "Enter path to names file [default: names.txt]: " + RESET);
            String input = scanner.nextLine().trim();
            filePath = input.isEmpty() ? "names.txt" : input;
        }

        // ── 2. Load names ────────────────────────────────────────────────────
        List<String> nameList;
        try {
            nameList = NamesFileReader.readNames(filePath);
        } catch (IOException e) {
            System.err.println(RED + "Error reading file \"" + filePath + "\": " + e.getMessage() + RESET);
            scanner.close();
            return;
        }

        if (nameList.size() < 2) {
            System.err.println(RED + "At least 2 names are required to run the simulation." + RESET);
            scanner.close();
            return;
        }

        int n = nameList.size();
        String[] namesArray = nameList.toArray(new String[0]);

        // ── 3. Print participants ────────────────────────────────────────────
        System.out.println();
        System.out.println(BOLD + CYAN + "╔══════════════════════════════════════════╗");
        System.out.println(       "║      JOSEPHUS PROBLEM  SIMULATION        ║");
        System.out.println(       "╚══════════════════════════════════════════╝" + RESET);
        System.out.println();
        System.out.println(BOLD + "Participants (" + n + "):" + RESET);
        for (int i = 0; i < n; i++) {
            System.out.printf("  %2d. %s%n", i + 1, namesArray[i]);
        }

        // ── 4. Get elimination count k ───────────────────────────────────────
        int k = 0;
        while (k < 1) {
            System.out.print(BOLD + "\nEnter elimination count k (must be >= 1): " + RESET);
            try {
                k = Integer.parseInt(scanner.nextLine().trim());
                if (k < 1) {
                    System.out.println(RED + "  k must be at least 1. Try again." + RESET);
                }
            } catch (NumberFormatException e) {
                System.out.println(RED + "  Invalid number. Try again." + RESET);
            }
        }

        // ── 5. Build the circle ──────────────────────────────────────────────
        JosephusCircle circle = new JosephusCircle();
        for (String name : namesArray) {
            circle.add(name);
        }

        // ── 6. Run the simulation ────────────────────────────────────────────
        System.out.println();
        System.out.println(BOLD + "─────────────────────────────────────────────");
        System.out.println("  SIMULATION  (k = " + k + ")");
        System.out.println("─────────────────────────────────────────────" + RESET);

        int round = 1;
        while (!circle.hasOneRemaining()) {
            String eliminated = circle.eliminate(k);
            int survivorsLeft = circle.getSize();

            System.out.println();
            System.out.printf(RED + BOLD + "  Round %d — Eliminated: %s" + RESET + "%n",
                              round, eliminated);
            System.out.printf("  Survivors (%d): %s%n",
                              survivorsLeft, circle.listRemaining());
            round++;
        }

        // ── 7. Announce simulation winner ────────────────────────────────────
        String simulationSurvivor = circle.getSurvivorName();
        System.out.println();
        System.out.println(BOLD + "─────────────────────────────────────────────" + RESET);
        System.out.println(BOLD + GREEN + "  SIMULATION SURVIVOR: " + simulationSurvivor + RESET);
        System.out.println(BOLD + "─────────────────────────────────────────────" + RESET);

        // ── 8. Mathematical verification ─────────────────────────────────────
        String mathSurvivor = JosephusMath.survivorName(namesArray, k);
        int    mathIndex    = JosephusMath.survivorIndex(n, k);

        System.out.println();
        System.out.println(BOLD + CYAN + "  MATHEMATICAL VERIFICATION" + RESET);
        System.out.printf("  Formula survivor (0-based index %d): %s%n",
                          mathIndex, mathSurvivor);

        if (simulationSurvivor.equals(mathSurvivor)) {
            System.out.println(GREEN + BOLD
                + "  ✔  Results match! The simulation is correct." + RESET);
        } else {
            System.out.println(RED + BOLD
                + "  ✘  Mismatch! Simulation: " + simulationSurvivor
                + "  |  Math: " + mathSurvivor + RESET);
        }

        System.out.println();
        scanner.close();
    }
}
