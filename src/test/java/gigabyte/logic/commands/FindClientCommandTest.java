package gigabyte.logic.commands;

import static gigabyte.logic.Messages.MESSAGE_CLIENTS_LISTED_OVERVIEW;
import static gigabyte.logic.commands.CommandTestUtil.assertCommandSuccess;
import static gigabyte.testutil.TypicalClients.CARL;
import static gigabyte.testutil.TypicalClients.ELLE;
import static gigabyte.testutil.TypicalClients.FIONA;
import static gigabyte.testutil.TypicalClients.getTypicalGigabyteData;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import gigabyte.model.Model;
import gigabyte.model.ModelManager;
import gigabyte.model.UserPrefs;
import gigabyte.model.client.ClientNameContainsKeywordsPredicate;

/**
 * Contains integration tests (interaction with the Model) for {@code FindClientCommand}.
 */
public class FindClientCommandTest {
    private Model model = new ModelManager(getTypicalGigabyteData(), new UserPrefs());
    private Model expectedModel = new ModelManager(getTypicalGigabyteData(), new UserPrefs());

    @Test
    public void equals() {
        ClientNameContainsKeywordsPredicate firstPredicate =
                new ClientNameContainsKeywordsPredicate(List.of("first"));
        ClientNameContainsKeywordsPredicate secondPredicate =
                new ClientNameContainsKeywordsPredicate(List.of("second"));

        FindClientCommand findFirstCommand = new FindClientCommand(firstPredicate);
        FindClientCommand findSecondCommand = new FindClientCommand(secondPredicate);

        // same object -> returns true
        assertTrue(findFirstCommand.equals(findFirstCommand));

        // same values -> returns true
        FindClientCommand findFirstCommandCopy = new FindClientCommand(firstPredicate);
        assertTrue(findFirstCommand.equals(findFirstCommandCopy));

        // different types -> returns false
        assertFalse(findFirstCommand.equals(1));

        // null -> returns false
        assertFalse(findFirstCommand.equals(null));

        // different client -> returns false
        assertFalse(findFirstCommand.equals(findSecondCommand));
    }

    @Test
    public void execute_zeroKeywords_noClientFound() {
        String expectedMessage = String.format(MESSAGE_CLIENTS_LISTED_OVERVIEW, 0);
        ClientNameContainsKeywordsPredicate predicate = preparePredicate(" ");
        FindClientCommand command = new FindClientCommand(predicate);
        expectedModel.updateFilteredClientList(predicate);
        assertCommandSuccess(command, model, expectedMessage, expectedModel);
        assertEquals(List.of(), model.getFilteredClientList());
    }

    @Test
    public void execute_multipleKeywords_multipleClientsFound() {
        String expectedMessage = String.format(MESSAGE_CLIENTS_LISTED_OVERVIEW, 3);
        ClientNameContainsKeywordsPredicate predicate = preparePredicate("Kurz Elle Kunz");
        FindClientCommand command = new FindClientCommand(predicate);
        expectedModel.updateFilteredClientList(predicate);
        assertCommandSuccess(command, model, expectedMessage, expectedModel);
        assertEquals(List.of(CARL, ELLE, FIONA), model.getFilteredClientList());
    }

    @Test
    public void toStringMethod() {
        ClientNameContainsKeywordsPredicate predicate = new ClientNameContainsKeywordsPredicate(List.of("keyword"));
        FindClientCommand findCommand = new FindClientCommand(predicate);
        String expected = FindClientCommand.class.getCanonicalName() + "{predicate=" + predicate + "}";
        assertEquals(expected, findCommand.toString());
    }

    /**
     * Parses {@code userInput} into a {@code ClientNameContainsKeywordsPredicate}.
     */
    private ClientNameContainsKeywordsPredicate preparePredicate(String userInput) {
        return new ClientNameContainsKeywordsPredicate(List.of(userInput.split("\\s+")));
    }
}
