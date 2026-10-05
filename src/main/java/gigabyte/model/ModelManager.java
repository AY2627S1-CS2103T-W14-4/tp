package gigabyte.model;

import static gigabyte.commons.util.CollectionUtil.requireAllNonNull;
import static java.util.Objects.requireNonNull;

import java.util.function.Predicate;
import java.util.logging.Logger;

import gigabyte.commons.core.GuiSettings;
import gigabyte.commons.core.LogsCenter;
import gigabyte.model.client.Client;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

/**
 * Represents the in-memory model of the client list data.
 */
public class ModelManager implements Model {
    private static final Logger logger = LogsCenter.getLogger(ModelManager.class);

    private final GigabyteData gigabyteData;
    private final UserPrefs userPrefs;
    private final FilteredList<Client> filteredClients;

    /**
     * Initializes a ModelManager with the given gigabyteData and userPrefs.
     */
    public ModelManager(ReadOnlyGigabyteData gigabyteData, ReadOnlyUserPrefs userPrefs) {
        requireAllNonNull(gigabyteData, userPrefs);

        logger.fine("Initializing with client list: " + gigabyteData + " and user prefs " + userPrefs);

        this.gigabyteData = new GigabyteData(gigabyteData);
        this.userPrefs = new UserPrefs(userPrefs);
        filteredClients = new FilteredList<>(this.gigabyteData.getClientList());
    }

    public ModelManager() {
        this(new GigabyteData(), new UserPrefs());
    }

    //=========== UserPrefs ==================================================================================

    @Override
    public ReadOnlyUserPrefs getUserPrefs() {
        return userPrefs;
    }

    @Override
    public GuiSettings getGuiSettings() {
        return userPrefs.getGuiSettings();
    }

    @Override
    public void setGuiSettings(GuiSettings guiSettings) {
        requireNonNull(guiSettings);
        userPrefs.setGuiSettings(guiSettings);
    }

    //=========== GigabyteData ================================================================================

    @Override
    public void setGigabyteData(ReadOnlyGigabyteData gigabyteData) {
        this.gigabyteData.resetData(gigabyteData);
    }

    @Override
    public ReadOnlyGigabyteData getGigabyteData() {
        return gigabyteData;
    }

    @Override
    public boolean hasClient(Client client) {
        requireNonNull(client);
        return gigabyteData.hasClient(client);
    }

    @Override
    public void deleteClient(Client target) {
        gigabyteData.removeClient(target);
    }

    @Override
    public void addClient(Client client) {
        gigabyteData.addClient(client);
        updateFilteredClientList(PREDICATE_SHOW_ALL_CLIENTS);
    }

    @Override
    public void setClient(Client target, Client editedClient) {
        requireAllNonNull(target, editedClient);

        gigabyteData.setClient(target, editedClient);
    }

    //=========== Filtered Client List Accessors =============================================================

    /**
     * Returns an unmodifiable view of the list of {@code Client} backed by the internal list of
     * {@code gigabyteData}
     */
    @Override
    public ObservableList<Client> getFilteredClientList() {
        return filteredClients;
    }

    @Override
    public void updateFilteredClientList(Predicate<Client> predicate) {
        requireNonNull(predicate);
        filteredClients.setPredicate(predicate);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof ModelManager otherModelManager)) {
            return false;
        }

        return gigabyteData.equals(otherModelManager.gigabyteData)
                && userPrefs.equals(otherModelManager.userPrefs)
                && filteredClients.equals(otherModelManager.filteredClients);
    }

}
