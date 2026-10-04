import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** No dependencies or display server required. Run with -Djava.awt.headless=true. */
public final class SpriteRegressionTest {
    public static void main(String[] args) throws Exception {
        int frames = 0;
        for (String file : SpriteAudit.FILES) {
            SpriteSheet.Atlas atlas = SpriteSheet.load("res/" + file + ".png");
            try { atlas.timeline(0); throw new AssertionError("Zero tick timeline accepted"); }
            catch (IllegalArgumentException expected) { }
            for (SpriteSheet.Frame[] row : atlas.rows) for (SpriteSheet.Frame frame : row) {
                BufferedImage normal = render(frame, false), flipped = render(frame, true);
                Rectangle a = bounds(normal), b = bounds(flipped);
                check(Math.abs(a.x - (512 - b.x - b.width)) <= 1, "Flip shifts horizontal pivot: " + file);
                check(Math.abs(a.y - b.y) <= 1 && Math.abs(a.height - b.height) <= 1, "Flip shifts feet: " + file);
                check(Math.abs(a.width - b.width) <= 1, "Flip changes scale: " + file);
                frames++;
            }
        }
        validateBadAssets();
        validateBalance();
        validateGameplay();
        System.out.println("PASS: " + frames + " mirrored poses; stable flip pivots/scale; invalid assets rejected; combat timing and dungeon reset unchanged.");
    }

    private static BufferedImage render(SpriteSheet.Frame frame, boolean flip) {
        BufferedImage image = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        AffineTransform before = g.getTransform();
        SpriteSheet.draw(g, frame, 224, 120, 64, 84, flip);
        check(g.getTransform().equals(before), "Drawing changed caller transform");
        g.dispose();
        return image;
    }

    private static Rectangle bounds(BufferedImage image) {
        int x0 = image.getWidth(), y0 = image.getHeight(), x1 = -1, y1 = -1;
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
            if ((image.getRGB(x, y) >>> 24) <= 16) continue;
            x0 = Math.min(x0, x); x1 = Math.max(x1, x); y0 = Math.min(y0, y); y1 = Math.max(y1, y);
        }
        check(x1 >= x0, "Invisible pose");
        return new Rectangle(x0, y0, x1 - x0 + 1, y1 - y0 + 1);
    }

    private static void validateBadAssets() throws Exception {
        Path folder = Files.createTempDirectory("runeslayer-sprite-test-");
        Path png = folder.resolve("probe.png"), manifest = folder.resolve("probe.png.sprites");
        try {
            Files.copy(Path.of("res/player/01.png"), png);
            expectLoadFailure(png, "Missing annotation");
            String original = Files.readString(Path.of("res/player/01.png.sprites"));
            Files.writeString(manifest, original.replaceFirst("sha256=[a-f0-9]+", "sha256=invalid"));
            expectLoadFailure(png, "Changed source fingerprint");
            Files.writeString(manifest, original.replace("size=1536,1024", "size=1,1"));
            expectLoadFailure(png, "Wrong source dimensions");
        } finally {
            Files.deleteIfExists(manifest); Files.deleteIfExists(png); Files.deleteIfExists(folder);
        }
    }

    private static void expectLoadFailure(Path file, String reason) throws Exception {
        try { SpriteSheet.load(file.toString()); throw new AssertionError(reason + " was accepted"); }
        catch (IOException expected) { }
    }

    private static void validateGameplay() throws Exception {
        for (int stage = 0; stage < 3; stage++) {
            Player p = new Player(500, 400);
            set(p, "evolutionStage", stage);
            p.attack();
            for (int tick = 0; tick < 64; tick++) {
                int frame = tick / 4;
                check(p.isAttackHitboxActive() == (frame >= 4 && frame <= 12), "Attack hit timing changed");
                p.update();
            }
            check(!(boolean) get(p, "attacking"), "Attack no longer ends at 64 ticks");
            p.skill();
            for (int tick = 0; tick < 64; tick++) {
                int frame = tick / 4;
                boolean hit = stage == 2 ? frame >= 11 : stage == 1 ? frame >= 8 && frame <= 12 : frame >= 8 && frame <= 10;
                check(p.isAttackHitboxActive() == hit, "Skill hit timing changed");
                p.update();
            }
            check(!(boolean) get(p, "skillActive"), "Skill no longer ends at 64 ticks");
        }
        GamePanel panel = new GamePanel();
        panel.setSize(1200, 700);
        press(panel, KeyEvent.VK_ENTER);
        check(get(panel, "boss") == null, "Boss appears in first forest map");
        press(panel, KeyEvent.VK_X);
        Boss first = (Boss) get(panel, "boss");
        check(first != null, "X did not enter dungeon");
        first.takeDamage(100);
        press(panel, KeyEvent.VK_X);
        check(!(boolean) get(panel, "inBossDungeon"), "X did not exit dungeon");
        press(panel, KeyEvent.VK_X);
        Boss reset = (Boss) get(panel, "boss");
        check(reset != first && reset.getHP() == reset.getMaxHP(), "Leaving did not reset boss HP");
        check(reset.getBossNumber() == 1, "Unfinished boss was skipped");
        press(panel, KeyEvent.VK_X);
        BufferedImage scene = new BufferedImage(1200, 700, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scene.createGraphics(); panel.paint(g); g.dispose();
        Files.createDirectories(Path.of("artifacts/sprite-audit"));
        ImageIO.write(scene, "png", Path.of("artifacts/sprite-audit/gameplay-smoke.png").toFile());
    }

    private static void validateBalance() throws Exception {
        int[] skillDamage = {35, 50, 120};
        int[] hitsToKill = {3, 2, 1};
        double[] lifesteal = {0.0, 0.10, 0.20};
        for (int form = 0; form < 3; form++) {
            Player player = new Player(500, 400);
            set(player, "evolutionStage", form);
            check(player.getSkillDamage() == skillDamage[form], "Wrong E-skill damage for Form " + (form + 1));
            player.skill();
            Rectangle skillRange = player.getAttackHitbox();
            int expectedWidth = form == 0 ? 160 : form == 1 ? 200 : 260;
            int expectedHeight = form == 2 ? 220 : 54;
            check(skillRange.width == expectedWidth && skillRange.height == expectedHeight,
                    "Wrong E-skill boundary for Form " + (form + 1));
            Monster monster = new Monster(750, 400);
            int hits = 0;
            while (!monster.isDead()) {
                monster.takeDamage(player.getSkillDamage());
                hits++;
            }
            check(hits == hitsToKill[form], "Form " + (form + 1) + " E-skill takes " + hits + " hits");
            check(player.getMaxHP() == 200 + form * 100, "Max HP should grow by 100 per form");
            set(player, "hp", 100);
            check(Math.abs(player.getLifeStealPercent() - lifesteal[form]) < 0.0001,
                    "Wrong lifesteal rate for Form " + (form + 1));
            player.onAttackHit(50);
            check(player.getHP() == 100 + (int) (50 * lifesteal[form]),
                    "Wrong lifesteal healing for Form " + (form + 1));
            check(player.getEvolution1Cost() == 5 && player.getEvolution2Cost() == 10,
                    "Rune costs should be 5 for Form 2 and 10 for Form 3");
        }

        Player aoePlayer = new Player(500, 400);
        set(aoePlayer, "evolutionStage", 2);
        aoePlayer.skill();
        Rectangle aoe = aoePlayer.getAttackHitbox();
        check(aoe.width == 260 && aoe.height == 220, "Form 3 E-skill should use a wide area hitbox");
        check(aoe.x < 500 && aoe.y < 400 && aoe.x + aoe.width > 563 && aoe.y + aoe.height > 484,
                "Form 3 E-skill area should surround the player");

        Player progression = new Player(500, 400);
        for (int i = 0; i < 5; i++) progression.addRune();
        progression.evolve();
        check(progression.getEvolutionStage() == 1 && progression.getRuneCount() == 0
                        && progression.getMaxHP() == 300 && progression.getHP() == 300,
                "Five runes should unlock Form 2 and raise HP to 300");
        set(progression, "usingEffect", false);
        for (int i = 0; i < 10; i++) progression.addRune();
        progression.evolve();
        check(progression.getEvolutionStage() == 2 && progression.getRuneCount() == 0
                        && progression.getMaxHP() == 400 && progression.getHP() == 400,
                "Ten runes should unlock Form 3 and raise HP to 400");

        int[] bossHealth = {1000, 1250, 1500, 2000};
        for (int i = 0; i < bossHealth.length; i++) {
            check(new Boss(500, 300, i + 1).getMaxHP() == bossHealth[i],
                    "Boss " + (i + 1) + " should have increased max HP");
        }
    }

    private static void press(GamePanel panel, int key) {
        panel.keyPressed(new KeyEvent(panel, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, key, KeyEvent.CHAR_UNDEFINED));
    }
    private static Object get(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object);
    }
    private static void set(Object object, String name, Object value) throws Exception {
        Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); field.set(object, value);
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
