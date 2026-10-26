package gigabyte.ui;

import java.util.List;

import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/** Handles keyboard navigation across the client, gig, and payment lists. */
final class ListNavigationController {
    private final List<ListView<?>> views;
    private final CommandBox commandBox;

    ListNavigationController(ListView<?> clients, ListView<?> gigs, ListView<?> payments, CommandBox commandBox) {
        views = List.of(clients, gigs, payments);
        this.commandBox = commandBox;
        for (ListView<?> view : views) {
            view.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
                if (isFocused && view.getSelectionModel().getSelectedIndex() < 0 && !view.getItems().isEmpty()) {
                    view.getSelectionModel().selectFirst();
                    view.scrollTo(0);
                }
            });
        }
    }

    /** Installs navigation shortcuts on the main scene. */
    void install(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
    }

    private void handleKeyPressed(KeyEvent event) {
        boolean hasModifier = event.isAltDown() || event.isControlDown() || event.isMetaDown()
                || event.isShiftDown();
        if (event.getCode() == KeyCode.TAB) {
            if (!hasModifier) {
                if (commandBox.isInputFocused()) {
                    views.get(0).requestFocus();
                } else {
                    for (ListView<?> view : views) {
                        if (view.isFocused()) {
                            commandBox.focusInput();
                            break;
                        }
                    }
                }
            }
            // Prevent JavaFX from moving focus to another control, including on Shift+Tab.
            event.consume();
            return;
        }

        if (hasModifier) {
            return;
        }

        for (int i = 0; i < views.size(); i++) {
            ListView<?> view = views.get(i);
            if (!view.isFocused()) {
                continue;
            }

            switch (event.getCode()) {
            case UP -> moveSelection(view, -1);
            case DOWN -> moveSelection(view, 1);
            case RIGHT -> {
                if (i + 1 < views.size()) {
                    views.get(i + 1).requestFocus();
                }
            }
            case LEFT -> {
                if (i > 0) {
                    views.get(i - 1).requestFocus();
                }
            }
            default -> {
                return;
            }
            }
            event.consume();
            return;
        }
    }

    /** Selects the next row, starting at the first or last row when nothing is selected. */
    private static void moveSelection(ListView<?> view, int direction) {
        int size = view.getItems().size();
        if (size == 0) {
            return;
        }
        int selectedIndex = view.getSelectionModel().getSelectedIndex();
        int nextIndex = selectedIndex < 0 ? (direction > 0 ? 0 : size - 1)
                : Math.max(0, Math.min(size - 1, selectedIndex + direction));
        view.getSelectionModel().clearAndSelect(nextIndex);
        view.scrollTo(nextIndex);
    }
}
