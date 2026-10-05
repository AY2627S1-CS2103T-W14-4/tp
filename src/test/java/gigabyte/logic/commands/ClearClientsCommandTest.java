package gigabyte.logic.commands;

import static gigabyte.logic.commands.CommandTestUtil.assertCommandSuccess;
import static gigabyte.testutil.TypicalClients.getTypicalGigabyteData;

import org.junit.jupiter.api.Test;

import gigabyte.model.GigabyteData;
import gigabyte.model.Model;
import gigabyte.model.ModelManager;
import gigabyte.model.UserPrefs;

public class ClearClientsCommandTest {

    @Test
    public void execute_emptyGigabyteData_success() {
        Model model = new ModelManager();
        Model expectedModel = new ModelManager();

        assertCommandSuccess(new ClearClientsCommand(), model, ClearClientsCommand.MESSAGE_SUCCESS, expectedModel);
    }

    @Test
    public void execute_nonEmptyGigabyteData_success() {
        Model model = new ModelManager(getTypicalGigabyteData(), new UserPrefs());
        Model expectedModel = new ModelManager(getTypicalGigabyteData(), new UserPrefs());
        expectedModel.setGigabyteData(new GigabyteData());

        assertCommandSuccess(new ClearClientsCommand(), model, ClearClientsCommand.MESSAGE_SUCCESS, expectedModel);
    }

}
