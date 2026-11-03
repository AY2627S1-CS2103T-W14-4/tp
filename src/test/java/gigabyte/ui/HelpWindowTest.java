package gigabyte.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.stage.Stage;

public class HelpWindowTest {

    @BeforeAll
    public static void setUpJavaFx() {
        JavaFxTestUtil.startToolkit();
    }

    @Test
    public void focus_minimizedWindow_restoresWindow() throws Exception {
        CompletableFuture<Void> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            HelpWindow helpWindow = new HelpWindow();
            Stage stage = helpWindow.getRoot();
            try {
                stage.setIconified(true);
                assertTrue(stage.isIconified());

                helpWindow.focus();

                assertFalse(stage.isIconified());
                result.complete(null);
            } catch (Throwable throwable) {
                result.completeExceptionally(throwable);
            } finally {
                helpWindow.hide();
            }
        });
        result.get(10, TimeUnit.SECONDS);
    }
}
