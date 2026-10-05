package gigabyte.model;

import java.util.function.Predicate;

import gigabyte.commons.core.GuiSettings;
import gigabyte.model.client.Client;
import javafx.collections.ObservableList;

/**
 * The API of the Model component.
 */
public interface Model {
    /** {@code Predicate} that always evaluates to true */
    Predicate<Client> PREDICATE_SHOW_ALL_CLIENTS = unused -> true;

    /**
     * Returns the user prefs.
     */
    ReadOnlyUserPrefs getUserPrefs();

    /**
     * Returns the user prefs' GUI settings.
     */
    GuiSettings getGuiSettings();

    /**
     * Sets the user prefs' GUI settings.
     */
    void setGuiSettings(GuiSettings guiSettings);

    /**
     * Replaces gigabyte data with the data in {@code gigabyteData}.
     */
    void setGigabyteData(ReadOnlyGigabyteData gigabyteData);

    /** Returns the GigabyteData */
    ReadOnlyGigabyteData getGigabyteData();

    /**
     * Returns true if a client with the same identity as {@code client} exists in the client list.
     */
    boolean hasClient(Client client);

    /**
     * Deletes the given client.
     * The client must exist in the client list.
     */
    void deleteClient(Client target);

    /**
     * Adds the given client.
     * {@code client} must not already exist in the client list.
     */
    void addClient(Client client);

    /**
     * Replaces the given client {@code target} with {@code editedClient}.
     * {@code target} must exist in the client list.
     * The client identity of {@code editedClient} must not be the same as another existing client in the client list.
     */
    void setClient(Client target, Client editedClient);

    /** Returns an unmodifiable view of the filtered client list */
    ObservableList<Client> getFilteredClientList();

    /**
     * Updates the filter of the filtered client list to filter by the given {@code predicate}.
     * @throws NullPointerException if {@code predicate} is null.
     */
    void updateFilteredClientList(Predicate<Client> predicate);
}
