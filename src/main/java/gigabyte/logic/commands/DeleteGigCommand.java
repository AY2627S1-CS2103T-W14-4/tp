package gigabyte.logic.commands;

import static gigabyte.commons.util.CollectionUtil.requireAllNonNull;
import static java.util.Objects.requireNonNull;

import gigabyte.commons.core.index.Index;
import gigabyte.commons.util.ToStringBuilder;
import gigabyte.logic.commands.exceptions.CommandException;
import gigabyte.model.Model;
import gigabyte.model.client.Client;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.exceptions.GigHasPaymentsException;

/**
 * Permanently deletes a gig with no payment obligations using its client's normal gig order.
 */
public class DeleteGigCommand extends Command {
    public static final String COMMAND_WORD = "deletegig";
    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Permanently deletes a gig with no payment obligations.\n"
            + "Parameters: CLIENT_INDEX GIG_INDEX (positive integers)\n"
            + "Uses the displayed client list and that client's normal gig order.\n"
            + "Example: " + COMMAND_WORD + " 2 1";
    public static final String MESSAGE_SUCCESS = "Deleted gig for %1$s, gig %2$d: %3$s";

    private final Index clientIndex;
    private final Index gigIndex;

    /**
     * Creates a command to delete the indexed gig.
     */
    public DeleteGigCommand(Index clientIndex, Index gigIndex) {
        requireAllNonNull(clientIndex, gigIndex);
        this.clientIndex = clientIndex;
        this.gigIndex = gigIndex;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        Client client = CommandUtil.getClient(model, clientIndex);
        Gig target = CommandUtil.getGig(model, client, gigIndex);
        try {
            model.deleteGig(target);
        } catch (GigHasPaymentsException exception) {
            throw new CommandException(GigHasPaymentsException.MESSAGE, exception);
        }
        return new CommandResult(String.format(MESSAGE_SUCCESS, client.getName(), gigIndex.getOneBased(),
                target.getTitle()));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof DeleteGigCommand otherCommand)) {
            return false;
        }
        return clientIndex.equals(otherCommand.clientIndex) && gigIndex.equals(otherCommand.gigIndex);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("clientIndex", clientIndex).add("gigIndex", gigIndex).toString();
    }
}
