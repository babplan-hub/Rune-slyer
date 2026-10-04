import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Dimension;

public class Game extends JPanel {

    public static final int SCREEN_WIDTH = 1200;
    public static final int SCREEN_HEIGHT = 700;

    private static final long TICK_NANOS = 1_000_000_000L / 60;
    private static final int MAX_CATCH_UP_TICKS = 5;

    private final GamePanel gamePanel;
    private final Timer timer;
    private long lastTickNanos;
    private long accumulatorNanos;

    public Game() {
        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        setFocusable(true);
        gamePanel = new GamePanel();
        add(gamePanel);
        timer = new Timer(16, event -> advanceFrame());
        timer.setCoalesce(true);
    }

    public void startGameThread() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::startGameThread);
            return;
        }
        if (timer.isRunning()) return;
        lastTickNanos = System.nanoTime();
        accumulatorNanos = 0;
        gamePanel.startGame();
        timer.start();
    }

    public void stopGameThread() {
        if (SwingUtilities.isEventDispatchThread()) {
            timer.stop();
        } else {
            SwingUtilities.invokeLater(timer::stop);
        }
    }

    private void advanceFrame() {
        long now = System.nanoTime();
        long elapsed = Math.min(now - lastTickNanos, TICK_NANOS * MAX_CATCH_UP_TICKS);
        lastTickNanos = now;
        accumulatorNanos += elapsed;

        int ticks = 0;
        while (accumulatorNanos >= TICK_NANOS && ticks < MAX_CATCH_UP_TICKS) {
            gamePanel.update();
            accumulatorNanos -= TICK_NANOS;
            ticks++;
        }
        if (ticks == MAX_CATCH_UP_TICKS && accumulatorNanos >= TICK_NANOS) {
            accumulatorNanos %= TICK_NANOS;
        }
        gamePanel.repaint();
    }
}
