package gigabyte.logic.commands;

import static gigabyte.testutil.Assert.assertThrows;
import static gigabyte.testutil.TypicalClients.ALICE;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import gigabyte.commons.core.GuiSettings;
import gigabyte.logic.Messages;
import gigabyte.logic.commands.exceptions.CommandException;
import gigabyte.model.GigabyteData;
import gigabyte.model.Model;
import gigabyte.model.ModelManager;
import gigabyte.model.ReadOnlyGigabyteData;
import gigabyte.model.ReadOnlyUserPrefs;
import gigabyte.model.client.Client;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.PaymentObligation;
import gigabyte.testutil.ClientBuilder;
import javafx.collections.ObservableList;

public class AddClientCommandTest {

    @Test
    public void constructor_nullClient_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new AddClientCommand(null));
    }

    @Test
    public void execute_clientAcceptedByModel_addSuccessful() throws Exception {
        ModelStubAcceptingClientAdded modelStub = new ModelStubAcceptingClientAdded();
        Client validClient = new ClientBuilder().build();

        CommandResult commandResult = new AddClientCommand(validClient).execute(modelStub);

        assertEquals(String.format(AddClientCommand.MESSAGE_SUCCESS, Messages.format(validClient)),
                commandResult.getFeedbackToUser());
        assertEquals(List.of(validClient), modelStub.clientsAdded);
    }

    @Test
    public void execute_duplicateClient_throwsCommandException() {
        Client validClient = new ClientBuilder().build();
        AddClientCommand addCommand = new AddClientCommand(validClient);
        ModelStub modelStub = new ModelStubWithClient(validClient);

        assertThrows(CommandException.class,
                AddClientCommand.MESSAGE_DUPLICATE_CLIENT, () -> addCommand.execute(modelStub));
    }

    @Test
    public void execute_nameDiffersOnlyInCaseOrSpacing_throwsCommandException() throws Exception {
        ModelStubAcceptingClientAdded modelStub = new ModelStubAcceptingClientAdded();
        new AddClientCommand(new ClientBuilder().withName("John Doe").build()).execute(modelStub);

        // EP: differs in case only
        Client upperCaseJohn = new ClientBuilder().withName("JOHN DOE").withPhone("90000001").build();
        assertThrows(CommandException.class, Messages.MESSAGE_DUPLICATE_CLIENT_NAME, () ->
                new AddClientCommand(upperCaseJohn).execute(modelStub));

        // EP: differs in internal and trailing spacing only
        Client spacedJohn = new ClientBuilder().withName("John   Doe ").withPhone("90000002").build();
        assertThrows(CommandException.class, Messages.MESSAGE_DUPLICATE_CLIENT_NAME, () ->
                new AddClientCommand(spacedJohn).execute(modelStub));

        assertEquals(1, modelStub.clientsAdded.size());
    }

    @Test
    public void execute_samePhoneOrEmailAsExistingClient_addsWithNote() throws Exception {
        ModelStubAcceptingClientAdded modelStub = new ModelStubAcceptingClientAdded();
        Client bernice = new ClientBuilder().withName("Bernice Yu").withPhone("99272758")
                .withEmail("berniceyu@example.com").build();
        new AddClientCommand(bernice).execute(modelStub);

        // EP: same phone only
        Client samePhone = new ClientBuilder().withName("Alex Yeoh").withPhone("99272758")
                .withEmail("alex@example.com").build();
        assertEquals(String.format(AddClientCommand.MESSAGE_SUCCESS, Messages.format(samePhone))
                + "\nNote: Bernice Yu has the same phone number.",
                new AddClientCommand(samePhone).execute(modelStub).getFeedbackToUser());

        // EP: same email only, differing in case
        Client sameEmail = new ClientBuilder().withName("Charlotte Oliveiro").withPhone("93210283")
                .withEmail("BerniceYu@example.com").build();
        assertEquals(String.format(AddClientCommand.MESSAGE_SUCCESS, Messages.format(sameEmail))
                + "\nNote: Bernice Yu has the same email.",
                new AddClientCommand(sameEmail).execute(modelStub).getFeedbackToUser());

        // EP: same phone as one client and same email as another -> one note per client
        Client sameBoth = new ClientBuilder().withName("David Li").withPhone("93210283")
                .withEmail("alex@example.com").build();
        assertEquals(String.format(AddClientCommand.MESSAGE_SUCCESS, Messages.format(sameBoth))
                + "\nNote: Alex Yeoh has the same email."
                + "\nNote: Charlotte Oliveiro has the same phone number.",
                new AddClientCommand(sameBoth).execute(modelStub).getFeedbackToUser());

        // EP: same phone and email as one client, plus one shared field each with two others
        Client sameAsBernice = new ClientBuilder().withName("Irfan Ibrahim").withPhone("99272758")
                .withEmail("berniceyu@example.com").build();
        assertEquals(String.format(AddClientCommand.MESSAGE_SUCCESS, Messages.format(sameAsBernice))
                + "\nNote: Bernice Yu has the same phone number and email."
                + "\nNote: Alex Yeoh has the same phone number."
                + "\nNote: Charlotte Oliveiro has the same email.",
                new AddClientCommand(sameAsBernice).execute(modelStub).getFeedbackToUser());

        assertEquals(5, modelStub.clientsAdded.size());
    }

    @Test
    public void execute_similarNameWithDifferentContacts_addsWithWarning() throws Exception {
        Model model = new ModelManager();
        model.addClient(new ClientBuilder().withName("John Doe").build());
        Client jon = new ClientBuilder().withName("Jon Doe").withPhone("90000001")
                .withEmail("jon@example.com").build();

        assertEquals(String.format(AddClientCommand.MESSAGE_SUCCESS, Messages.format(jon))
                + "\nWarning: John Doe has a similar name. Check for an accidental duplicate.",
                new AddClientCommand(jon).execute(model).getFeedbackToUser());
        assertEquals(List.of("John Doe", "Jon Doe"), model.getGigabyteData().getClientList().stream()
                .map(client -> client.getName().fullName).toList());
    }

    @Test
    public void execute_multipleSimilarNamesHiddenByFilter_warnsInOriginalClientOrder() throws Exception {
        Model model = new ModelManager();
        model.addClient(new ClientBuilder().withName("John Doe").build());
        model.addClient(new ClientBuilder().withName("Zelda Wong").build());
        model.addClient(new ClientBuilder().withName("Joan Doe").build());
        model.updateFilteredClientList(client -> false);
        assertTrue(model.getFilteredClientList().isEmpty());
        Client jon = new ClientBuilder().withName("Jon Doe").withPhone("90000001")
                .withEmail("jon@example.com").build();

        assertEquals(String.format(AddClientCommand.MESSAGE_SUCCESS, Messages.format(jon))
                + "\nWarning: John Doe has a similar name. Check for an accidental duplicate."
                + "\nWarning: Joan Doe has a similar name. Check for an accidental duplicate.",
                new AddClientCommand(jon).execute(model).getFeedbackToUser());
        assertTrue(model.getGigabyteData().getClientList().contains(jon));
    }

    @Test
    public void execute_unrelatedOrShortName_addsWithoutSimilarNameWarning() throws Exception {
        for (String name : List.of("Lu", "Zelda Wong")) {
            Model model = new ModelManager();
            model.addClient(new ClientBuilder().withName("Li").build());
            model.addClient(new ClientBuilder().withName("John Doe").build());
            Client client = new ClientBuilder().withName(name).withPhone("90000001")
                    .withEmail("different@example.com").build();

            assertEquals(String.format(AddClientCommand.MESSAGE_SUCCESS, Messages.format(client)),
                    new AddClientCommand(client).execute(model).getFeedbackToUser());
            assertTrue(model.getGigabyteData().getClientList().contains(client));
        }
    }

    @Test
    public void execute_similarNameAndSharedContacts_preservesBothWarnings() throws Exception {
        Model model = new ModelManager();
        model.addClient(new ClientBuilder().withName("John Doe").build());
        Client jon = new ClientBuilder().withName("Jon Doe").build();

        assertEquals(String.format(AddClientCommand.MESSAGE_SUCCESS, Messages.format(jon))
                + "\nNote: John Doe has the same phone number and email."
                + "\nWarning: John Doe has a similar name. Check for an accidental duplicate.",
                new AddClientCommand(jon).execute(model).getFeedbackToUser());
        assertTrue(model.getGigabyteData().getClientList().contains(jon));
    }

    @Test
    public void equals() {
        Client alice = new ClientBuilder().withName("Alice").build();
        Client bob = new ClientBuilder().withName("Bob").build();
        AddClientCommand addAliceCommand = new AddClientCommand(alice);
        AddClientCommand addBobCommand = new AddClientCommand(bob);

        // same object -> returns true
        assertTrue(addAliceCommand.equals(addAliceCommand));

        // same values -> returns true
        AddClientCommand addAliceCommandCopy = new AddClientCommand(alice);
        assertTrue(addAliceCommand.equals(addAliceCommandCopy));

        // different types -> returns false
        assertFalse(addAliceCommand.equals(1));

        // null -> returns false
        assertFalse(addAliceCommand.equals(null));

        // different client -> returns false
        assertFalse(addAliceCommand.equals(addBobCommand));
    }

    @Test
    public void toStringMethod() {
        AddClientCommand addCommand = new AddClientCommand(ALICE);
        String expected = AddClientCommand.class.getCanonicalName() + "{toAdd=" + ALICE + "}";
        assertEquals(expected, addCommand.toString());
    }

    /**
     * A default model stub that has all of the methods failing.
     */
    private class ModelStub implements Model {
        @Override
        public ReadOnlyUserPrefs getUserPrefs() {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public GuiSettings getGuiSettings() {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public void setGuiSettings(GuiSettings guiSettings) {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public void addClient(Client client) {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public void setGigabyteData(ReadOnlyGigabyteData newData) {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public ReadOnlyGigabyteData getGigabyteData() {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public boolean hasClient(Client client) {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public boolean hasOtherClientWithSameNormalizedName(Client client) {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public void deleteClient(Client target) {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public void setClient(Client target, Client editedClient) {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public ObservableList<Client> getFilteredClientList() {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public void updateFilteredClientList(Predicate<Client> predicate) {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public void addGig(Gig gig) {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public void setGig(Gig target, Gig replacement) {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public void deleteGig(Gig target) {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public ObservableList<Gig> getGigList() {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public ObservableList<PaymentObligation> getPaymentObligationList() {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public void addPaymentObligation(PaymentObligation obligation) {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public void setPaymentPaidState(PaymentObligation obligation, boolean isPaid) {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public void deletePaymentObligation(PaymentObligation obligation) {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public void setPaymentObligation(PaymentObligation target, PaymentObligation replacement) {
            throw new AssertionError("This method should not be called.");
        }
    }

    /**
     * A Model stub that contains a single client.
     */
    private class ModelStubWithClient extends ModelStub {
        private final Client client;

        ModelStubWithClient(Client client) {
            requireNonNull(client);
            this.client = client;
        }

        @Override
        public boolean hasClient(Client client) {
            requireNonNull(client);
            return this.client.isSameClient(client);
        }
    }

    /**
     * A Model stub that always accepts the client being added.
     */
    private class ModelStubAcceptingClientAdded extends ModelStub {
        final ArrayList<Client> clientsAdded = new ArrayList<>();

        @Override
        public boolean hasClient(Client client) {
            requireNonNull(client);
            return clientsAdded.stream().anyMatch(client::isSameClient);
        }

        @Override
        public boolean hasOtherClientWithSameNormalizedName(Client client) {
            requireNonNull(client);
            return clientsAdded.stream().anyMatch(client::hasSameNormalizedName);
        }

        @Override
        public void addClient(Client client) {
            requireNonNull(client);
            clientsAdded.add(client);
        }

        @Override
        public ReadOnlyGigabyteData getGigabyteData() {
            GigabyteData gigabyteData = new GigabyteData();
            clientsAdded.forEach(gigabyteData::addClient);
            return gigabyteData;
        }
    }

}
