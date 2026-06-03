import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.List;

/**
 * Main Swing window for the Josephus Problem visualizer.
 *
 * <h3>Screens</h3>
 * <ul>
 *   <li><b>Setup card</b> — text area for names, k spinner, Start button.</li>
 *   <li><b>Simulation card</b> — {@link CirclePanel} on the left, eliminated
 *       list on the right, controls (Step / Play / Restart / Speed) at the
 *       bottom.</li>
 * </ul>
 *
 * <p>Swap between the two cards via the shared {@link CardLayout}.</p>
 */
public class JosephusGUI extends JFrame {

    // ── Card names ────────────────────────────────────────────────────────────
    private static final String CARD_SETUP = "setup";
    private static final String CARD_SIM   = "sim";

    // ── Layout ────────────────────────────────────────────────────────────────
    private final CardLayout  cardLayout  = new CardLayout();
    private final JPanel      rootPanel   = new JPanel(cardLayout);

    // ── Setup screen ──────────────────────────────────────────────────────────
    private final JTextArea   namesArea   = new JTextArea(10, 24);
    private final JSpinner    kSpinner    = new JSpinner(new SpinnerNumberModel(3, 1, 9999, 1));
    private final JButton     startBtn    = new JButton("Start simulation →");

    // ── Simulation screen ─────────────────────────────────────────────────────
    private final CirclePanel circlePanel = new CirclePanel();
    private final JList<String> elimList  = new JList<>(new DefaultListModel<>());
    private final JButton     stepBtn     = new JButton("Step");
    private final JButton     playBtn     = new JButton("Auto play");
    private final JButton     restartBtn  = new JButton("← Restart");
    private final JSlider     speedSlider = new JSlider(1, 5, 3);
    private final JLabel      statusLabel = new JLabel(" ");

    // ── Controller ────────────────────────────────────────────────────────────
    private SimulationController controller;

    // ── Constructor ──────────────────────────────────────────────────────────
    public JosephusGUI() {
        super("Josephus Problem Visualizer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);

        rootPanel.add(buildSetupPanel(), CARD_SETUP);
        rootPanel.add(buildSimPanel(),   CARD_SIM);

        add(rootPanel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ── Setup panel ───────────────────────────────────────────────────────────
    private JPanel buildSetupPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 16));
        p.setBorder(new EmptyBorder(28, 32, 28, 32));
        p.setBackground(Color.WHITE);

        // Title
        JLabel title = new JLabel("Josephus Problem");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        JLabel sub = new JLabel("Enter one name per line, set k, then start.");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sub.setForeground(new Color(0x606058));

        JPanel titleBox = new JPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.setOpaque(false);
        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(4));
        titleBox.add(sub);

        // Names text area
        namesArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        namesArea.setLineWrap(false);
        namesArea.setText(
            "Alice\nBob\nCharlie\nDiana\nEdward\nFiona\nGeorge\nHannah\nIvan\nJulia");
        namesArea.setBorder(new CompoundBorder(
            new LineBorder(new Color(0xC8C6C0), 1, true),
            new EmptyBorder(8, 10, 8, 10)));

        JScrollPane scroll = new JScrollPane(namesArea);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        // Load from file button
        JButton loadBtn = new JButton("Load from file…");
        loadBtn.setFont(new Font("SansSerif", Font.PLAIN, 12));
        loadBtn.addActionListener(e -> loadFile());

        // k row
        JLabel kLabel = new JLabel("Elimination count  k =");
        kLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        kSpinner.setFont(new Font("SansSerif", Font.PLAIN, 13));
        ((JSpinner.DefaultEditor) kSpinner.getEditor()).getTextField().setColumns(4);

        JPanel kRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        kRow.setOpaque(false);
        kRow.add(kLabel);
        kRow.add(kSpinner);
        kRow.add(Box.createHorizontalStrut(12));
        kRow.add(loadBtn);

        // Start button
        startBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        startBtn.setBackground(new Color(0x534AB7));
        startBtn.setForeground(Color.WHITE);
        startBtn.setFocusPainted(false);
        startBtn.setBorderPainted(false);
        startBtn.setOpaque(true);
        startBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        startBtn.addActionListener(e -> startSimulation());

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnRow.setOpaque(false);
        btnRow.add(startBtn);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(kRow,   BorderLayout.WEST);
        bottom.add(btnRow, BorderLayout.EAST);

        p.add(titleBox, BorderLayout.NORTH);
        p.add(scroll,   BorderLayout.CENTER);
        p.add(bottom,   BorderLayout.SOUTH);

        return p;
    }

    // ── Simulation panel ──────────────────────────────────────────────────────
    private JPanel buildSimPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 0));
        p.setBackground(new Color(0xF8F7F2));

        // Left: circle
        circlePanel.setPreferredSize(new Dimension(460, 460));

        // Right: eliminated list
        elimList.setFont(new Font("SansSerif", Font.PLAIN, 13));
        elimList.setBackground(new Color(0xF1F0EA));
        elimList.setBorder(new EmptyBorder(8, 8, 8, 8));
        elimList.setFixedCellHeight(28);

        JScrollPane elimScroll = new JScrollPane(elimList);
        elimScroll.setPreferredSize(new Dimension(160, 0));
        elimScroll.setBorder(new MatteBorder(0, 1, 0, 0, new Color(0xD0CEC8)));

        JLabel elimTitle = new JLabel("  Eliminated");
        elimTitle.setFont(new Font("SansSerif", Font.BOLD, 11));
        elimTitle.setForeground(new Color(0x888780));
        elimTitle.setBorder(new MatteBorder(0, 0, 1, 0, new Color(0xD0CEC8)));
        elimTitle.setPreferredSize(new Dimension(160, 28));

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(new Color(0xF1F0EA));
        rightPanel.setBorder(new MatteBorder(0, 1, 0, 0, new Color(0xD0CEC8)));
        rightPanel.add(elimTitle,  BorderLayout.NORTH);
        rightPanel.add(elimScroll, BorderLayout.CENTER);

        // Centre
        JPanel centre = new JPanel(new BorderLayout());
        centre.setOpaque(false);
        centre.add(circlePanel, BorderLayout.CENTER);
        centre.add(rightPanel,  BorderLayout.EAST);

        // Controls bar
        JPanel controls = buildControlsBar();

        p.add(centre,   BorderLayout.CENTER);
        p.add(controls, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildControlsBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        bar.setBackground(new Color(0xEEEDEA));
        bar.setBorder(new MatteBorder(1, 0, 0, 0, new Color(0xD0CEC8)));

        Font btnFont = new Font("SansSerif", Font.PLAIN, 13);

        stepBtn.setFont(btnFont);
        playBtn.setFont(btnFont);
        restartBtn.setFont(btnFont);

        stepBtn.addActionListener(e -> {
            if (controller != null) controller.step();
        });

        playBtn.addActionListener(e -> {
            if (controller == null) return;
            if (controller.isRunning()) {
                controller.pause();
                playBtn.setText("Auto play");
            } else {
                controller.play();
                playBtn.setText("Pause");
            }
        });

        restartBtn.addActionListener(e -> goToSetup());

        JLabel speedLbl = new JLabel("Speed");
        speedLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        speedLbl.setForeground(new Color(0x606058));

        speedSlider.setSnapToTicks(true);
        speedSlider.setPaintTicks(false);
        speedSlider.setPreferredSize(new Dimension(90, 24));
        speedSlider.setOpaque(false);
        speedSlider.addChangeListener(e -> {
            if (controller != null) controller.setSpeed(speedSlider.getValue());
        });

        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(0x606058));

        bar.add(stepBtn);
        bar.add(playBtn);
        bar.add(restartBtn);
        bar.add(Box.createHorizontalStrut(8));
        bar.add(speedLbl);
        bar.add(speedSlider);
        bar.add(Box.createHorizontalStrut(8));
        bar.add(statusLabel);

        return bar;
    }

    // ── Start simulation ──────────────────────────────────────────────────────
    private void startSimulation() {
        String raw = namesArea.getText().trim();
        if (raw.isEmpty()) { error("Please enter at least two names."); return; }

        String[] names = raw.lines()
                            .map(String::trim)
                            .filter(s -> !s.isEmpty() && !s.startsWith("#"))
                            .toArray(String[]::new);

        if (names.length < 2) { error("Please enter at least two names."); return; }

        int k = (int) kSpinner.getValue();

        // Reset UI
        ((DefaultListModel<String>) elimList.getModel()).clear();
        statusLabel.setText("Circle of " + names.length + "  —  k = " + k);
        stepBtn.setEnabled(true);
        playBtn.setEnabled(true);
        playBtn.setText("Auto play");

        // Build state + controller
        SimulationState state = new SimulationState(names, k);
        circlePanel.setState(state);

        if (controller != null) controller.dispose();

        controller = new SimulationController(
            state,
            circlePanel,
            () -> onElimination(state),
            () -> onFinished(state)
        );

        cardLayout.show(rootPanel, CARD_SIM);
    }

    private void onElimination(SimulationState state) {
        // Sync the eliminated list widget with the state log
        DefaultListModel<String> model = (DefaultListModel<String>) elimList.getModel();
        model.clear();
        for (String entry : state.eliminatedLog) model.addElement(entry);
        elimList.ensureIndexIsVisible(model.size() - 1);

        statusLabel.setText("Eliminated: "
            + state.eliminatedLog.get(state.eliminatedLog.size() - 1).replaceFirst("#\\d+\\s+", "")
            + "  |  " + state.alive.size() + " remaining");
    }

    private void onFinished(SimulationState state) {
        stepBtn.setEnabled(false);
        playBtn.setEnabled(false);
        playBtn.setText("Auto play");
        boolean match = state.survivorName.equals(state.mathSurvivorName);
        statusLabel.setText("Survivor: " + state.survivorName
            + "  |  Formula: " + state.mathSurvivorName
            + "  " + (match ? "✓" : "✗"));
    }

    // ── Navigation ────────────────────────────────────────────────────────────
    private void goToSetup() {
        if (controller != null) { controller.dispose(); controller = null; }
        playBtn.setText("Auto play");
        cardLayout.show(rootPanel, CARD_SETUP);
    }

    // ── File loader ───────────────────────────────────────────────────────────
    private void loadFile() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                List<String> lines = NamesFileReader.readNames(fc.getSelectedFile().getPath());
                namesArea.setText(String.join("\n", lines));
            } catch (IOException ex) {
                error("Could not read file:\n" + ex.getMessage());
            }
        }
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // ── Entry point ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(JosephusGUI::new);
    }
}
