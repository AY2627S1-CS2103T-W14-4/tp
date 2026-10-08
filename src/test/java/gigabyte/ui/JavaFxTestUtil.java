package gigabyte.ui;

import javafx.application.Platform;

/** Test helper for starting the JavaFX toolkit once. */
public final class JavaFxTestUtil {
    private static boolean started;

    private JavaFxTestUtil() {}

    /** Starts the JavaFX toolkit if it has not already been started. */
    public static synchronized void startToolkit() {
        if (!started) {
            Platform.startup(() -> { });
            started = true;
        }
    }
}
