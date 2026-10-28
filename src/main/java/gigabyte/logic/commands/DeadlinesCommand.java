package gigabyte.logic.commands;

import gigabyte.model.Model;

/** Shows all active gigs ordered by deadline. */
public class DeadlinesCommand extends Command {
    public static final String COMMAND_WORD = "deadlines";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Shows active gigs ordered by deadline.\n"
            + "Parameters: none\nExample: " + COMMAND_WORD;
    public static final String MESSAGE_SUCCESS = "Showing active gig deadlines.";

    @Override
    public CommandResult execute(Model model) {
        return CommandResult.deadlineOverview(MESSAGE_SUCCESS);
    }
}
