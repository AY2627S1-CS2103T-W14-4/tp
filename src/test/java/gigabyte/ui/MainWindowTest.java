package gigabyte.ui;

import static gigabyte.testutil.TypicalClients.ALICE;
import static gigabyte.testutil.TypicalClients.BENSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gigabyte.commons.core.GuiSettings;
import gigabyte.logic.Logic;
import gigabyte.logic.commands.CommandResult;
import gigabyte.logic.commands.exceptions.CommandException;
import gigabyte.logic.parser.exceptions.ParseException;
import gigabyte.model.client.Client;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.GigStatus;
import gigabyte.model.gig.GigTitle;
import gigabyte.model.gig.PaymentObligation;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.TextField;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class MainWindowTest {
    private static final Gig ALICE_GIG = new Gig(ALICE, new GigTitle("Website redesign"), GigStatus.IN_PROGRESS,
            new Deadline("2027-01-31"), new Fee("1000"));
    private static final PaymentObligation ALICE_PAYMENT = new PaymentObligation(
            ALICE_GIG, new Fee("500"), new Deadline("2027-02-15"), false);
    private static final Gig BENSON_GIG = new Gig(BENSON, new GigTitle("Brand identity"), GigStatus.NOT_STARTED,
            new Deadline("2027-03-31"), new Fee("1200"));
    private static final PaymentObligation BENSON_PAYMENT = new PaymentObligation(
            BENSON_GIG, new Fee("600"), new Deadline("2027-04-15"), false);

    @BeforeAll
    public static void setUpJavaFx() {
        JavaFxTestUtil.startToolkit();
    }

    @Test
    public void selectingClientAndGig_matchingHierarchy_showsPaymentAndReturnsCommandFocus() throws Exception {
        MainWindow mainWindow = callOnJavaFxThread(() -> {
            MainWindow window = new MainWindow(new Stage(), new LogicStub(), Path.of("data.json"));
            window.show();
            window.fillInnerParts();
            return window;
        });

        try {
            callOnJavaFxThread(() -> {
                mainWindow.getClientListPanel().getClientListView().getSelectionModel().select(0);
                assertEquals(1, mainWindow.getGigListPanel().getGigListView().getItems().size());
                mainWindow.getGigListPanel().getGigListView().getSelectionModel().select(0);
                assertEquals(1, mainWindow.getPaymentObligationListPanel()
                        .getPaymentListView().getItems().size());
                return null;
            });

            callOnJavaFxThread(() -> {
                TextField commandTextField = (TextField) mainWindow.getRoot().getScene()
                        .lookup("#commandTextField");
                assertTrue(commandTextField.isFocused());
                return null;
            });
        } finally {
            callOnJavaFxThread(() -> {
                mainWindow.getPrimaryStage().hide();
                return null;
            });
        }
    }

    @Test
    public void windowMinimumSize_scaled1280By720_fitsLogicalViewport() throws Exception {
        MainWindow mainWindow = callOnJavaFxThread(() ->
                new MainWindow(new Stage(), new LogicStub(), Path.of("data.json")));

        assertTrue(mainWindow.getPrimaryStage().getMinWidth() <= 1280 / 1.5);
        assertTrue(mainWindow.getPrimaryStage().getMinHeight() <= 720 / 1.5);
        assertEquals(480, MainWindow.clampWindowDimension(600, 450, 720 / 1.5));
    }

    @Test
    public void selectingAnotherClient_previousGigSelected_clearsGigAndPaymentSelection() throws Exception {
        MainWindow mainWindow = callOnJavaFxThread(() -> {
            MainWindow window = new MainWindow(new Stage(), new LogicStub(), Path.of("data.json"));
            window.show();
            window.fillInnerParts();
            return window;
        });

        try {
            callOnJavaFxThread(() -> {
                mainWindow.getClientListPanel().getClientListView().getSelectionModel().select(0);
                mainWindow.getGigListPanel().getGigListView().getSelectionModel().select(0);
                assertEquals(1, mainWindow.getPaymentObligationListPanel()
                        .getPaymentListView().getItems().size());

                mainWindow.getClientListPanel().getClientListView().getSelectionModel().select(1);

                assertEquals(1, mainWindow.getGigListPanel().getGigListView().getItems().size());
                assertEquals(BENSON_GIG, mainWindow.getGigListPanel().getGigListView().getItems().get(0));
                assertNull(mainWindow.getGigListPanel().getGigListView().getSelectionModel().getSelectedItem());
                assertEquals(0, mainWindow.getPaymentObligationListPanel()
                        .getPaymentListView().getItems().size());
                assertEquals("Select a gig to view its payment obligations.",
                        mainWindow.getPaymentObligationListPanel().getEmptyState().getText());
                return null;
            });
        } finally {
            callOnJavaFxThread(() -> {
                mainWindow.getPrimaryStage().hide();
                return null;
            });
        }
    }

    @Test
    public void windowSize_savedSizeExceedsScreen_clampsToVisualBounds() throws Exception {
        GuiSettings oversizedSettings = new GuiSettings(2000, 2000, 10000, 10000);
        MainWindow mainWindow = callOnJavaFxThread(() ->
                new MainWindow(new Stage(), new LogicStub(oversizedSettings), Path.of("data.json")));
        Rectangle2D visualBounds = callOnJavaFxThread(() -> Screen.getPrimary().getVisualBounds());

        assertTrue(mainWindow.getPrimaryStage().getWidth() <= visualBounds.getWidth());
        assertTrue(mainWindow.getPrimaryStage().getHeight() <= visualBounds.getHeight());
        assertTrue(mainWindow.getPrimaryStage().getX() >= visualBounds.getMinX());
        assertTrue(mainWindow.getPrimaryStage().getY() >= visualBounds.getMinY());
        assertTrue(mainWindow.getPrimaryStage().getX() + mainWindow.getPrimaryStage().getWidth()
                <= visualBounds.getMaxX());
        assertTrue(mainWindow.getPrimaryStage().getY() + mainWindow.getPrimaryStage().getHeight()
                <= visualBounds.getMaxY());
    }

    private static <T> T callOnJavaFxThread(Callable<T> action) throws Exception {
        CompletableFuture<T> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                result.complete(action.call());
            } catch (Throwable throwable) {
                result.completeExceptionally(throwable);
            }
        });
        return result.get(10, TimeUnit.SECONDS);
    }

    private static class LogicStub implements Logic {
        private final ObservableList<Client> clients = FXCollections.observableArrayList(ALICE, BENSON);
        private final ObservableList<Gig> gigs = FXCollections.observableArrayList(ALICE_GIG, BENSON_GIG);
        private final ObservableList<PaymentObligation> payments =
                FXCollections.observableArrayList(ALICE_PAYMENT, BENSON_PAYMENT);
        private final GuiSettings guiSettings;

        LogicStub() {
            this(new GuiSettings());
        }

        LogicStub(GuiSettings guiSettings) {
            this.guiSettings = guiSettings;
        }

        @Override
        public CommandResult execute(String commandText) throws CommandException, ParseException {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public ObservableList<Client> getFilteredClientList() {
            return clients;
        }

        @Override
        public ObservableList<Gig> getGigList() {
            return gigs;
        }

        @Override
        public ObservableList<PaymentObligation> getPaymentObligationList() {
            return payments;
        }

        @Override
        public GuiSettings getGuiSettings() {
            return guiSettings;
        }

        @Override
        public void setGuiSettings(GuiSettings guiSettings) {
            throw new AssertionError("This method should not be called.");
        }
    }
}
