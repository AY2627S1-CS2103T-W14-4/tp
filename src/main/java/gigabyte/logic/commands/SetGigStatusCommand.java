package gigabyte.logic.commands;

import static gigabyte.commons.util.CollectionUtil.requireAllNonNull;
import static java.util.Objects.requireNonNull;

import gigabyte.commons.core.index.Index;
import gigabyte.commons.util.ToStringBuilder;
import gigabyte.logic.commands.exceptions.CommandException;
import gigabyte.model.Model;
import gigabyte.model.client.Client;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.GigStatus;

/** Updates the status of a gig belonging to a displayed client. */
public class SetGigStatusCommand extends Command {
    public static final String COMMAND_WORD = "setstatus";
    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Updates the status of a gig belonging to a client in the displayed client list.\n"
            + "Parameters: CLIENT_INDEX GIG_INDEX s/STATUS\n"
            + "Indexes must be positive integers; GIG_INDEX is the position in this client's gig panel. "
            + "STATUS: NOT_STARTED, IN_PROGRESS, or COMPLETED.\n"
            + "Example: " + COMMAND_WORD + " 1 2 s/IN_PROGRESS";
    public static final String MESSAGE_STATUS_UNCHANGED = "The gig already has status %1$s.";
    public static final String MESSAGE_SUCCESS = "Updated status for %1$s, gig %2$d: %3$s";

    private final Index clientIndex;
    private final Index gigIndex;
    private final GigStatus status;

    /** Creates a command to set a gig's status. */
    public SetGigStatusCommand(Index clientIndex, Index gigIndex, GigStatus status) {
        requireAllNonNull(clientIndex, gigIndex, status);
        this.clientIndex = clientIndex;
        this.gigIndex = gigIndex;
        this.status = status;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        Client client = CommandUtil.getClient(model, clientIndex);
        Gig targetGig = CommandUtil.getGig(model, client, gigIndex);
        if (targetGig.getStatus() == status) {
            throw new CommandException(String.format(MESSAGE_STATUS_UNCHANGED, status));
        }

        model.setGig(targetGig, targetGig.withStatus(status));
        return new CommandResult(String.format(MESSAGE_SUCCESS, client.getName(), gigIndex.getOneBased(), status));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof SetGigStatusCommand otherCommand)) {
            return false;
        }
        return clientIndex.equals(otherCommand.clientIndex)
                && gigIndex.equals(otherCommand.gigIndex)
                && status == otherCommand.status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("clientIndex", clientIndex)
                .add("gigIndex", gigIndex)
                .add("status", status)
                .toString();
    }
}
