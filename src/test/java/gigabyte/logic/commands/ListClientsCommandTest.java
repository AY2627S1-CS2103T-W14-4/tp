package gigabyte.logic.commands;

import static gigabyte.logic.commands.CommandTestUtil.assertCommandSuccess;
import static gigabyte.logic.commands.CommandTestUtil.showClientAtIndex;
import static gigabyte.testutil.TypicalClients.getTypicalGigabyteData;
import static gigabyte.testutil.TypicalIndexes.INDEX_FIRST_CLIENT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gigabyte.model.Model;
import gigabyte.model.ModelManager;
import gigabyte.model.UserPrefs;

/**
 * Contains integration tests (interaction with the Model) and unit tests for ListClientsCommand.
 */
public class ListClientsCommandTest {

    private Model model;
    private Model expectedModel;

    @BeforeEach
    public void setUp() {
        model = new ModelManager(getTypicalGigabyteData(), new UserPrefs());
        expectedModel = new ModelManager(model.getGigabyteData(), new UserPrefs());
    }

    @Test
    public void execute_listIsNotFiltered_showsSameList() {
        assertCommandSuccess(new ListClientsCommand(), model, ListClientsCommand.MESSAGE_SUCCESS, expectedModel);
    }

    @Test
    public void execute_listIsFiltered_showsEverything() {
        showClientAtIndex(model, INDEX_FIRST_CLIENT);
        assertCommandSuccess(new ListClientsCommand(), model, ListClientsCommand.MESSAGE_SUCCESS, expectedModel);
    }
}
