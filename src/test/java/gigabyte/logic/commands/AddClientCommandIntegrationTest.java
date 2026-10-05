package gigabyte.logic.commands;

import static gigabyte.logic.commands.CommandTestUtil.assertCommandFailure;
import static gigabyte.logic.commands.CommandTestUtil.assertCommandSuccess;
import static gigabyte.testutil.TypicalClients.getTypicalGigabyteData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gigabyte.logic.Messages;
import gigabyte.model.Model;
import gigabyte.model.ModelManager;
import gigabyte.model.UserPrefs;
import gigabyte.model.client.Client;
import gigabyte.testutil.ClientBuilder;

/**
 * Contains integration tests (interaction with the Model) for {@code AddClientCommand}.
 */
public class AddClientCommandIntegrationTest {

    private Model model;

    @BeforeEach
    public void setUp() {
        model = new ModelManager(getTypicalGigabyteData(), new UserPrefs());
    }

    @Test
    public void execute_newClient_success() {
        Client validClient = new ClientBuilder().build();

        Model expectedModel = new ModelManager(model.getGigabyteData(), new UserPrefs());
        expectedModel.addClient(validClient);

        assertCommandSuccess(new AddClientCommand(validClient), model,
                String.format(AddClientCommand.MESSAGE_SUCCESS, Messages.format(validClient)),
                expectedModel);
    }

    @Test
    public void execute_duplicateClient_throwsCommandException() {
        Client clientInList = model.getGigabyteData().getClientList().get(0);
        assertCommandFailure(new AddClientCommand(clientInList), model,
                AddClientCommand.MESSAGE_DUPLICATE_CLIENT);
    }

}
