import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.imageio.ImageIO;

/** Read-only analysis of the supplied artwork; previews are written to the audit folder. */
public final class SpriteInspector {
    public static void main(String[] args) throws Exception {
        File audit = new File("artifacts/sprite-audit");
        if (!audit.isDirectory() && !audit.mkdirs()) throw new IllegalStateException("Cannot create " + audit);
        if (args.length == 5) {
            BufferedImage source = ImageIO.read(new File(args[0]));
            int x = Integer.parseInt(args[1]), y = Integer.parseInt(args[2]);
            int w = Integer.parseInt(args[3]), h = Integer.parseInt(args[4]);
            BufferedImage detail = new BufferedImage(w * 3, h * 3, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = detail.createGraphics();
            g.setColor(new Color(65, 69, 80)); g.fillRect(0, 0, w * 3, h * 3);
            g.drawImage(source, 0, 0, w * 3, h * 3, x, y, x + w, y + h, null);
            g.setColor(new Color(255, 200, 100, 130));
            for (int xx = 0; xx < w; xx += 20) { g.drawLine(xx * 3, 0, xx * 3, h * 3); g.drawString("" + (x + xx), xx * 3, 12); }
            for (int yy = 20; yy < h; yy += 20) { g.drawLine(0, yy * 3, w * 3, yy * 3); g.drawString("" + (y + yy), 2, yy * 3 - 2); }
            g.dispose();
            ImageIO.write(detail, "png", new File(audit, "detail.png"));
            return;
        }
        for (String name : args) {
            BufferedImage source = ImageIO.read(new File(name));
            int width = source.getWidth(), height = source.getHeight();
            int[] pixels = source.getRGB(0, 0, width, height, null, 0, width);
            int[] histogram = new int[256];
            for (int pixel : pixels) histogram[pixel >>> 24]++;
            if (Boolean.getBoolean("detail")) {
                for (int a = 0; a < 256; a += 16) {
                    int n = 0;
                    for (int b = a; b < a + 16; b++) n += histogram[b];
                    System.out.printf("alpha%d-%d:%d ", a, a + 15, n);
                }
                System.out.println();
                for (int y : new int[]{0, 50, 128, 500}) {
                    for (int x : new int[]{0, 95, 96, 97, 191, 192, 193})
                        System.out.printf("(%d,%d)=%08x ", x, y, source.getRGB(x, y));
                    System.out.println();
                }
            }
            if (name.endsWith("gg.png") || name.matches(".*boss[123]\\.png")) {
                int[] original = pixels.clone();
                for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
                    if (x % 96 != 0 && y % 128 != 0) continue;
                    if (x == 0 || y == 0) { pixels[y * width + x] = 0; continue; }
                    int nx = x % 96 == 0 ? x - 1 : x;
                    int ny = y % 128 == 0 ? y - 1 : y;
                    pixels[y * width + x] = original[ny * width + nx];
                }
            }
            System.out.printf("%s %dx%d alpha0=%d alpha255=%d partial=%d%n", name, width, height,
                    histogram[0], histogram[255], pixels.length - histogram[0] - histogram[255]);
            List<int[]> components = components(pixels, width, height, 64);
            components.removeIf(box -> box[4] < 120);
            components.sort(Comparator.comparingInt((int[] box) -> box[1]).thenComparingInt(box -> box[0]));
            System.out.println("Components (x,y,w,h,area)=" + components.size());
            for (int[] box : components) {
                System.out.printf("%d,%d,%d,%d,%d ", box[0], box[1], box[2], box[3], box[4]);
            }
            System.out.println();
            System.out.print("Horizontal valleys:");
            for (int row = 1; row < 8; row++) {
                int expected = row * height / 8, best = expected, score = Integer.MAX_VALUE;
                for (int y = Math.max(1, expected - 60); y < Math.min(height - 1, expected + 60); y++) {
                    int count = 0;
                    for (int x = 0; x < width; x++) if ((pixels[y * width + x] >>> 24) > 64) count++;
                    if (count < score) { score = count; best = y; }
                }
                System.out.printf(" %d(%d)", best, score);
            }
            System.out.println();
            BufferedImage preview = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = preview.createGraphics();
            for (int y = 0; y < height; y += 16) {
                for (int x = 0; x < width; x += 16) {
                    g.setColor(((x / 16 + y / 16) & 1) == 0 ? new Color(70, 75, 85) : new Color(95, 100, 110));
                    g.fillRect(x, y, 16, 16);
                }
            }
            g.drawImage(source, 0, 0, null);
            g.dispose();
            String slug = name.replace("res/", "").replace('/', '-').replace(".png", "");
            ImageIO.write(preview, "png", new File(audit, slug + "-source.png"));
        }
    }

    private static List<int[]> components(int[] pixels, int width, int height, int threshold) {
        boolean[] visited = new boolean[pixels.length];
        int[] queue = new int[pixels.length];
        List<int[]> results = new ArrayList<>();
        for (int seed = 0; seed < pixels.length; seed++) {
            if (visited[seed] || (pixels[seed] >>> 24) <= threshold) continue;
            int head = 0, tail = 0;
            queue[tail++] = seed;
            visited[seed] = true;
            int left = width, top = height, right = 0, bottom = 0;
            while (head < tail) {
                int position = queue[head++], x = position % width, y = position / width;
                left = Math.min(left, x); top = Math.min(top, y);
                right = Math.max(right, x); bottom = Math.max(bottom, y);
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = x + dx, ny = y + dy;
                        if (nx < 0 || ny < 0 || nx >= width || ny >= height) continue;
                        int next = ny * width + nx;
                        if (!visited[next] && (pixels[next] >>> 24) > threshold) {
                            visited[next] = true;
                            queue[tail++] = next;
                        }
                    }
                }
            }
            results.add(new int[]{left, top, right - left + 1, bottom - top + 1, tail});
        }
        return results;
    }
}
