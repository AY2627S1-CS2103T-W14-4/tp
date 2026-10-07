package gigabyte.ui;

import java.util.logging.Logger;
import java.util.function.Consumer;

import gigabyte.commons.core.LogsCenter;
import gigabyte.model.client.Client;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Region;

/**
 * Panel containing the list of clients.
 */
public class ClientListPanel extends UiPart<Region> {
    private static final String FXML = "ClientListPanel.fxml";
    private final Logger logger = LogsCenter.getLogger(ClientListPanel.class);

    @FXML
    private ListView<Client> clientListView;
    private final Consumer<Client> clientSelectionHandler;

    /**
     * Creates a {@code ClientListPanel} with the given {@code ObservableList}.
     */
    public ClientListPanel(ObservableList<Client> clientList) {
        this(clientList, client -> { });
    }

    public ClientListPanel(ObservableList<Client> clientList, Consumer<Client> clientSelectionHandler) {
        super(FXML);
        this.clientSelectionHandler = clientSelectionHandler;
        clientListView.setItems(clientList);
        clientListView.setCellFactory(listView -> new ClientListViewCell());
        clientListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldClient, newClient) -> clientSelectionHandler.accept(newClient));
    }

    /**
     * Custom {@code ListCell} that displays the graphics of a {@code Client} using a {@code ClientCard}.
     */
    class ClientListViewCell extends ListCell<Client> {
        @Override
        protected void updateItem(Client client, boolean empty) {
            super.updateItem(client, empty);

            if (empty || client == null) {
                setGraphic(null);
                setText(null);
            } else {
                setGraphic(new ClientCard(client, getIndex() + 1).getRoot());
            }
        }
    }

}
