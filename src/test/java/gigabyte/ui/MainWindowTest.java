package gigabyte.ui;

import static gigabyte.testutil.TypicalClients.ALICE;
import static gigabyte.testutil.TypicalClients.BENSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import gigabyte.commons.core.GuiSettings;
import gigabyte.logic.Logic;
import gigabyte.logic.LogicManager;
import gigabyte.logic.commands.CommandResult;
import gigabyte.logic.commands.exceptions.CommandException;
import gigabyte.logic.parser.exceptions.ParseException;
import gigabyte.model.Model;
import gigabyte.model.ModelManager;
import gigabyte.model.client.Client;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.GigStatus;
import gigabyte.model.gig.GigTitle;
import gigabyte.model.gig.PaymentObligation;
import gigabyte.storage.JsonGigabyteDataStorage;
import gigabyte.storage.JsonUserPrefsStorage;
import gigabyte.storage.StorageManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
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

    @TempDir
    public Path temporaryFolder;

    @BeforeAll
    public static void setUpJavaFx() {
        JavaFxTestUtil.startToolkit();
    }

    @Test
    public void gigAndPaymentCommands_switchViewsAndPreserveClientWorkflow() throws Exception {
        Model model = new ModelManager();
        model.addClient(ALICE);
        model.addClient(BENSON);
        model.addGig(ALICE_GIG);
        model.addGig(BENSON_GIG);
        model.addPaymentObligation(ALICE_PAYMENT);
        model.addPaymentObligation(BENSON_PAYMENT);
        Logic logic = new LogicManager(model, new StorageManager(
                new JsonGigabyteDataStorage(temporaryFolder.resolve("data.json")),
                new JsonUserPrefsStorage(temporaryFolder.resolve("preferences.json"))));
        MainWindow window = callOnJavaFxThread(() -> {
            MainWindow mainWindow = new MainWindow(new Stage(), logic, temporaryFolder.resolve("data.json"));
            mainWindow.fillInnerParts();
            return mainWindow;
        });

        try {
            callOnJavaFxThread(() -> {
                window.getClientListPanel().getClientListView().getSelectionModel().select(0);
                window.getGigListPanel().getGigListView().getSelectionModel().select(0);
                enterCommand(window, "gigs s/NOT_STARTED");
                assertEquals(List.of(BENSON_GIG), window.getGigListPanel().getGigListView().getItems());
                assertNull(window.getClientListPanel().getClientListView().getSelectionModel().getSelectedItem());
                assertEquals(0, window.getPaymentObligationListPanel().getPaymentListView().getItems().size());
                assertEquals(model.getFilteredClientList(), window.getClientListPanel().getClientListView().getItems());

                window.getGigListPanel().getGigListView().getSelectionModel().select(0);
                assertEquals(List.of(BENSON_PAYMENT),
                        window.getPaymentObligationListPanel().getPaymentListView().getItems());
                enterCommand(window, "list");
                assertEquals(List.of(BENSON_GIG), window.getGigListPanel().getGigListView().getItems());
                assertEquals(BENSON_GIG, window.getGigListPanel().getGigListView()
                        .getSelectionModel().getSelectedItem());
                assertEquals(List.of(BENSON_PAYMENT),
                        window.getPaymentObligationListPanel().getPaymentListView().getItems());
                enterCommand(window, "gigs s/INVALID");
                assertEquals(BENSON_GIG, window.getGigListPanel().getGigListView()
                        .getSelectionModel().getSelectedItem());
                assertEquals(List.of(BENSON_PAYMENT),
                        window.getPaymentObligationListPanel().getPaymentListView().getItems());

                enterCommand(window, "gigs s/COMPLETED");
                assertEquals("No gigs with status COMPLETED.", window.getGigListPanel().getEmptyState().getText());
                assertEquals(0, window.getPaymentObligationListPanel().getPaymentListView().getItems().size());
                window.getClientListPanel().getClientListView().getSelectionModel().select(0);
                assertEquals(List.of(ALICE_GIG), window.getGigListPanel().getGigListView().getItems());

                window.getGigListPanel().getGigListView().getSelectionModel().select(0);
                enterCommand(window, "outstanding");
                assertEquals(List.of(ALICE_PAYMENT, BENSON_PAYMENT),
                        window.getPaymentObligationListPanel().getPaymentListView().getItems());
                assertNull(window.getClientListPanel().getClientListView().getSelectionModel().getSelectedItem());
                assertNull(window.getGigListPanel().getGigListView().getSelectionModel().getSelectedItem());
                enterCommand(window, "outstanding extra");
                enterCommand(window, "list");
                enterCommand(window, "find Alice");
                assertEquals(List.of(ALICE_PAYMENT, BENSON_PAYMENT),
                        window.getPaymentObligationListPanel().getPaymentListView().getItems());
                assertEquals(List.of(ALICE), window.getClientListPanel().getClientListView().getItems());
                window.getClientListPanel().getClientListView().getSelectionModel().select(0);
                window.getGigListPanel().getGigListView().getSelectionModel().select(0);
                assertEquals(List.of(ALICE_PAYMENT),
                        window.getPaymentObligationListPanel().getPaymentListView().getItems());
                enterCommand(window, "gigs s/NOT_STARTED");
                window.getGigListPanel().getGigListView().getSelectionModel().select(0);
                enterCommand(window, "outstanding");
                assertNull(window.getGigListPanel().getGigListView().getSelectionModel().getSelectedItem());
                assertEquals(List.of(ALICE_PAYMENT, BENSON_PAYMENT),
                        window.getPaymentObligationListPanel().getPaymentListView().getItems());
                enterCommand(window, "deadlines");
                assertNull(window.getClientListPanel().getClientListView().getSelectionModel().getSelectedItem());
                assertEquals(List.of(ALICE_GIG, BENSON_GIG), window.getGigListPanel().getGigListView().getItems());
                enterCommand(window, "outstanding");
                enterCommand(window, "markpaid 1 1 1");
                assertEquals(List.of(BENSON_PAYMENT),
                        window.getPaymentObligationListPanel().getPaymentListView().getItems());
                enterCommand(window, "outstanding");
                assertEquals(List.of(BENSON_PAYMENT),
                        window.getPaymentObligationListPanel().getPaymentListView().getItems());
                enterCommand(window, "list");
                enterCommand(window, "markpaid 2 1 1");
                enterCommand(window, "outstanding");
                assertEquals(0, window.getPaymentObligationListPanel().getPaymentListView().getItems().size());
                assertEquals("No outstanding payment obligations.",
                        window.getPaymentObligationListPanel().getEmptyState().getText());
                return null;
            });
        } finally {
            callOnJavaFxThread(() -> {
                window.getPrimaryStage().hide();
                return null;
            });
        }
    }

    @Test
    public void markPaidCommand_refreshesDisplayedPaymentImmediately() throws Exception {
        Model model = new ModelManager();
        model.addClient(ALICE);
        model.addGig(ALICE_GIG);
        model.addPaymentObligation(ALICE_PAYMENT);
        Logic logic = new LogicManager(model, new StorageManager(
                new JsonGigabyteDataStorage(temporaryFolder.resolve("paid-data.json")),
                new JsonUserPrefsStorage(temporaryFolder.resolve("paid-preferences.json"))));
        MainWindow window = callOnJavaFxThread(() -> {
            MainWindow mainWindow = new MainWindow(new Stage(), logic, temporaryFolder.resolve("paid-data.json"));
            mainWindow.fillInnerParts();
            return mainWindow;
        });

        try {
            callOnJavaFxThread(() -> {
                window.getClientListPanel().getClientListView().getSelectionModel().select(0);
                window.getGigListPanel().getGigListView().getSelectionModel().select(0);
                assertEquals(List.of(ALICE_PAYMENT), window.getPaymentObligationListPanel()
                        .getPaymentListView().getItems());

                enterCommand(window, "markpaid 1 1 1");

                assertTrue(window.getPaymentObligationListPanel().getPaymentListView().getItems().get(0).isPaid());
                return null;
            });
        } finally {
            callOnJavaFxThread(() -> {
                window.getPrimaryStage().hide();
                return null;
            });
        }
    }

    private static void enterCommand(MainWindow window, String command) {
        TextField textField = (TextField) window.getRoot().getScene().lookup("#commandTextField");
        textField.setText(command);
        textField.fireEvent(new ActionEvent());
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
                mainWindow.getClientListPanel().getClientListView().requestFocus();
                assertTrue(mainWindow.getClientListPanel().getClientListView().isFocused());

                mainWindow.getClientListPanel().getClientListView().getSelectionModel().select(0);
                assertEquals(1, mainWindow.getGigListPanel().getGigListView().getItems().size());
                return null;
            });

            callOnJavaFxThread(() -> {
                TextField commandTextField = (TextField) mainWindow.getRoot().getScene()
                        .lookup("#commandTextField");
                assertTrue(commandTextField.isFocused());

                mainWindow.getGigListPanel().getGigListView().requestFocus();
                assertTrue(mainWindow.getGigListPanel().getGigListView().isFocused());

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
        assertEquals(440, MainWindow.clampWindowDimension(600, 450, 440));
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
