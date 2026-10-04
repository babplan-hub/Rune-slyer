import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import javax.imageio.ImageIO;

/** Exports runtime crops, contact sheets, annotated originals, and frame metadata. */
public final class SpriteAudit {
    static final String[] FILES = {"player/01", "player/Evo1", "player/Evo2", "monster/gg",
            "summon/summon", "boss/boss1", "boss/boss2", "boss/boss3", "boss/boss4"};
    static final String[] STATES = {"idle", "walk", "run", "attack1", "attack2", "hurt", "death", "skill"};
    static final int[][] COUNTS = {{18,18,18,18,15,18,14,16}, {17,17,17,16,14,16,13,17},
            {17,17,17,16,13,16,13,14}, {16,16,16,16,16,15,15,14}, {16,16,16,16,16,16,15,14},
            {16,16,16,15,14,14,14,13}, {16,16,16,15,15,16,16,15}, {16,16,16,16,16,16,15,14},
            {16,16,16,16,16,16,15,16}};
    public static void main(String[] args) throws Exception {
        Path output = Path.of("artifacts/sprite-audit");
        Files.createDirectories(output);
        StringBuilder csv = new StringBuilder("asset,state,frame,sourceX,sourceY,width,height,pivotX,pivotY,bodyHeight\n");
        int total = 0;
        long start = System.nanoTime();
        for (int s = 0; s < FILES.length; s++) {
            String file = FILES[s], slug = file.replace('/', '-');
            SpriteSheet.Atlas atlas = SpriteSheet.load("res/" + file + ".png");
            if (SpriteSheet.load("res/" + file + ".png") != atlas) throw new AssertionError("Atlas not cached");
            int[] ownership = new int[atlas.width * atlas.height];
            Arrays.fill(ownership, -1);
            Path folder = output.resolve("frames/" + slug);
            Files.createDirectories(folder);
            BufferedImage[] strips = new BufferedImage[8];
            int serial = 0;
            for (int r = 0; r < 8; r++) {
                if (atlas.rows[r].length != COUNTS[s][r]) throw new AssertionError("Unexpected count " + file + " row " + r);
                int width = 0, height = 0;
                for (SpriteSheet.Frame f : atlas.rows[r]) {
                    width += Math.max(100, f.image.getWidth() + 24);
                    height = Math.max(height, f.image.getHeight() + 40);
                }
                BufferedImage strip = background(width, height);
                Graphics2D g = strip.createGraphics();
                g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
                int x = 0;
                for (int c = 0; c < atlas.rows[r].length; c++) {
                    SpriteSheet.Frame f = atlas.rows[r][c];
                    verifyFrame(f, ownership, ++serial, atlas.width, atlas.height);
                    String imageName = STATES[r] + "-" + String.format("%02d", c) + ".png";
                    ImageIO.write(f.image, "png", folder.resolve(imageName).toFile());
                    csv.append(file).append(',').append(STATES[r]).append(',').append(c).append(',')
                            .append(f.sourceX).append(',').append(f.sourceY).append(',').append(f.image.getWidth()).append(',')
                            .append(f.image.getHeight()).append(',').append(f.pivotX).append(',').append(f.pivotY).append(',')
                            .append(f.bodyHeight).append('\n');
                    int cw = Math.max(100, f.image.getWidth() + 24), ix = x + (cw - f.image.getWidth()) / 2;
                    int iy = height - 8 - f.image.getHeight();
                    boolean selected = false;
                    for (int index : atlas.playback[r]) if (index == c) selected = true;
                    g.setColor(selected ? Color.WHITE : new Color(255, 185, 85));
                    g.drawString(STATES[r] + "." + c + (selected ? "" : " [source]"), x + 7, 15);
                    g.drawImage(f.image, ix, iy, null);
                    int px = ix + (int) f.pivotX, py = iy + (int) f.pivotY;
                    g.setColor(new Color(255, 185, 85));
                    g.drawLine(px - 3, py, px + 3, py); g.drawLine(px, py - 3, px, py + 3);
                    g.setColor(new Color(130, 140, 155));
                    g.drawLine(x + cw - 1, 0, x + cw - 1, height);
                    x += cw;
                    total++;
                }
                g.dispose();
                strips[r] = strip;
                ImageIO.write(strip, "png", output.resolve(slug + "-" + STATES[r] + ".png").toFile());
            }
            int mw = 0, mh = 0;
            for (BufferedImage strip : strips) { mw = Math.max(mw, strip.getWidth()); mh += strip.getHeight() + 12; }
            BufferedImage overview = background(mw, mh);
            Graphics2D g = overview.createGraphics();
            int yy = 0;
            for (BufferedImage strip : strips) { g.drawImage(strip, 0, yy, null); yy += strip.getHeight() + 12; }
            g.dispose();
            ImageIO.write(overview, "png", output.resolve(slug + "-contact.png").toFile());
            annotate(file, atlas, output.resolve(slug + "-boundaries.png"));
            verifyTimeline(atlas);
            System.out.println(file + " " + Arrays.toString(COUNTS[s]) + " : bounds, ownership, padding, timeline PASS");
        }
        Files.writeString(output.resolve("frames.csv"), csv);
        smokeTest(output);
        System.out.printf("PASS: %d separate poses; no source pixel shared by two frames. %.2f seconds.%n",
                total, (System.nanoTime() - start) / 1e9);
    }

    private static void verifyFrame(SpriteSheet.Frame f, int[] owner, int id, int w, int h) {
        int visible = 0;
        for (int y = 0; y < f.image.getHeight(); y++) for (int x = 0; x < f.image.getWidth(); x++) {
            int p = f.image.getRGB(x, y);
            if ((p >>> 24) == 0) continue;
            visible++;
            if (x < 2 || y < 2 || x >= f.image.getWidth() - 2 || y >= f.image.getHeight() - 2)
                throw new AssertionError("No transparent padding");
            int sx = f.sourceX + x, sy = f.sourceY + y;
            if (sx < 0 || sx >= w || sy < 0 || sy >= h) throw new AssertionError("Out of bounds");
            int pos = sy * w + sx;
            if (owner[pos] >= 0) throw new AssertionError("Source pixel belongs to frames " + owner[pos] + " and " + id);
            owner[pos] = id;
            if (p == 0x87ffffff) throw new AssertionError("Guide line leaked into sprite");
        }
        if (visible < 5 || !Double.isFinite(f.pivotX + f.pivotY) || f.bodyHeight <= 0)
            throw new AssertionError("Empty/invalid frame");
    }

    private static void verifyTimeline(SpriteSheet.Atlas atlas) {
        SpriteSheet.Frame[][] timeline = atlas.timeline(16);
        for (int r = 0; r < 8; r++) for (int c = 0; c < 16; c++) {
            if (timeline[r][c] == null) throw new AssertionError("Missing timeline frame");
            BufferedImage image = new BufferedImage(700, 360, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            SpriteSheet.draw(g, timeline[r][c], 180, 140, 63, 84, false);
            SpriteSheet.draw(g, timeline[r][c], 480, 140, 63, 84, true);
            g.dispose();
        }
    }

    private static void annotate(String file, SpriteSheet.Atlas atlas, Path target) throws Exception {
        BufferedImage image = background(atlas.width, atlas.height);
        Graphics2D g = image.createGraphics();
        g.drawImage(ImageIO.read(Path.of("res/" + file + ".png").toFile()), 0, 0, null);
        for (int r = 1; r < 8; r++) {
            g.setColor(new Color(255, 170, 0));
            for (int x = 0; x < atlas.width; x++) g.fillRect(x, atlas.horizontalCuts[r][x], 1, 1);
        }
        for (int r = 0; r < 8; r++) for (int c = 1; c < atlas.verticalCuts[r].length - 1; c++) {
            g.setColor(new Color(0, 255, 200));
            for (int y = 0; y < atlas.height; y++) {
                int x = atlas.verticalCuts[r][c][y];
                if (y >= atlas.horizontalCuts[r][x] && y < atlas.horizontalCuts[r + 1][x]) g.fillRect(x, y, 1, 1);
            }
        }
        g.dispose();
        ImageIO.write(image, "png", target.toFile());
    }

    private static void smokeTest(Path output) throws Exception {
        BufferedImage image = background(1200, 700);
        Graphics2D g = image.createGraphics();
        g.drawImage(ImageIO.read(Path.of("res/map/forest.png").toFile()), 0, 0, 1200, 700, null);
        Monster monster = new Monster(130, 283);
        monster.draw(g);
        java.lang.reflect.Field evolution = Player.class.getDeclaredField("evolutionStage");
        evolution.setAccessible(true);
        for (int stage = 0; stage < 3; stage++) {
            Player p = new Player(285 + stage * 145, 270);
            evolution.setInt(p, stage);
            p.draw(g);
        }
        new Summon(755, 255).draw(g);
        // Boss.draw also paints its HUD, so use its same runtime frame helper for the size comparison.
        new Boss(935, 239, 1).draw(g);
        g.setColor(Color.WHITE); g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        String[] labels = {"gg", "Player 0", "Evo1", "Evo2", "Summon", "Boss"};
        int[] xs = {135, 285, 430, 575, 755, 935};
        for (int i = 0; i < labels.length; i++) g.drawString(labels[i], xs[i], 400);
        g.dispose();
        ImageIO.write(image, "png", output.resolve("in-game-scale.png").toFile());
    }

    private static BufferedImage background(int w, int h) {
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        for (int y = 0; y < h; y += 16) for (int x = 0; x < w; x += 16) {
            g.setColor(((x / 16 + y / 16) & 1) == 0 ? new Color(42, 47, 57) : new Color(53, 59, 70));
            g.fillRect(x, y, 16, 16);
        }
        g.dispose();
        return image;
    }
}
