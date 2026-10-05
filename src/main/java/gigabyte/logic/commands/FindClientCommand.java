package gigabyte.logic.commands;

import static java.util.Objects.requireNonNull;

import gigabyte.commons.util.ToStringBuilder;
import gigabyte.logic.Messages;
import gigabyte.model.Model;
import gigabyte.model.client.ClientNameContainsKeywordsPredicate;

/**
 * Finds and lists all clients in the client list whose name contains any of the argument keywords.
 * Keyword matching is case insensitive.
 */
public class FindClientCommand extends Command {

    public static final String COMMAND_WORD = "find";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Finds all clients whose names contain any of "
            + "the specified keywords (case-insensitive) and displays them as a list with index numbers.\n"
            + "Parameters: KEYWORD [MORE_KEYWORDS]...\n"
            + "Example: " + COMMAND_WORD + " alice bob charlie";

    private final ClientNameContainsKeywordsPredicate predicate;

    public FindClientCommand(ClientNameContainsKeywordsPredicate predicate) {
        this.predicate = predicate;
    }

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        model.updateFilteredClientList(predicate);
        return new CommandResult(
                String.format(Messages.MESSAGE_CLIENTS_LISTED_OVERVIEW, model.getFilteredClientList().size()));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof FindClientCommand otherFindClientCommand)) {
            return false;
        }

        return predicate.equals(otherFindClientCommand.predicate);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("predicate", predicate)
                .toString();
    }
}
