import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Plays the game's MP3 ambience continuously using the host's native media player. */
public final class BackgroundMusic {

    private static final Path TRACK = Path.of("res/Audio/blackground.mp3");
    private static final double MAC_VOLUME = 0.28;
    private static final int FFMPEG_VOLUME = 28;

    private static volatile boolean running;
    private static volatile Process playerProcess;
    private static Thread musicThread;

    private BackgroundMusic() { }

    public static synchronized void start() {
        if (running) return;
        if (!Files.isRegularFile(TRACK)) {
            System.err.println("Background music not found: " + TRACK);
            return;
        }

        running = true;
        musicThread = new Thread(BackgroundMusic::playLoop, "runeslayer-background-music");
        musicThread.setDaemon(true);
        musicThread.start();
        Runtime.getRuntime().addShutdownHook(new Thread(BackgroundMusic::stop, "runeslayer-audio-shutdown"));
    }

    public static synchronized void stop() {
        running = false;
        Process process = playerProcess;
        if (process != null) process.destroy();
        Thread thread = musicThread;
        if (thread != null) thread.interrupt();
    }

    private static void playLoop() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String[] command = os.contains("mac")
                ? new String[] {"afplay", "-v", Double.toString(MAC_VOLUME), TRACK.toString()}
                : new String[] {"ffplay", "-nodisp", "-autoexit", "-loglevel", "quiet",
                        "-volume", Integer.toString(FFMPEG_VOLUME), TRACK.toString()};

        while (running) {
            try {
                Process process = new ProcessBuilder(command)
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .redirectError(ProcessBuilder.Redirect.DISCARD)
                        .start();
                playerProcess = process;
                process.waitFor();
                playerProcess = null;
                if (running) Thread.sleep(150);
            } catch (IOException e) {
                System.err.println("Could not play background music. Install ffplay if afplay is unavailable.");
                running = false;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
    }
}
