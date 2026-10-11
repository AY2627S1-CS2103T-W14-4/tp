package gigabyte.model;

import static gigabyte.model.Model.PREDICATE_SHOW_ALL_CLIENTS;
import static gigabyte.testutil.Assert.assertThrows;
import static gigabyte.testutil.TypicalClients.ALICE;
import static gigabyte.testutil.TypicalClients.BENSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import gigabyte.commons.core.GuiSettings;
import gigabyte.model.client.Client;
import gigabyte.model.client.ClientNameContainsKeywordsPredicate;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.GigStatus;
import gigabyte.model.gig.GigTitle;
import gigabyte.model.gig.exceptions.ClientHasGigsException;
import gigabyte.model.gig.exceptions.GigClientNotFoundException;
import gigabyte.testutil.ClientBuilder;
import gigabyte.testutil.GigabyteBuilder;

public class ModelManagerTest {

    private ModelManager modelManager = new ModelManager();

    @Test
    public void constructor() {
        assertEquals(new UserPrefs(), modelManager.getUserPrefs());
        assertEquals(new GuiSettings(), modelManager.getGuiSettings());
        assertEquals(new GigabyteData(), new GigabyteData(modelManager.getGigabyteData()));
    }

    @Test
    public void constructor_validUserPrefs_copiesUserPrefs() {
        UserPrefs userPrefs = new UserPrefs();
        userPrefs.setGuiSettings(new GuiSettings(1, 2, 3, 4));
        modelManager = new ModelManager(new GigabyteData(), userPrefs);
        assertEquals(userPrefs, modelManager.getUserPrefs());

        // Modifying userPrefs should not modify modelManager's userPrefs
        UserPrefs oldUserPrefs = new UserPrefs(userPrefs);
        userPrefs.setGuiSettings(new GuiSettings(5, 6, 7, 8));
        assertEquals(oldUserPrefs, modelManager.getUserPrefs());
    }

    @Test
    public void setGuiSettings_nullGuiSettings_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> modelManager.setGuiSettings(null));
    }

    @Test
    public void setGuiSettings_validGuiSettings_setsGuiSettings() {
        GuiSettings guiSettings = new GuiSettings(1, 2, 3, 4);
        modelManager.setGuiSettings(guiSettings);
        assertEquals(guiSettings, modelManager.getGuiSettings());
    }

    @Test
    public void hasClient_nullClient_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> modelManager.hasClient(null));
    }

    @Test
    public void hasClient_clientNotInGigabyteData_returnsFalse() {
        assertFalse(modelManager.hasClient(ALICE));
    }

    @Test
    public void hasClient_clientInGigabyteData_returnsTrue() {
        modelManager.addClient(ALICE);
        assertTrue(modelManager.hasClient(ALICE));
    }

    @Test
    public void getFilteredClientList_modifyList_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> modelManager.getFilteredClientList().remove(0));
    }

    @Test
    public void equals() {
        GigabyteData gigabyteData = new GigabyteBuilder().withClient(ALICE).withClient(BENSON).build();
        GigabyteData differentGigabyteData = new GigabyteData();
        UserPrefs userPrefs = new UserPrefs();

        // same values -> returns true
        modelManager = new ModelManager(gigabyteData, userPrefs);
        ModelManager modelManagerCopy = new ModelManager(gigabyteData, userPrefs);
        assertTrue(modelManager.equals(modelManagerCopy));

        // same object -> returns true
        assertTrue(modelManager.equals(modelManager));

        // null -> returns false
        assertFalse(modelManager.equals(null));

        // different types -> returns false
        assertFalse(modelManager.equals(5));

        // different gigabyteData -> returns false
        assertFalse(modelManager.equals(new ModelManager(differentGigabyteData, userPrefs)));

        // different filteredList -> returns false
        String[] keywords = ALICE.getName().fullName.split("\\s+");
        modelManager.updateFilteredClientList(new ClientNameContainsKeywordsPredicate(List.of(keywords)));
        assertFalse(modelManager.equals(new ModelManager(gigabyteData, userPrefs)));

        // resets modelManager to initial state for upcoming tests
        modelManager.updateFilteredClientList(PREDICATE_SHOW_ALL_CLIENTS);

        // different userPrefs -> returns false
        UserPrefs differentUserPrefs = new UserPrefs();
        differentUserPrefs.setGuiSettings(new GuiSettings(1, 2, 3, 4));
        assertFalse(modelManager.equals(new ModelManager(gigabyteData, differentUserPrefs)));
    }

    @Test
    public void addGig_existingClient_addsGig() {
        modelManager.addClient(ALICE);
        Gig gig = createGig(ALICE);

        modelManager.addGig(gig);

        assertEquals(List.of(gig), modelManager.getGigList());
    }

    @Test
    public void addGig_nonexistentClient_throwsGigClientNotFoundException() {
        Gig gig = createGig(ALICE);

        assertThrows(GigClientNotFoundException.class,
                GigClientNotFoundException.MESSAGE, () -> modelManager.addGig(gig));
    }

    @Test
    public void addGig_equivalentClient_usesStoredClientReference() {
        modelManager.addClient(ALICE);
        Client equivalentAlice = new ClientBuilder(ALICE)
                .withAddress("Different address")
                .build();

        modelManager.addGig(createGig(equivalentAlice));

        assertSame(ALICE, modelManager.getGigList().get(0).getClient());
    }

    @Test
    public void getGigList_modifyList_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () ->
                modelManager.getGigList().add(createGig(ALICE)));
    }

    @Test
    public void deleteClient_clientHasGig_throwsClientHasGigsException() {
        modelManager.addClient(ALICE);
        modelManager.addGig(createGig(ALICE));

        assertThrows(ClientHasGigsException.class,
                ClientHasGigsException.MESSAGE, () -> modelManager.deleteClient(ALICE));
        assertTrue(modelManager.hasClient(ALICE));
    }

    @Test
    public void setClient_clientHasGig_updatesGigClientReference() {
        modelManager.addClient(ALICE);
        modelManager.addGig(createGig(ALICE));
        Client editedAlice = new ClientBuilder(ALICE)
                .withAddress("Updated address")
                .build();

        modelManager.setClient(ALICE, editedAlice);

        assertSame(editedAlice, modelManager.getGigList().get(0).getClient());
    }

    private Gig createGig(Client client) {
        return new Gig(client, new GigTitle("Website redesign"), GigStatus.NOT_STARTED,
                new Deadline("2027-01-31"), new Fee("1250.00"));
    }
}
