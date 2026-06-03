import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.RenderingHints;

/**
 * Custom Swing panel that draws the Josephus circle.
 *
 * It reads from a {@link SimulationState} and re-renders whenever
 * {@code repaint()} is called.  It does not modify the state.
 *
 * Layout:
 *   - Names are placed as labelled circles around the perimeter.
 *   - A golden arrow rotates from the centre toward the current person.
 *   - A small hub in the centre shows the current step counter.
 */
public class CirclePanel extends JPanel {

    // ── Colours ──────────────────────────────────────────────────────────────
    private static final Color BG            = new Color(0xF8F7F2);
    private static final Color RING          = new Color(0x000000, false);  // tinted below
    private static final Color NODE_FILL     = new Color(0xEEEDEA);
    private static final Color NODE_BORDER   = new Color(0xC8C6C0);
    private static final Color NODE_TEXT     = new Color(0x2C2C2A);
    private static final Color ACTIVE_FILL   = new Color(0x7F77DD);
    private static final Color ACTIVE_BORDER = new Color(0x534AB7);
    private static final Color ACTIVE_TEXT   = Color.WHITE;
    private static final Color ARROW_COLOR   = new Color(0xBA7517);
    private static final Color HUB_FILL      = new Color(0xEEEDFE);
    private static final Color HUB_BORDER    = new Color(0x7F77DD);
    private static final Color HUB_TEXT      = new Color(0x534AB7);
    private static final Color ELIM_FILL     = new Color(0xE24B4A);
    private static final Color ELIM_TEXT     = Color.WHITE;

    // ── Fonts ────────────────────────────────────────────────────────────────
    private static final Font FONT_NODE   = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font FONT_ACTIVE = new Font("SansSerif", Font.BOLD,  12);
    private static final Font FONT_HUB    = new Font("SansSerif", Font.BOLD,  16);
    private static final Font FONT_LABEL  = new Font("SansSerif", Font.PLAIN, 10);
    private static final Font FONT_DONE   = new Font("SansSerif", Font.BOLD,  22);
    private static final Font FONT_VERIFY = new Font("SansSerif", Font.PLAIN, 13);

    // ── State ────────────────────────────────────────────────────────────────
    private SimulationState state;

    // ── Constructor ──────────────────────────────────────────────────────────
    public CirclePanel() {
        setBackground(BG);
        setPreferredSize(new Dimension(480, 480));
    }

    public void setState(SimulationState state) {
        this.state = state;
        repaint();
    }

    // ── Painting ─────────────────────────────────────────────────────────────
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (state == null) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();
        int cx = w / 2, cy = h / 2;

        int n = state.alive.size();
        if (n == 0) { g2.dispose(); return; }

        double circleR = computeCircleR(n, w, h);
        double nodeR   = computeNodeR(n);

        // ── Ring ──────────────────────────────────────────────────────────────
        g2.setColor(new Color(0, 0, 0, 25));
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(new Ellipse2D.Double(cx - circleR, cy - circleR, circleR * 2, circleR * 2));

        // ── Arrow (drawn before nodes so nodes appear on top) ─────────────────
        drawArrow(g2, cx, cy, circleR, nodeR, state.arrowIndex, n);

        // ── Nodes ─────────────────────────────────────────────────────────────
        for (int i = 0; i < n; i++) {
            drawNode(g2, cx, cy, circleR, nodeR, i, n);
        }

        // ── Hub (centre counter) ──────────────────────────────────────────────
        drawHub(g2, cx, cy, n);

        // ── Finished overlay ──────────────────────────────────────────────────
        if (state.finished) {
            drawFinishedOverlay(g2, cx, cy, w, h);
        }

        g2.dispose();
    }

    // ── Node drawing ─────────────────────────────────────────────────────────
    private void drawNode(Graphics2D g2, int cx, int cy,
                          double circleR, double nodeR, int i, int n) {

        boolean isActive      = (i == state.arrowIndex) && !state.finished;
        boolean isEliminating = (i == state.eliminatingIndex);

        Point2D pos = nodePosition(cx, cy, circleR, i, n);
        double  x   = pos.getX(), y = pos.getY();

        // Determine fill / alpha
        Composite originalComposite = g2.getComposite();
        if (isEliminating) {
            g2.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, state.eliminatingAlpha));
        }

        // Filled circle
        Ellipse2D circle = new Ellipse2D.Double(
                x - nodeR, y - nodeR, nodeR * 2, nodeR * 2);

        if (isEliminating) {
            g2.setColor(ELIM_FILL);
        } else if (isActive) {
            g2.setColor(ACTIVE_FILL);
        } else {
            g2.setColor(NODE_FILL);
        }
        g2.fill(circle);

        // Border
        g2.setStroke(new BasicStroke(isActive ? 1.5f : 0.75f));
        g2.setColor(isActive || isEliminating ? ACTIVE_BORDER : NODE_BORDER);
        g2.draw(circle);

        // Pulse ring for active node
        if (isActive && !state.finished) {
            double pr = nodeR + 5;
            g2.setColor(new Color(127, 119, 221, 60));
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(new Ellipse2D.Double(x - pr, y - pr, pr * 2, pr * 2));
        }

        // Name label — clip to node width
        String name = state.alive.get(i);
        Font   font = isActive || isEliminating ? FONT_ACTIVE : FONT_NODE;
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();

        String display = name;
        while (display.length() > 1 &&
               fm.stringWidth(display) > nodeR * 1.8) {
            display = display.substring(0, display.length() - 1);
        }
        if (!display.equals(name)) display = display + "…";

        g2.setColor(isEliminating ? ELIM_TEXT : (isActive ? ACTIVE_TEXT : NODE_TEXT));
        int tw = fm.stringWidth(display);
        g2.drawString(display, (float)(x - tw / 2.0), (float)(y + fm.getAscent() / 2.0 - 1));

        if (isEliminating) {
            g2.setComposite(originalComposite);
        }
    }

    // ── Arrow drawing ─────────────────────────────────────────────────────────
    private void drawArrow(Graphics2D g2, int cx, int cy,
                           double circleR, double nodeR,
                           int targetIdx, int n) {

        double angle = nodeAngle(targetIdx, n);

        double startR = 28;
        double endR   = circleR - nodeR - 6;

        double x1 = cx + Math.cos(angle) * startR;
        double y1 = cy + Math.sin(angle) * startR;
        double x2 = cx + Math.cos(angle) * endR;
        double y2 = cy + Math.sin(angle) * endR;

        // Dashed trail
        g2.setColor(new Color(186, 117, 23, 80));
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                1f, new float[]{5f, 5f}, 0f));
        g2.draw(new Line2D.Double(x1, y1, x2, y2));

        // Solid arrow shaft
        g2.setColor(ARROW_COLOR);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Line2D.Double(x1, y1, x2, y2));

        // Arrowhead
        double headLen = 12;
        double spread  = 0.38;
        double ax1 = x2 - headLen * Math.cos(angle - spread);
        double ay1 = y2 - headLen * Math.sin(angle - spread);
        double ax2 = x2 - headLen * Math.cos(angle + spread);
        double ay2 = y2 - headLen * Math.sin(angle + spread);

        int[] xPts = {(int) x2, (int) ax1, (int) ax2};
        int[] yPts = {(int) y2, (int) ay1, (int) ay2};
        g2.setColor(ARROW_COLOR);
        g2.fillPolygon(xPts, yPts, 3);
    }

    // ── Hub (centre counter) ──────────────────────────────────────────────────
    private void drawHub(Graphics2D g2, int cx, int cy, int n) {
        int hubR = 24;
        Ellipse2D hub = new Ellipse2D.Double(cx - hubR, cy - hubR, hubR * 2, hubR * 2);
        g2.setColor(HUB_FILL);
        g2.fill(hub);
        g2.setColor(HUB_BORDER);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(hub);

        // Step counter number
        g2.setFont(FONT_HUB);
        g2.setColor(HUB_TEXT);
        String counter = String.valueOf(state.stepCount);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(counter, cx - fm.stringWidth(counter) / 2, cy + 5);

        // "count" label below number
        g2.setFont(FONT_LABEL);
        g2.setColor(new Color(127, 119, 221, 180));
        String lbl = "count";
        fm = g2.getFontMetrics();
        g2.drawString(lbl, cx - fm.stringWidth(lbl) / 2, cy + 17);

        // Remaining label below hub
        g2.setFont(FONT_LABEL);
        g2.setColor(new Color(100, 100, 90, 160));
        String rem = n + " remaining";
        fm = g2.getFontMetrics();
        g2.drawString(rem, cx - fm.stringWidth(rem) / 2, cy + hubR + 14);
    }

    // ── Finished overlay ──────────────────────────────────────────────────────
    private void drawFinishedOverlay(Graphics2D g2, int cx, int cy, int w, int h) {
        // Semi-transparent banner at the bottom of the circle panel
        int bh = 70;
        g2.setColor(new Color(255, 255, 255, 220));
        g2.fillRoundRect(20, h - bh - 20, w - 40, bh, 12, 12);
        g2.setColor(new Color(0, 0, 0, 40));
        g2.setStroke(new BasicStroke(0.5f));
        g2.drawRoundRect(20, h - bh - 20, w - 40, bh, 12, 12);

        // Survivor name
        g2.setFont(FONT_DONE);
        g2.setColor(new Color(0x1D9E75));
        String winner = "Survivor: " + state.survivorName;
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(winner, cx - fm.stringWidth(winner) / 2, h - bh + 8);

        // Verification line
        boolean match = state.survivorName.equals(state.mathSurvivorName);
        g2.setFont(FONT_VERIFY);
        g2.setColor(match ? new Color(0x3B6D11) : new Color(0xA32D2D));
        String verify = (match ? "✓ " : "✗ ") + "Formula predicts: " + state.mathSurvivorName;
        fm = g2.getFontMetrics();
        g2.drawString(verify, cx - fm.stringWidth(verify) / 2, h - bh + 30);
    }

    // ── Geometry helpers ──────────────────────────────────────────────────────
    private double nodeAngle(int idx, int n) {
        return (idx / (double) n) * 2 * Math.PI - Math.PI / 2;
    }

    private Point2D nodePosition(int cx, int cy, double circleR, int i, int n) {
        double angle = nodeAngle(i, n);
        return new Point2D.Double(cx + Math.cos(angle) * circleR,
                                  cy + Math.sin(angle) * circleR);
    }

    private double computeCircleR(int n, int w, int h) {
        double maxR = Math.min(w, h) / 2.0 - 48;
        return Math.max(80, Math.min(maxR, 155));
    }

    private double computeNodeR(int n) {
        return Math.max(20, Math.min(30, 280.0 / n));
    }
}
