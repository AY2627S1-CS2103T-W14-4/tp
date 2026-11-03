package gigabyte.ui;

import static gigabyte.testutil.TypicalClients.ALICE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gigabyte.model.client.Client;
import gigabyte.testutil.ClientBuilder;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollBar;
import javafx.scene.text.Text;

public class ClientListPanelTest {
    /** The client list's minimum width in MainWindow.fxml. */
    private static final double MIN_PANEL_WIDTH = 340;

    @BeforeAll
    public static void setUpJavaFx() {
        JavaFxTestUtil.startToolkit();
    }

    @Test
    public void selectingClient_invokesSelectionHandler() {
        AtomicReference<Object> selected = new AtomicReference<>();
        ClientListPanel panel = new ClientListPanel(FXCollections.observableArrayList(ALICE), selected::set);

        panel.getClientListView().getSelectionModel().select(0);

        assertEquals(ALICE, selected.get());
    }

    @Test
    public void defaultConstructor_acceptsClientList() {
        ClientListPanel panel = new ClientListPanel(FXCollections.observableArrayList(ALICE));

        assertEquals(1, panel.getClientListView().getItems().size());
    }

    @Test
    public void clientCard_minimumPanelWidthWithLongDetails_showsAllTextInFull() {
        Client client = new ClientBuilder()
                .withName("Alexandra Catherine Wellington Montgomery Fitzgerald")
                .withPhone("+65 9123 4567")
                .withEmail("alexandra.catherine.wellington@longcompanyname.com.sg")
                .withAddress("Blk 123 Ang Mo Kio Avenue 6, #12-345, Singapore 560123")
                .build();
        ClientListPanel panel = new ClientListPanel(FXCollections.observableArrayList(client));
        Scene scene = new Scene(panel.getRoot(), MIN_PANEL_WIDTH, 300);
        scene.getStylesheets().add(getClass().getResource("/view/DarkTheme.css").toExternalForm());
        panel.getRoot().applyCss();
        panel.getRoot().layout();

        @SuppressWarnings("unchecked")
        ListCell<Client> cell = (ListCell<Client>) panel.getClientListView().lookup(".list-cell");
        for (String labelId : new String[] {"#name", "#phone", "#address", "#email"}) {
            Label label = (Label) cell.getGraphic().lookup(labelId);
            Text shownText = (Text) label.lookup(".text");
            assertEquals(label.getText(), shownText.getText(), labelId + " is cut off");
        }
        ScrollBar horizontalScrollBar = (ScrollBar) panel.getClientListView().lookup(".scroll-bar:horizontal");
        assertTrue(cell.getWidth() <= panel.getClientListView().getWidth());
        assertFalse(horizontalScrollBar.isVisible());
    }
}
