package gigabyte.ui;

import static gigabyte.testutil.TypicalClients.ALICE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.collections.FXCollections;

public class ClientListPanelTest {
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
}
