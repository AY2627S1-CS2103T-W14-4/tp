package gigabyte.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import gigabyte.model.ModelManager;

public class DeadlinesCommandTest {
    @Test
    public void execute_requestsDeadlineOverviewWithoutChangingData() {
        CommandResult result = new DeadlinesCommand().execute(new ModelManager());

        assertEquals(CommandResult.deadlineOverview(DeadlinesCommand.MESSAGE_SUCCESS), result);
        assertEquals(DeadlinesCommand.MESSAGE_SUCCESS, result.getFeedbackToUser());
        assertEquals(true, result.shouldShowDeadlineOverview());
    }
}
