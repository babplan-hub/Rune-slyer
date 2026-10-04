import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("Rune Slayer");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setResizable(false);

            Game game = new Game();
            window.add(game);
            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);

            game.startGameThread();
        });
    }
}
