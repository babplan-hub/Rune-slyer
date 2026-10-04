import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class GameMap {

    public void draw(
            Graphics2D g2,
            BufferedImage map
    ) {

        if (map != null) {

            g2.drawImage(
                    map,
                    0,
                    0,
                    1200,
                    700,
                    null
            );
        }
    }
}