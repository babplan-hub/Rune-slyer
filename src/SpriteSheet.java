import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.imageio.ImageIO;

/** Content-aware extraction using an independent annotation for each source image. */
public final class SpriteSheet {
    private static final Map<String, Atlas> CACHE = new HashMap<>();
    private static final int ALPHA_FLOOR = 16;
    private SpriteSheet() { }

    /** Tight crop and original foot pivot. Cropping never changes character scale. */
    public static final class Frame {
        public final BufferedImage image;
        public final int sourceX, sourceY;
        public final double pivotX, pivotY, bodyHeight;
        private Frame(BufferedImage image, int x, int y, double px, double py, double bodyHeight) {
            this.image = image;
            sourceX = x; sourceY = y;
            pivotX = px - x; pivotY = py - y;
            this.bodyHeight = bodyHeight;
        }
    }

    public static final class Atlas {
        public final Frame[][] rows;
        public final int[][] playback, horizontalCuts;
        public final int[][][] verticalCuts;
        public final int width, height;
        private Atlas(Frame[][] rows, int[][] playback, int[][] h, int[][][] v, int width, int height) {
            this.rows = rows; this.playback = playback;
            horizontalCuts = h; verticalCuts = v;
            this.width = width; this.height = height;
        }
        /** Gameplay ticks stay unchanged; art rows can contain different numbers of poses. */
        public Frame[][] timeline(int ticks) {
            if (ticks <= 0) throw new IllegalArgumentException("Timeline ticks must be positive");
            Frame[][] result = new Frame[rows.length][ticks];
            for (int r = 0; r < rows.length; r++) {
                int duration = r < 3 ? Math.min(8, ticks) : ticks;
                for (int t = 0; t < ticks; t++) {
                    int index = (t % duration) * playback[r].length / duration;
                    result[r][t] = rows[r][playback[r][index]];
                }
            }
            return result;
        }
    }

    public static synchronized Atlas load(String sourceName) throws IOException {
        String key = Path.of(sourceName).toAbsolutePath().normalize().toString();
        Atlas cached = CACHE.get(key);
        if (cached != null) return cached;
        Path sourcePath = Path.of(sourceName);
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(Path.of(sourceName + ".sprites"))) { p.load(in); }
        if (!sha256(Files.readAllBytes(sourcePath)).equals(p.getProperty("sha256")))
            throw new IOException(sourceName + " changed: re-check its .sprites annotation before using it");
        BufferedImage source = ImageIO.read(sourcePath.toFile());
        if (source == null) throw new IOException("Unreadable sprite image: " + sourceName);
        int w = source.getWidth(), h = source.getHeight();
        int[] expected = ints(p.getProperty("size"));
        if (expected.length != 2 || w != expected[0] || h != expected[1])
            throw new IOException("Annotation dimensions do not match " + sourceName);
        int[] pixels = source.getRGB(0, 0, w, h, null, 0, w);
        repairGuideLines(pixels, w, h, p.getProperty("guides"));
        for (int i = 0; i < pixels.length; i++) if ((pixels[i] >>> 24) <= ALPHA_FLOOR) pixels[i] = 0;
        int[] bands = ints(p.getProperty("bands"));
        if (bands.length != 9 || bands[0] != 0 || bands[8] != h)
            throw new IOException("Eight animation bands are required: " + sourceName);
        int[][] horizontal = new int[9][w];
        Arrays.fill(horizontal[8], h);
        for (int r = 1; r < 8; r++) {
            if (bands[r] <= bands[r - 1]) throw new IOException("Unordered bands: " + sourceName);
            horizontal[r] = seam(pixels, w, h, bands[r], Math.max(1, bands[r] - 12),
                    Math.min(h - 2, bands[r] + 12), true, 0, w);
        }
        double bodyHeight = Double.parseDouble(p.getProperty("bodyHeight"));
        if (!(bodyHeight > 0)) throw new IOException("Invalid bodyHeight: " + sourceName);
        Frame[][] rows = new Frame[8][];
        int[][] playback = new int[8][];
        int[][][] vertical = new int[8][][];
        for (int r = 0; r < 8; r++) {
            int[] anchors = ints(p.getProperty("row." + r));
            if (anchors.length == 0 || anchors.length % 2 != 0) throw new IOException("Invalid anchors in row " + r);
            int count = anchors.length / 2;
            rows[r] = new Frame[count];
            vertical[r] = new int[count + 1][h];
            Arrays.fill(vertical[r][count], w);
            for (int f = 0; f < count; f++) {
                int ax = anchors[2 * f], ay = anchors[2 * f + 1];
                if (ax < 0 || ax >= w || ay < bands[r] - 12 || ay > bands[r + 1] + 12
                        || (f > 0 && ax <= anchors[2 * f - 2]))
                    throw new IOException("Invalid pivot: row=" + r + " frame=" + f + " in " + sourceName);
                if (f > 0) {
                    int previous = anchors[2 * f - 2], guide = (previous + ax) / 2;
                    // Per-pose search corridors, not a global grid or fixed crop size.
                    vertical[r][f] = seam(pixels, w, h, guide, previous + 14, ax - 14, false,
                            Math.max(0, bands[r] - 12), Math.min(h, bands[r + 1] + 12));
                }
            }
            for (int f = 1; f < count; f++) {
                String override = p.getProperty("cut." + r + "." + f);
                if (override != null) vertical[r][f] = polygonCut(override, w, h);
            }
            for (int y = 0; y < h; y++) for (int f = 1; f <= count; f++)
                if (vertical[r][f][y] < vertical[r][f - 1][y]) throw new IOException("Crossing cut paths in " + sourceName);
            for (int f = 0; f < count; f++) {
                rows[r][f] = extract(pixels, w, h, horizontal[r], horizontal[r + 1],
                        vertical[r][f], vertical[r][f + 1], anchors[2 * f], anchors[2 * f + 1], bodyHeight);
                String exclude = p.getProperty("exclude." + r + "." + f);
                if (exclude != null) excludePolygon(rows[r][f], exclude);
                if ("main".equals(p.getProperty("keep." + r + "." + f))) keepMainComponent(rows[r][f].image);
            }
            String sequence = p.getProperty("play." + r);
            playback[r] = sequence == null ? new int[count] : ints(sequence);
            if (sequence == null) for (int f = 0; f < count; f++) playback[r][f] = f;
            if (playback[r].length == 0) throw new IOException("Empty playback row " + r);
            for (int index : playback[r]) if (index < 0 || index >= count) throw new IOException("Invalid playback index " + index);
        }
        Atlas atlas = new Atlas(rows, playback, horizontal, vertical, w, h);
        CACHE.put(key, atlas);
        return atlas;
    }

    public static void draw(Graphics2D g, Frame frame, int x, int y, int width, int height, boolean flip) {
        if (frame == null) return;
        Graphics2D copy = (Graphics2D) g.create();
        copy.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        double scale = height * 0.85 / frame.bodyHeight;
        AffineTransform transform = new AffineTransform();
        transform.translate(x + width / 2.0, y + height * 0.94);
        transform.scale(flip ? -scale : scale, scale);
        transform.translate(-frame.pivotX, -frame.pivotY);
        copy.drawImage(frame.image, transform, null);
        copy.dispose();
    }

    private static Frame extract(int[] pixels, int w, int h, int[] top, int[] bottom,
            int[] left, int[] right, int ax, int ay, double bodyHeight) throws IOException {
        int x0 = w, y0 = h, x1 = -1, y1 = -1;
        for (int y = 0; y < h; y++) for (int x = left[y]; x < right[y]; x++) {
            if (y >= top[x] && y < bottom[x] && (pixels[y * w + x] >>> 24) > 0) {
                x0 = Math.min(x0, x); x1 = Math.max(x1, x);
                y0 = Math.min(y0, y); y1 = Math.max(y1, y);
            }
        }
        if (x1 < x0) throw new IOException("Empty sprite around pivot " + ax + "," + ay);
        BufferedImage image = new BufferedImage(x1 - x0 + 5, y1 - y0 + 5, BufferedImage.TYPE_INT_ARGB);
        // Two transparent pixels around every independently extracted pose: no texture bleed.
        for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++)
            if (x >= left[y] && x < right[y] && y >= top[x] && y < bottom[x])
                image.setRGB(x - x0 + 2, y - y0 + 2, pixels[y * w + x]);
        removeDebris(image);
        return new Frame(image, x0 - 2, y0 - 2, ax, ay, bodyHeight);
    }

    /** Remove isolated noise, retaining detached weapons and substantial VFX. */
    private static void removeDebris(BufferedImage image) {
        int w = image.getWidth(), h = image.getHeight();
        int[] p = image.getRGB(0, 0, w, h, null, 0, w);
        boolean[] seen = new boolean[p.length];
        int[] queue = new int[p.length];
        for (int seed = 0; seed < p.length; seed++) {
            if (seen[seed] || (p[seed] >>> 24) == 0) continue;
            int head = 0, tail = 0, opaque = 0;
            queue[tail++] = seed; seen[seed] = true;
            while (head < tail) {
                int pos = queue[head++], x = pos % w, y = pos / w;
                if ((p[pos] >>> 24) > 64) opaque++;
                for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                    int nx = x + dx, ny = y + dy;
                    if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                    int next = ny * w + nx;
                    if (!seen[next] && (p[next] >>> 24) > 0) { seen[next] = true; queue[tail++] = next; }
                }
            }
            if (opaque < 6) for (int i = 0; i < tail; i++) p[queue[i]] = 0;
        }
        image.setRGB(0, 0, w, h, p, 0, w);
    }

    /** An explicitly reviewed exception for a disconnected piece of a neighbouring effect. */
    private static void keepMainComponent(BufferedImage image) {
        int w = image.getWidth(), h = image.getHeight();
        int[] pixels = image.getRGB(0, 0, w, h, null, 0, w), labels = new int[pixels.length];
        int[] queue = new int[pixels.length];
        int label = 0, bestLabel = 0, bestSize = 0;
        for (int seed = 0; seed < pixels.length; seed++) {
            if (labels[seed] != 0 || pixels[seed] >>> 24 == 0) continue;
            int head = 0, tail = 0, size = 0;
            queue[tail++] = seed; labels[seed] = ++label;
            while (head < tail) {
                int pos = queue[head++], x = pos % w, y = pos / w;
                if ((pixels[pos] >>> 24) > 64) size++;
                for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                    int nx = x + dx, ny = y + dy;
                    if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                    int next = ny * w + nx;
                    if (labels[next] == 0 && (pixels[next] >>> 24) > 0) {
                        labels[next] = label; queue[tail++] = next;
                    }
                }
            }
            if (size > bestSize) { bestSize = size; bestLabel = label; }
        }
        for (int i = 0; i < pixels.length; i++) if (labels[i] != bestLabel) pixels[i] = 0;
        image.setRGB(0, 0, w, h, pixels, 0, w);
    }

    /** Reviewed source-space mask; removes a neighbouring effect without redrawing the character. */
    private static void excludePolygon(Frame frame, String coordinates) throws IOException {
        int[] points = ints(coordinates);
        if (points.length < 6 || points.length % 2 != 0) throw new IOException("Invalid exclusion polygon");
        Polygon polygon = new Polygon();
        for (int i = 0; i < points.length; i += 2) polygon.addPoint(points[i], points[i + 1]);
        for (int y = 0; y < frame.image.getHeight(); y++) for (int x = 0; x < frame.image.getWidth(); x++)
            if (polygon.contains(frame.sourceX + x + 0.5, frame.sourceY + y + 0.5)) frame.image.setRGB(x, y, 0);
    }

    private static int[] polygonCut(String value, int width, int height) throws IOException {
        int[] points = ints(value), cut = new int[height];
        if (points.length < 4 || points.length % 2 != 0) throw new IOException("Invalid manual cut");
        for (int i = 0; i < points.length; i += 2)
            if (points[i] < 0 || points[i] > width) throw new IOException("Manual cut outside source image");
        int segment = 0;
        for (int i = 3; i < points.length; i += 2)
            if (points[i] <= points[i - 2]) throw new IOException("Manual cut y coordinates must increase");
        for (int y = 0; y < height; y++) {
            while (segment + 4 < points.length && y > points[segment + 3]) segment += 2;
            double t = Math.max(0, Math.min(1, (double) (y - points[segment + 1]) /
                    (points[segment + 3] - points[segment + 1])));
            cut[y] = (int) Math.round(points[segment] * (1 - t) + points[segment + 2] * t);
        }
        return cut;
    }

    /** Repair only known guide overlays, never all white character/effect pixels. */
    private static void repairGuideLines(int[] pixels, int w, int h, String guides) {
        if (guides == null) return;
        int[] spacing = ints(guides);
        int[] original = pixels.clone();
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            if (original[y * w + x] != 0x87ffffff || (x % spacing[0] != 0 && y % spacing[1] != 0)) continue;
            int dx = x % spacing[0] == 0 ? 1 : 0, dy = y % spacing[1] == 0 ? 1 : 0;
            int a = original[Math.max(0, y - dy) * w + Math.max(0, x - dx)];
            int b = original[Math.min(h - 1, y + dy) * w + Math.min(w - 1, x + dx)];
            if (a == 0x87ffffff) a = b;
            if (b == 0x87ffffff) b = a;
            int blended = 0;
            for (int shift : new int[]{0, 8, 16, 24})
                blended |= (((a >>> shift & 255) + (b >>> shift & 255)) / 2) << shift;
            pixels[y * w + x] = blended;
        }
    }

    /** Lowest-opacity path through an annotated gap; can curve around swords and capes. */
    private static int[] seam(int[] p, int w, int h, int guide, int lo, int hi,
            boolean horizontal, int start, int end) {
        int length = horizontal ? w : h, n = hi - lo + 1;
        int[] result = new int[length];
        Arrays.fill(result, guide);
        if (n <= 0) throw new IllegalArgumentException("Overlapping pivot search corridors");
        double[] previous = new double[n];
        int[][] back = new int[end - start][n];
        for (int t = start; t < end; t++) {
            double[] current = new double[n];
            for (int i = 0; i < n; i++) {
                int pos = lo + i, bestIndex = i;
                double best = Double.POSITIVE_INFINITY;
                for (int j = Math.max(0, i - 4); j <= Math.min(n - 1, i + 4); j++) {
                    double value = previous[j] + Math.abs(j - i) * 4;
                    if (value < best) { best = value; bestIndex = j; }
                }
                int x = horizontal ? t : pos, y = horizontal ? pos : t;
                double cost = 0;
                for (int d = -1; d <= 1; d++) {
                    int nx = horizontal ? x : Math.max(0, Math.min(w - 1, x + d));
                    int ny = horizontal ? Math.max(0, Math.min(h - 1, y + d)) : y;
                    int alpha = p[ny * w + nx] >>> 24;
                    cost += alpha * alpha;
                }
                current[i] = best + cost + (pos - guide) * (pos - guide) * 0.12;
                back[t - start][i] = bestIndex;
            }
            previous = current;
        }
        int chosen = 0;
        for (int i = 1; i < n; i++) if (previous[i] < previous[chosen]) chosen = i;
        for (int t = end - 1; t >= start; t--) {
            result[t] = lo + chosen;
            chosen = back[t - start][chosen];
        }
        return result;
    }

    private static int[] ints(String value) {
        if (value == null || value.trim().isEmpty()) return new int[0];
        return Arrays.stream(value.trim().split("[ ,:]+")).mapToInt(Integer::parseInt).toArray();
    }

    public static String sha256(byte[] bytes) throws IOException {
        try {
            StringBuilder value = new StringBuilder();
            for (byte b : MessageDigest.getInstance("SHA-256").digest(bytes)) value.append(String.format("%02x", b & 255));
            return value.toString();
        } catch (java.security.NoSuchAlgorithmException e) { throw new IOException(e); }
    }
}
