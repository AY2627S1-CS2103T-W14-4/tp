package gigabyte.ui;

import java.awt.Point;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import gigabyte.commons.core.GuiSettings;
import gigabyte.commons.core.LogsCenter;
import gigabyte.logic.Logic;
import gigabyte.logic.commands.CommandResult;
import gigabyte.logic.commands.exceptions.CommandException;
import gigabyte.logic.parser.exceptions.ParseException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * The Main Window. Provides the basic application layout containing
 * a menu bar and space where other JavaFX elements can be placed.
 */
public class MainWindow extends UiPart<Stage> {

    private static final String FXML = "MainWindow.fxml";

    private final Logger logger = LogsCenter.getLogger(getClass());

    private Stage primaryStage;
    private Logic logic;
    private Path dataFilePath;

    // Independent Ui parts residing in this Ui container
    private ClientListPanel clientListPanel;
    private GigListPanel gigListPanel;
    private PaymentObligationListPanel paymentObligationListPanel;
    private ResultDisplay resultDisplay;
    private HelpWindow helpWindow;

    @FXML
    private StackPane commandBoxPlaceholder;

    @FXML
    private MenuItem helpMenuItem;

    @FXML
    private StackPane clientListPanelPlaceholder;

    @FXML
    private StackPane gigListPanelPlaceholder;

    @FXML
    private StackPane paymentObligationListPanelPlaceholder;

    @FXML
    private StackPane resultDisplayPlaceholder;

    @FXML
    private StackPane statusbarPlaceholder;

    /**
     * Creates a {@code MainWindow} with the given {@code Stage}, {@code Logic},
     * and the data file path to show in the status bar.
     */
    public MainWindow(Stage primaryStage, Logic logic, Path dataFilePath) {
        super(FXML, primaryStage);

        // Set dependencies
        this.primaryStage = primaryStage;
        this.logic = logic;
        this.dataFilePath = dataFilePath;

        // Configure the UI
        setWindowDefaultSize(logic.getGuiSettings());

        setAccelerators();

        helpWindow = new HelpWindow();
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    private void setAccelerators() {
        setAccelerator(helpMenuItem, KeyCombination.valueOf("F1"));
    }

    /**
     * Sets the accelerator of a MenuItem.
     * @param keyCombination the KeyCombination value of the accelerator
     */
    private void setAccelerator(MenuItem menuItem, KeyCombination keyCombination) {
        menuItem.setAccelerator(keyCombination);

        /*
         * TODO: the code below can be removed once the bug reported here
         * https://bugs.openjdk.java.net/browse/JDK-8131666
         * is fixed in a later version of the SDK.
         *
         * According to the bug report, TextInputControl (TextField, TextArea) will
         * consume function-key events. Because CommandBox contains a TextField and
         * ResultDisplay contains a TextArea, some accelerators (e.g., F1) will
         * not work when the focus is in them because the key event is consumed by
         * the TextInputControl(s).
         *
         * For now, we add the following event filter to capture such key events and open
         * the help window purposely so as to support accelerators even when focus is
         * in CommandBox or ResultDisplay.
         */
        getRoot().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getTarget() instanceof TextInputControl && keyCombination.match(event)) {
                menuItem.getOnAction().handle(new ActionEvent());
                event.consume();
            }
        });
    }

    /**
     * Fills up all the placeholders of this window.
     */
    void fillInnerParts() {
        CommandBox commandBox = new CommandBox(this::executeCommand);
        commandBoxPlaceholder.getChildren().add(commandBox.getRoot());

        paymentObligationListPanel = new PaymentObligationListPanel(logic.getPaymentObligationList());
        gigListPanel = new GigListPanel(logic.getGigList(), paymentObligationListPanel::showPaymentsFor);
        clientListPanel = new ClientListPanel(logic.getFilteredClientList(), client -> {
            gigListPanel.showGigsFor(client);
            paymentObligationListPanel.showPaymentsFor(null);
        });
        clientListPanelPlaceholder.getChildren().add(clientListPanel.getRoot());
        gigListPanelPlaceholder.getChildren().add(gigListPanel.getRoot());
        paymentObligationListPanelPlaceholder.getChildren().add(paymentObligationListPanel.getRoot());

        resultDisplay = new ResultDisplay();
        resultDisplayPlaceholder.getChildren().add(resultDisplay.getRoot());

        StatusBarFooter statusBarFooter = new StatusBarFooter(dataFilePath);
        statusbarPlaceholder.getChildren().add(statusBarFooter.getRoot());

        ListNavigationController navigation = new ListNavigationController(
                clientListPanel.getClientListView(), gigListPanel.getGigListView(),
                paymentObligationListPanel.getPaymentListView(), commandBox);
        navigation.install(primaryStage.getScene());
    }

    /**
     * Sets the default size based on {@code guiSettings}.
     */
    private void setWindowDefaultSize(GuiSettings guiSettings) {
        Rectangle2D visualBounds = getTargetVisualBounds(guiSettings);

        primaryStage.setMinHeight(
                Math.min(primaryStage.getMinHeight(), visualBounds.getHeight()));
        primaryStage.setMinWidth(
                Math.min(primaryStage.getMinWidth(), visualBounds.getWidth()));

        double windowHeight = clampWindowDimension(
                guiSettings.getWindowHeight(), primaryStage.getMinHeight(), visualBounds.getHeight());
        double windowWidth = clampWindowDimension(
                guiSettings.getWindowWidth(), primaryStage.getMinWidth(), visualBounds.getWidth());
        primaryStage.setHeight(windowHeight);
        primaryStage.setWidth(windowWidth);

        Point windowCoordinates = guiSettings.getWindowCoordinates();
        if (windowCoordinates != null) {
            primaryStage.setX(clampWindowCoordinate(windowCoordinates.getX(),
                    visualBounds.getMinX(), visualBounds.getMaxX() - windowWidth));
            primaryStage.setY(clampWindowCoordinate(windowCoordinates.getY(),
                    visualBounds.getMinY(), visualBounds.getMaxY() - windowHeight));
        }
    }

    /** Returns a visual screen area that contains the saved window position, or the primary screen area. */
    private Rectangle2D getTargetVisualBounds(GuiSettings guiSettings) {
        Point windowCoordinates = guiSettings.getWindowCoordinates();
        if (windowCoordinates != null) {
            List<Screen> matchingScreens = Screen.getScreensForRectangle(
                    windowCoordinates.getX(), windowCoordinates.getY(), 1, 1);
            if (!matchingScreens.isEmpty()) {
                return matchingScreens.get(0).getVisualBounds();
            }
        }
        return Screen.getPrimary().getVisualBounds();
    }

    /** Clamps a requested window dimension between the supported minimum and available screen dimension. */
    static double clampWindowDimension(double requestedSize, double minimumSize, double availableSize) {
        double effectiveMinimum = Math.min(minimumSize, availableSize);
        return Math.max(effectiveMinimum, Math.min(requestedSize, availableSize));
    }

    /** Clamps a saved window coordinate within the available screen-axis range. */
    private static double clampWindowCoordinate(double requestedCoordinate,
                                                double minimumCoordinate,
                                                double maximumCoordinate) {
        return Math.max(minimumCoordinate, Math.min(requestedCoordinate, maximumCoordinate));
    }

    /**
     * Opens the help window or focuses on it if it's already opened.
     */
    @FXML
    public void handleHelp() {
        if (!helpWindow.isShowing()) {
            helpWindow.show();
        } else {
            helpWindow.focus();
        }
    }

    void show() {
        primaryStage.show();
    }

    /**
     * Closes the application.
     */
    @FXML
    private void handleExit() {
        GuiSettings guiSettings = new GuiSettings(primaryStage.getWidth(), primaryStage.getHeight(),
                (int) primaryStage.getX(), (int) primaryStage.getY());
        logic.setGuiSettings(guiSettings);
        helpWindow.hide();
        primaryStage.hide();
    }

    public ClientListPanel getClientListPanel() {
        return clientListPanel;
    }

    GigListPanel getGigListPanel() {
        return gigListPanel;
    }

    PaymentObligationListPanel getPaymentObligationListPanel() {
        return paymentObligationListPanel;
    }

    /**
     * Executes the command and returns the result.
     *
     * @see Logic#execute(String)
     */
    private CommandResult executeCommand(String commandText) throws CommandException, ParseException {
        try {
            CommandResult commandResult = logic.execute(commandText);
            logger.info("Result: " + commandResult.getFeedbackToUser());
            resultDisplay.setFeedbackToUser(commandResult.getFeedbackToUser());
            paymentObligationListPanel.refreshPaymentStates();
            showRequestedView(commandResult);

            if (commandResult.shouldShowHelp()) {
                handleHelp();
            }

            if (commandResult.shouldExit()) {
                handleExit();
            }

            return commandResult;
        } catch (CommandException | ParseException e) {
            logger.info("An error occurred while executing command: " + commandText);
            resultDisplay.setFeedbackToUser(e.getMessage());
            throw e;
        }
    }

    /**
     * Switches the client, gig, and payment lists to the view requested by {@code commandResult}, if any.
     */
    private void showRequestedView(CommandResult commandResult) {
        if (commandResult.getGigStatusFilter() != null) {
            // Clear the selection so clicking the same client restores their gigs too.
            clientListPanel.getClientListView().getSelectionModel().clearSelection();
            gigListPanel.showGigsWithStatus(commandResult.getGigStatusFilter());
            paymentObligationListPanel.showPaymentsFor(null);
        }

        if (commandResult.getGigTitleSearch() != null) {
            clientListPanel.getClientListView().getSelectionModel().clearSelection();
            gigListPanel.showGigsMatchingTitle(commandResult.getGigTitleSearch());
            paymentObligationListPanel.showPaymentsFor(null);
        }

        if (commandResult.shouldShowOutstandingPayments()) {
            clientListPanel.getClientListView().getSelectionModel().clearSelection();
            gigListPanel.getGigListView().getSelectionModel().clearSelection();
            paymentObligationListPanel.showOutstandingPayments();
        }

        if (commandResult.shouldShowDeadlineOverview()) {
            clientListPanel.getClientListView().getSelectionModel().clearSelection();
            gigListPanel.showDeadlineOverview();
            paymentObligationListPanel.showPaymentsFor(null);
        }
    }
}
