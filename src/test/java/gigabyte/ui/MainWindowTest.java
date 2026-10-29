package gigabyte.ui;

import static gigabyte.testutil.TypicalClients.ALICE;
import static gigabyte.testutil.TypicalClients.BENSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class MainWindowTest {
    private static final double SCALED_1280_BY_720_CONTENT_WIDTH = 841;
    private static final double SCALED_1280_BY_720_CONTENT_HEIGHT = 401;
    /** Window content of a maximised window on a 1920x1080 screen at 100% scale. */
    private static final double FULL_HD_CONTENT_WIDTH = 1902;
    private static final double FULL_HD_CONTENT_HEIGHT = 985;
    private static final Gig ALICE_GIG = new Gig(ALICE, new GigTitle("Website redesign"), GigStatus.IN_PROGRESS,
            new Deadline("2027-01-31"), new Fee("1000"));
    private static final PaymentObligation ALICE_PAYMENT = new PaymentObligation(
            ALICE_GIG, new Fee("500"), new Deadline("2027-02-15"), false);
    private static final Gig BENSON_GIG = new Gig(BENSON, new GigTitle("Brand identity"), GigStatus.NOT_STARTED,
            new Deadline("2027-03-31"), new Fee("1200"));
    private static final PaymentObligation BENSON_PAYMENT = new PaymentObligation(
            BENSON_GIG, new Fee("600"), new Deadline("2027-04-15"), false);

    @TempDir
    Path temporaryFolder;

    @BeforeAll
    public static void setUpJavaFx() {
        JavaFxTestUtil.startToolkit();
    }

    @Test
    public void editClientCommand_renameOutOfFilter_clearsSelectionAndGigs() throws Exception {
        Model model = new ModelManager();
        model.addClient(ALICE);
        model.addClient(BENSON);
        model.addGig(ALICE_GIG);
        Logic logic = new LogicManager(model, new StorageManager(
                new JsonGigabyteDataStorage(temporaryFolder.resolve("edit-filter-data.json")),
                new JsonUserPrefsStorage(temporaryFolder.resolve("edit-filter-preferences.json"))));
        MainWindow window = callOnJavaFxThread(() -> {
            MainWindow mainWindow = new MainWindow(new Stage(), logic,
                    temporaryFolder.resolve("edit-filter-data.json"));
            mainWindow.fillInnerParts();
            return mainWindow;
        });

        try {
            callOnJavaFxThread(() -> {
                enterCommand(window, "find Alice");
                ListView<Client> clientListView = window.getClientListPanel().getClientListView();
                clientListView.getSelectionModel().select(0);
                assertEquals(List.of(ALICE_GIG), window.getGigListPanel().getGigListView().getItems());

                enterCommand(window, "edit 1 n/Amelia Pauline");

                assertTrue(clientListView.getItems().isEmpty());
                assertNull(clientListView.getSelectionModel().getSelectedItem());
                assertTrue(window.getGigListPanel().getGigListView().getItems().isEmpty());
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
                assertEquals(model.getFilteredClientList(),
                        window.getClientListPanel().getClientListView().getItems());

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
    public void findGigCommand_filtersAcrossClientsAndSelectionShowsPayments() throws Exception {
        Model model = new ModelManager();
        model.addClient(ALICE);
        model.addClient(BENSON);
        model.addGig(ALICE_GIG);
        model.addGig(BENSON_GIG);
        model.addPaymentObligation(ALICE_PAYMENT);
        model.addPaymentObligation(BENSON_PAYMENT);
        Logic logic = new LogicManager(model, new StorageManager(
                new JsonGigabyteDataStorage(temporaryFolder.resolve("findgig-data.json")),
                new JsonUserPrefsStorage(temporaryFolder.resolve("findgig-preferences.json"))));
        MainWindow window = callOnJavaFxThread(() -> {
            MainWindow mainWindow = new MainWindow(new Stage(), logic, temporaryFolder.resolve("findgig-data.json"));
            mainWindow.fillInnerParts();
            return mainWindow;
        });

        try {
            callOnJavaFxThread(() -> {
                window.getClientListPanel().getClientListView().getSelectionModel().select(0);
                enterCommand(window, "findgig redesign identity");
                assertEquals(List.of(ALICE_GIG, BENSON_GIG), window.getGigListPanel().getGigListView().getItems());
                assertEquals(List.of(ALICE, BENSON), model.getFilteredClientList());
                assertEquals(0, window.getPaymentObligationListPanel().getPaymentListView().getItems().size());

                window.getGigListPanel().getGigListView().getSelectionModel().select(1);
                assertEquals(List.of(BENSON_PAYMENT), window.getPaymentObligationListPanel()
                        .getPaymentListView().getItems());
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

    @Test
    public void deleteGigCommand_selectedIdenticalGig_clearsSelectionInEveryView() throws Exception {
        for (String view : new String[] {"", "gigs s/IN_PROGRESS", "deadlines", "findgig redesign"}) {
            Model model = new ModelManager();
            model.addClient(ALICE);
            model.addGig(ALICE_GIG);
            model.addGig(ALICE_GIG);
            Gig survivor = model.getGigList().get(0);
            Gig target = model.getGigList().get(1);
            Logic logic = new LogicManager(model, new StorageManager(
                    new JsonGigabyteDataStorage(temporaryFolder.resolve("delete-gig.json")),
                    new JsonUserPrefsStorage(temporaryFolder.resolve("delete-gig-preferences.json"))));
            MainWindow window = callOnJavaFxThread(() -> {
                MainWindow mainWindow = new MainWindow(new Stage(), logic,
                        temporaryFolder.resolve("delete-gig.json"));
                mainWindow.fillInnerParts();
                return mainWindow;
            });
            try {
                callOnJavaFxThread(() -> {
                    window.getClientListPanel().getClientListView().getSelectionModel().select(0);
                    if (!view.isEmpty()) {
                        enterCommand(window, view);
                    }
                    int targetIndex = window.getGigListPanel().getGigListView().getItems().get(0).hasSameUid(target)
                            ? 0 : 1;
                    window.getGigListPanel().getGigListView().getSelectionModel().select(targetIndex);
                    assertSame(target, window.getGigListPanel().getGigListView()
                            .getSelectionModel().getSelectedItem());

                    enterCommand(window, "deletegig 1 2");

                    assertEquals(1, window.getGigListPanel().getGigListView().getItems().size());
                    assertSame(survivor, window.getGigListPanel().getGigListView().getItems().get(0));
                    assertNull(window.getGigListPanel().getGigListView().getSelectionModel().getSelectedItem());
                    assertTrue(window.getPaymentObligationListPanel().getPaymentListView().getItems().isEmpty());
                    assertEquals("Select a gig to view its payment obligations.",
                            window.getPaymentObligationListPanel().getEmptyState().getText());

                    enterCommand(window, "deletegig 1 1");

                    assertTrue(window.getGigListPanel().getGigListView().getItems().isEmpty());
                    assertEquals(view.isEmpty() ? "This client has no gigs."
                            : view.startsWith("gigs") ? "No gigs with status IN_PROGRESS."
                            : view.equals("deadlines") ? "There are no active gig deadlines."
                            : "No gigs match the supplied title keywords.",
                            window.getGigListPanel().getEmptyState().getText());
                    assertEquals(List.of(ALICE), model.getFilteredClientList());
                    return null;
                });
            } finally {
                callOnJavaFxThread(() -> {
                    window.getPrimaryStage().hide();
                    return null;
                });
            }
        }
    }

    @Test
    public void deleteGigCommand_unselectedGig_preservesSelectedGigAndPayments() throws Exception {
        Model model = new ModelManager();
        model.addClient(ALICE);
        model.addGig(ALICE_GIG);
        model.addGig(ALICE_GIG);
        Gig selected = model.getGigList().get(1);
        model.addPaymentObligation(new PaymentObligation(selected, new Fee("50"), new Deadline("2099-01-31"), true));
        PaymentObligation payment = model.getPaymentObligationList().get(0);
        Logic logic = new LogicManager(model, new StorageManager(
                new JsonGigabyteDataStorage(temporaryFolder.resolve("keep-selection.json")),
                new JsonUserPrefsStorage(temporaryFolder.resolve("keep-selection-preferences.json"))));
        MainWindow window = callOnJavaFxThread(() -> {
            MainWindow mainWindow = new MainWindow(new Stage(), logic,
                    temporaryFolder.resolve("keep-selection.json"));
            mainWindow.fillInnerParts();
            return mainWindow;
        });
        try {
            callOnJavaFxThread(() -> {
                window.getClientListPanel().getClientListView().getSelectionModel().select(0);
                window.getGigListPanel().getGigListView().getSelectionModel().select(1);

                enterCommand(window, "deletegig 1 1");

                assertSame(selected, window.getGigListPanel().getGigListView()
                        .getSelectionModel().getSelectedItem());
                assertSame(payment, window.getPaymentObligationListPanel().getPaymentListView().getItems().get(0));
                enterCommand(window, "deletegig 1 1");
                assertSame(selected, window.getGigListPanel().getGigListView()
                        .getSelectionModel().getSelectedItem());
                assertSame(payment, model.getPaymentObligationList().get(0));
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
    public void deletePaymentCommand_refreshesSelectedListAndShowsEmptyStateImmediately() throws Exception {
        Model model = new ModelManager();
        model.addClient(ALICE);
        model.addClient(BENSON);
        model.addGig(ALICE_GIG);
        model.addGig(BENSON_GIG);
        model.addPaymentObligation(ALICE_PAYMENT);
        model.addPaymentObligation(ALICE_PAYMENT);
        model.addPaymentObligation(ALICE_PAYMENT.withPaid(true));
        model.addPaymentObligation(BENSON_PAYMENT);
        PaymentObligation first = model.getPaymentObligationList().get(0);
        PaymentObligation paid = model.getPaymentObligationList().get(2);
        Gig selectedGig = model.getGigList().get(0);
        Logic logic = new LogicManager(model, new StorageManager(
                new JsonGigabyteDataStorage(temporaryFolder.resolve("delete-data.json")),
                new JsonUserPrefsStorage(temporaryFolder.resolve("delete-preferences.json"))));
        MainWindow window = callOnJavaFxThread(() -> {
            MainWindow mainWindow = new MainWindow(new Stage(), logic, temporaryFolder.resolve("delete-data.json"));
            mainWindow.fillInnerParts();
            return mainWindow;
        });

        try {
            callOnJavaFxThread(() -> {
                window.getClientListPanel().getClientListView().getSelectionModel().select(0);
                window.getGigListPanel().getGigListView().getSelectionModel().select(0);
                PaymentObligationListPanel panel = window.getPaymentObligationListPanel();

                enterCommand(window, "deletepayment 1 1 2");

                assertEquals(2, panel.getPaymentListView().getItems().size());
                assertSame(first, panel.getPaymentListView().getItems().get(0));
                assertSame(paid, panel.getPaymentListView().getItems().get(1));
                assertEquals("", panel.getEmptyState().getText());

                enterCommand(window, "deletepayment 1 1 2");
                assertEquals(List.of(first), panel.getPaymentListView().getItems());
                enterCommand(window, "deletepayment 1 1 1");

                assertTrue(panel.getPaymentListView().getItems().isEmpty());
                assertEquals("This gig has no payment obligations.", panel.getEmptyState().getText());
                assertSame(selectedGig, window.getGigListPanel().getGigListView()
                        .getSelectionModel().getSelectedItem());
                assertSame(ALICE, window.getClientListPanel().getClientListView()
                        .getSelectionModel().getSelectedItem());
                assertEquals(List.of(BENSON_PAYMENT), model.getPaymentObligationList());

                enterCommand(window, "deletegig 1 1");

                assertTrue(window.getGigListPanel().getGigListView().getItems().isEmpty());
                assertNull(window.getGigListPanel().getGigListView().getSelectionModel().getSelectedItem());
                assertEquals("Select a gig to view its payment obligations.", panel.getEmptyState().getText());
                assertEquals(List.of(BENSON_PAYMENT), model.getPaymentObligationList());
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
    public void deletePaymentCommand_refreshesOutstandingOverviewWithFilteredClientsImmediately() throws Exception {
        Model model = new ModelManager();
        model.addClient(ALICE);
        model.addClient(BENSON);
        model.addGig(ALICE_GIG);
        model.addGig(BENSON_GIG);
        model.addPaymentObligation(ALICE_PAYMENT.withPaid(true));
        model.addPaymentObligation(new PaymentObligation(BENSON_GIG, new Fee("100"),
                new Deadline("2000-01-01"), false));
        model.addPaymentObligation(new PaymentObligation(BENSON_GIG, new Fee("200"),
                new Deadline("2099-01-01"), false));
        PaymentObligation future = model.getPaymentObligationList().get(2);
        Logic logic = new LogicManager(model, new StorageManager(
                new JsonGigabyteDataStorage(temporaryFolder.resolve("delete-outstanding.json")),
                new JsonUserPrefsStorage(temporaryFolder.resolve("delete-outstanding-preferences.json"))));
        MainWindow window = callOnJavaFxThread(() -> {
            MainWindow mainWindow = new MainWindow(new Stage(), logic,
                    temporaryFolder.resolve("delete-outstanding.json"));
            mainWindow.fillInnerParts();
            return mainWindow;
        });

        try {
            callOnJavaFxThread(() -> {
                enterCommand(window, "find Benson");
                enterCommand(window, "outstanding");
                PaymentObligationListPanel panel = window.getPaymentObligationListPanel();
                assertEquals(2, panel.getPaymentListView().getItems().size());

                enterCommand(window, "deletepayment 1 1 1");

                assertEquals(List.of(future), panel.getPaymentListView().getItems());
                assertEquals("", panel.getEmptyState().getText());

                enterCommand(window, "deletepayment 1 1 1");

                assertTrue(panel.getPaymentListView().getItems().isEmpty());
                assertEquals("No outstanding payment obligations.", panel.getEmptyState().getText());
                assertEquals(List.of(BENSON), window.getClientListPanel().getClientListView().getItems());
                assertEquals(List.of(ALICE_PAYMENT.withPaid(true)), model.getPaymentObligationList());
                assertEquals(2, model.getGigList().size());
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
    public void focusingEmptyList_keepsSelectionEmpty() throws Exception {
        LogicStub logic = new LogicStub() {
            @Override
            public ObservableList<Client> getFilteredClientList() {
                return FXCollections.observableArrayList();
            }
        };
        MainWindow mainWindow = callOnJavaFxThread(() -> {
            MainWindow window = new MainWindow(new Stage(), logic, Path.of("data.json"));
            window.show();
            window.fillInnerParts();
            return window;
        });

        try {
            callOnJavaFxThread(() -> {
                TextField commandTextField = (TextField) mainWindow.getRoot().getScene()
                        .lookup("#commandTextField");
                commandTextField.requestFocus();
                pressKey(mainWindow, KeyCode.TAB);

                var clients = mainWindow.getClientListPanel().getClientListView();
                assertTrue(clients.isFocused());
                assertTrue(clients.getItems().isEmpty());
                assertEquals(-1, clients.getSelectionModel().getSelectedIndex());
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
    public void tabOutsideNavigation_doesNotTraverseFocus() throws Exception {
        MainWindow mainWindow = callOnJavaFxThread(() -> {
            MainWindow window = new MainWindow(new Stage(), new LogicStub(), Path.of("data.json"));
            window.show();
            window.fillInnerParts();
            return window;
        });

        try {
            callOnJavaFxThread(() -> {
                TextArea resultDisplay = (TextArea) mainWindow.getRoot().getScene().lookup("#resultDisplay");
                resultDisplay.requestFocus();
                assertTrue(resultDisplay.isFocused());

                pressKey(mainWindow, KeyCode.TAB);
                assertTrue(resultDisplay.isFocused());
                resultDisplay.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED,
                        "", "", KeyCode.TAB, true, false, false, false));
                assertTrue(resultDisplay.isFocused());
                return null;
            });
        } finally {
            callOnJavaFxThread(() -> {
                mainWindow.getPrimaryStage().hide();
                return null;
            });
        }
    }

    private static void pressKey(MainWindow window, KeyCode keyCode) {
        window.getRoot().getScene().getFocusOwner().fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED,
                "", "", keyCode, false, false, false, false));
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
    public void listHeights_scaled1280By720_eachListFitsOneFullRow() throws Exception {
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
                SplitPane splitPane = (SplitPane) mainWindow.getRoot().getScene().lookup(".split-pane");

                // Default split, then the divider dragged fully up and fully down.
                for (double dividerPosition : new double[] {0.5, 0.0, 1.0}) {
                    splitPane.setDividerPositions(dividerPosition);
                    layOutAtScaled1280By720(mainWindow);
                    assertFirstRowFits(mainWindow.getGigListPanel().getGigListView());
                    assertFirstRowFits(mainWindow.getPaymentObligationListPanel().getPaymentListView());
                }
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
    public void resultDisplayHeight_fullHdWindow_keepsFullHeight() throws Exception {
        MainWindow mainWindow = callOnJavaFxThread(() -> {
            MainWindow window = new MainWindow(new Stage(), new LogicStub(), Path.of("data.json"));
            window.show();
            window.fillInnerParts();
            return window;
        });

        try {
            callOnJavaFxThread(() -> {
                layOut(mainWindow, FULL_HD_CONTENT_WIDTH, FULL_HD_CONTENT_HEIGHT);
                Region resultDisplay = (Region) mainWindow.getRoot().getScene().lookup("#resultDisplayPlaceholder");
                assertEquals(100, resultDisplay.getHeight());
                return null;
            });
        } finally {
            callOnJavaFxThread(() -> {
                mainWindow.getPrimaryStage().hide();
                return null;
            });
        }
    }

    /**
     * Lays out the window content at the size it gets on a 1280x720 screen at 150% scale: 853x480 logical,
     * minus the 48px Windows taskbar and the window's title bar and borders.
     */
    private static void layOutAtScaled1280By720(MainWindow window) {
        layOut(window, SCALED_1280_BY_720_CONTENT_WIDTH, SCALED_1280_BY_720_CONTENT_HEIGHT);
    }

    private static void layOut(MainWindow window, double contentWidth, double contentHeight) {
        Region root = (Region) window.getRoot().getScene().getRoot();
        root.resize(contentWidth, contentHeight);
        root.applyCss();
        root.layout();
    }

    private static void assertFirstRowFits(ListView<?> listView) {
        ListCell<?> firstRow = listView.lookupAll(".list-cell").stream()
                .filter(node -> node instanceof ListCell<?> cell && cell.isVisible() && cell.getIndex() == 0)
                .map(node -> (ListCell<?>) node)
                .findFirst()
                .orElseThrow();
        double visibleHeight = listView.getHeight() - listView.snappedTopInset() - listView.snappedBottomInset();
        assertTrue(visibleHeight >= firstRow.getHeight(), listView.getId() + " is " + visibleHeight
                + "px tall but its first row needs " + firstRow.getHeight() + "px");
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
