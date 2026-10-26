package gigabyte.logic.commands;

import static gigabyte.logic.commands.CommandTestUtil.DESC_AMY;
import static gigabyte.logic.commands.CommandTestUtil.DESC_BOB;
import static gigabyte.logic.commands.CommandTestUtil.VALID_NAME_BOB;
import static gigabyte.logic.commands.CommandTestUtil.VALID_PHONE_BOB;
import static gigabyte.logic.commands.CommandTestUtil.VALID_TAG_HUSBAND;
import static gigabyte.logic.commands.CommandTestUtil.assertCommandFailure;
import static gigabyte.logic.commands.CommandTestUtil.assertCommandSuccess;
import static gigabyte.logic.commands.CommandTestUtil.showClientAtIndex;
import static gigabyte.testutil.TypicalClients.getTypicalGigabyteData;
import static gigabyte.testutil.TypicalIndexes.INDEX_FIRST_CLIENT;
import static gigabyte.testutil.TypicalIndexes.INDEX_SECOND_CLIENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import gigabyte.commons.core.index.Index;
import gigabyte.logic.Messages;
import gigabyte.logic.commands.EditClientCommand.EditClientDescriptor;
import gigabyte.model.GigabyteData;
import gigabyte.model.Model;
import gigabyte.model.ModelManager;
import gigabyte.model.UserPrefs;
import gigabyte.model.client.Client;
import gigabyte.testutil.ClientBuilder;
import gigabyte.testutil.EditClientDescriptorBuilder;

/**
 * Contains integration tests (interaction with the Model) and unit tests for EditClientCommand.
 */
public class EditClientCommandTest {

    private Model model = new ModelManager(getTypicalGigabyteData(), new UserPrefs());

    @Test
    public void execute_allFieldsSpecifiedUnfilteredList_success() {
        Client clientToEdit = model.getFilteredClientList().get(INDEX_FIRST_CLIENT.getZeroBased());
        Client editedClient = new ClientBuilder(clientToEdit)
                .withName("Edited Client")
                .withPhone("81234567")
                .withEmail("edited@example.com")
                .withAddress("123 Edited Road")
                .withTags(VALID_TAG_HUSBAND)
                .build();
        EditClientCommand.EditClientDescriptor descriptor = new EditClientDescriptorBuilder(editedClient).build();
        EditClientCommand editCommand = new EditClientCommand(INDEX_FIRST_CLIENT, descriptor);

        String expectedMessage = String.format(EditClientCommand.MESSAGE_EDIT_CLIENT_SUCCESS,
                Messages.format(editedClient));

        Model expectedModel = new ModelManager(new GigabyteData(model.getGigabyteData()), new UserPrefs());
        expectedModel.setClient(clientToEdit, editedClient);

        assertCommandSuccess(editCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_someFieldsSpecifiedUnfilteredList_success() {
        Index indexLastClient = Index.fromOneBased(model.getFilteredClientList().size());
        Client lastClient = model.getFilteredClientList().get(indexLastClient.getZeroBased());

        ClientBuilder clientInList = new ClientBuilder(lastClient);
        Client editedClient = clientInList.withName(VALID_NAME_BOB).withPhone(VALID_PHONE_BOB)
                .withTags(VALID_TAG_HUSBAND).build();

        EditClientDescriptor descriptor = new EditClientDescriptorBuilder().withName(VALID_NAME_BOB)
                .withPhone(VALID_PHONE_BOB).withTags(VALID_TAG_HUSBAND).build();
        EditClientCommand editCommand = new EditClientCommand(indexLastClient, descriptor);

        String expectedMessage = String.format(EditClientCommand.MESSAGE_EDIT_CLIENT_SUCCESS,
                Messages.format(editedClient));

        Model expectedModel = new ModelManager(new GigabyteData(model.getGigabyteData()), new UserPrefs());
        expectedModel.setClient(lastClient, editedClient);

        assertCommandSuccess(editCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_noFieldSpecifiedUnfilteredList_success() {
        EditClientCommand editCommand = new EditClientCommand(INDEX_FIRST_CLIENT, new EditClientDescriptor());
        Client editedClient = model.getFilteredClientList().get(INDEX_FIRST_CLIENT.getZeroBased());

        String expectedMessage = String.format(EditClientCommand.MESSAGE_EDIT_CLIENT_SUCCESS,
                Messages.format(editedClient));

        Model expectedModel = new ModelManager(new GigabyteData(model.getGigabyteData()), new UserPrefs());

        assertCommandSuccess(editCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_filteredList_success() {
        showClientAtIndex(model, INDEX_FIRST_CLIENT);

        Client clientInFilteredList = model.getFilteredClientList().get(INDEX_FIRST_CLIENT.getZeroBased());
        Client editedClient = new ClientBuilder(clientInFilteredList).withName(VALID_NAME_BOB).build();
        EditClientCommand editCommand = new EditClientCommand(INDEX_FIRST_CLIENT,
                new EditClientDescriptorBuilder().withName(VALID_NAME_BOB).build());

        String expectedMessage = String.format(EditClientCommand.MESSAGE_EDIT_CLIENT_SUCCESS,
                Messages.format(editedClient));

        Model expectedModel = new ModelManager(new GigabyteData(model.getGigabyteData()), new UserPrefs());
        expectedModel.setClient(model.getFilteredClientList().get(0), editedClient);

        assertCommandSuccess(editCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_duplicateClientUnfilteredList_failure() {
        Client firstClient = model.getFilteredClientList().get(INDEX_FIRST_CLIENT.getZeroBased());
        EditClientDescriptor descriptor = new EditClientDescriptorBuilder(firstClient).build();
        EditClientCommand editCommand = new EditClientCommand(INDEX_SECOND_CLIENT, descriptor);

        assertCommandFailure(editCommand, model, EditClientCommand.MESSAGE_DUPLICATE_CLIENT);
    }

    @Test
    public void execute_duplicateClientFilteredList_failure() {
        showClientAtIndex(model, INDEX_FIRST_CLIENT);

        // edit client in filtered list into a duplicate in client list
        Client clientInList = model.getGigabyteData().getClientList().get(INDEX_SECOND_CLIENT.getZeroBased());
        EditClientCommand editCommand = new EditClientCommand(INDEX_FIRST_CLIENT,
                new EditClientDescriptorBuilder(clientInList).build());

        assertCommandFailure(editCommand, model, EditClientCommand.MESSAGE_DUPLICATE_CLIENT);
    }

    @Test
    public void execute_renameToOtherClientNameDifferingInCaseOrSpacing_failure() {
        // EP: target name differs from ALICE's only in case and spacing
        EditClientCommand editCommand = new EditClientCommand(INDEX_SECOND_CLIENT,
                new EditClientDescriptorBuilder().withName("alice  PAULINE").build());

        assertCommandFailure(editCommand, model, Messages.MESSAGE_DUPLICATE_CLIENT_NAME);
    }

    @Test
    public void execute_ownNameCaseOrSpacingOnlyChange_success() {
        Client clientToEdit = model.getFilteredClientList().get(INDEX_FIRST_CLIENT.getZeroBased());
        Client editedClient = new ClientBuilder(clientToEdit).withName("ALICE  pauline").build();
        EditClientCommand editCommand = new EditClientCommand(INDEX_FIRST_CLIENT,
                new EditClientDescriptorBuilder().withName("ALICE  pauline").build());

        Model expectedModel = new ModelManager(new GigabyteData(model.getGigabyteData()), new UserPrefs());
        expectedModel.setClient(clientToEdit, editedClient);

        assertCommandSuccess(editCommand, model, String.format(EditClientCommand.MESSAGE_EDIT_CLIENT_SUCCESS,
                Messages.format(editedClient)), expectedModel);
    }

    @Test
    public void execute_samePhoneOrEmailAsOtherClient_successWithNote() {
        Client clientToEdit = model.getFilteredClientList().get(INDEX_FIRST_CLIENT.getZeroBased());
        Client benson = model.getFilteredClientList().get(INDEX_SECOND_CLIENT.getZeroBased());
        Client editedClient = new ClientBuilder(clientToEdit).withPhone(benson.getPhone().value)
                .withEmail(benson.getEmail().value).build();
        EditClientCommand editCommand = new EditClientCommand(INDEX_FIRST_CLIENT,
                new EditClientDescriptorBuilder().withPhone(benson.getPhone().value)
                        .withEmail(benson.getEmail().value).build());

        Model expectedModel = new ModelManager(new GigabyteData(model.getGigabyteData()), new UserPrefs());
        expectedModel.setClient(clientToEdit, editedClient);

        assertCommandSuccess(editCommand, model, String.format(EditClientCommand.MESSAGE_EDIT_CLIENT_SUCCESS,
                Messages.format(editedClient)) + "\nNote: Benson Meier has the same phone number and email.",
                expectedModel);
    }

    @Test
    public void execute_unchangedPhoneAndEmail_noNoteAboutItself() {
        Client clientToEdit = model.getFilteredClientList().get(INDEX_FIRST_CLIENT.getZeroBased());
        Client editedClient = new ClientBuilder(clientToEdit).withAddress("1 New Road").build();
        EditClientCommand editCommand = new EditClientCommand(INDEX_FIRST_CLIENT,
                new EditClientDescriptorBuilder().withAddress("1 New Road").build());

        Model expectedModel = new ModelManager(new GigabyteData(model.getGigabyteData()), new UserPrefs());
        expectedModel.setClient(clientToEdit, editedClient);

        assertCommandSuccess(editCommand, model, String.format(EditClientCommand.MESSAGE_EDIT_CLIENT_SUCCESS,
                Messages.format(editedClient)), expectedModel);
    }

    @Test
    public void execute_legacyCaseVariantPair_otherFieldsAndCaseOnlyRenamesEditable() throws Exception {
        Client johnDoe = new ClientBuilder().withName("John Doe").withPhone("90000001")
                .withEmail("john1@example.com").build();
        Client lowerCaseJohnDoe = new ClientBuilder().withName("john doe").withPhone("90000002")
                .withEmail("john2@example.com").build();
        GigabyteData legacyData = new GigabyteData();
        legacyData.addClient(johnDoe);
        legacyData.addClient(lowerCaseJohnDoe);
        Model legacyModel = new ModelManager(legacyData, new UserPrefs());

        // EP: phone, email, address or tag only edit, for both clients of the pair
        for (Index index : new Index[] {INDEX_FIRST_CLIENT, INDEX_SECOND_CLIENT}) {
            new EditClientCommand(index, new EditClientDescriptorBuilder().withPhone("8000000"
                    + index.getOneBased()).build()).execute(legacyModel);
            new EditClientCommand(index, new EditClientDescriptorBuilder().withEmail("new"
                    + index.getOneBased() + "@example.com").build()).execute(legacyModel);
            new EditClientCommand(index, new EditClientDescriptorBuilder().withAddress("New Road").build())
                    .execute(legacyModel);
            new EditClientCommand(index, new EditClientDescriptorBuilder().withTags(VALID_TAG_HUSBAND).build())
                    .execute(legacyModel);
        }

        // EP: capitalisation-only and spacing-only rename of own name
        new EditClientCommand(INDEX_FIRST_CLIENT, new EditClientDescriptorBuilder().withName("JOHN DOE").build())
                .execute(legacyModel);
        new EditClientCommand(INDEX_FIRST_CLIENT, new EditClientDescriptorBuilder().withName("John  Doe").build())
                .execute(legacyModel);
        assertEquals("John  Doe", legacyModel.getFilteredClientList().get(0).getName().fullName);
        assertEquals(2, legacyModel.getFilteredClientList().size());

        // EP: rename to exactly the other client's name -> rejected by the exact-name check
        assertCommandFailure(new EditClientCommand(INDEX_FIRST_CLIENT,
                new EditClientDescriptorBuilder().withName("john doe").build()), legacyModel,
                EditClientCommand.MESSAGE_DUPLICATE_CLIENT);
    }

    @Test
    public void execute_invalidClientIndexUnfilteredList_failure() {
        Index outOfBoundIndex = Index.fromOneBased(model.getFilteredClientList().size() + 1);
        EditClientDescriptor descriptor = new EditClientDescriptorBuilder().withName(VALID_NAME_BOB).build();
        EditClientCommand editCommand = new EditClientCommand(outOfBoundIndex, descriptor);

        assertCommandFailure(editCommand, model, Messages.MESSAGE_INVALID_CLIENT_DISPLAYED_INDEX);
    }

    /**
     * Edit filtered list where index is larger than size of filtered list,
     * but smaller than size of client list
     */
    @Test
    public void execute_invalidClientIndexFilteredList_failure() {
        showClientAtIndex(model, INDEX_FIRST_CLIENT);
        Index outOfBoundIndex = INDEX_SECOND_CLIENT;
        // ensures that outOfBoundIndex is still in bounds of client list list
        assertTrue(outOfBoundIndex.getZeroBased() < model.getGigabyteData().getClientList().size());

        EditClientCommand editCommand = new EditClientCommand(outOfBoundIndex,
                new EditClientDescriptorBuilder().withName(VALID_NAME_BOB).build());

        assertCommandFailure(editCommand, model, Messages.MESSAGE_INVALID_CLIENT_DISPLAYED_INDEX);
    }

    @Test
    public void equals() {
        final EditClientCommand standardCommand = new EditClientCommand(INDEX_FIRST_CLIENT, DESC_AMY);

        // same values -> returns true
        EditClientCommand.EditClientDescriptor copyDescriptor = new EditClientCommand.EditClientDescriptor(DESC_AMY);
        EditClientCommand commandWithSameValues = new EditClientCommand(INDEX_FIRST_CLIENT, copyDescriptor);
        assertTrue(standardCommand.equals(commandWithSameValues));

        // same object -> returns true
        assertTrue(standardCommand.equals(standardCommand));

        // null -> returns false
        assertFalse(standardCommand.equals(null));

        // different types -> returns false
        assertFalse(standardCommand.equals(new ClearClientsCommand()));

        // different index -> returns false
        assertFalse(standardCommand.equals(new EditClientCommand(INDEX_SECOND_CLIENT, DESC_AMY)));

        // different descriptor -> returns false
        assertFalse(standardCommand.equals(new EditClientCommand(INDEX_FIRST_CLIENT, DESC_BOB)));
    }

    @Test
    public void toStringMethod() {
        Index index = Index.fromOneBased(1);
        EditClientCommand.EditClientDescriptor editClientDescriptor = new EditClientDescriptor();
        EditClientCommand editCommand = new EditClientCommand(index, editClientDescriptor);
        String expected = EditClientCommand.class.getCanonicalName() + "{index=" + index + ", editClientDescriptor="
                + editClientDescriptor + "}";
        assertEquals(expected, editCommand.toString());
    }

}
