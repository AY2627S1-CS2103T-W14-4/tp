package gigabyte.logic.commands;

import static gigabyte.commons.util.CollectionUtil.requireAllNonNull;
import static java.util.Objects.requireNonNull;

import java.util.List;

import gigabyte.commons.core.index.Index;
import gigabyte.commons.util.ToStringBuilder;
import gigabyte.logic.Messages;
import gigabyte.logic.commands.exceptions.CommandException;
import gigabyte.model.Model;
import gigabyte.model.client.Client;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.GigStatus;

/**
 * Creates a gig linked to a client identified by its displayed index.
 */
public class AddGigCommand extends Command {
    public static final String COMMAND_WORD = "addgig";
    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Adds a gig for a client in the displayed client list.\n"
            + "Parameters: INDEX s/STATUS d/DEADLINE f/FEE\n"
            + "INDEX must be a positive integer; STATUS: NOT_STARTED, IN_PROGRESS, or COMPLETED; "
            + "DEADLINE: yyyy-MM-dd; FEE: positive amount with at most two decimal places.\n"
            + "Example: " + COMMAND_WORD + " 1 s/NOT_STARTED d/2027-01-31 f/1250.00";
    public static final String MESSAGE_SUCCESS = "New gig added for %1$s: "
            + "Status: %2$s; Deadline: %3$s; Agreed fee: %4$s";

    private final Index clientIndex;
    private final GigStatus status;
    private final Deadline deadline;
    private final Fee fee;

    /**
     * Creates a command to add a gig with the given fields for the client at {@code clientIndex}.
     */
    public AddGigCommand(Index clientIndex, GigStatus status, Deadline deadline, Fee fee) {
        requireAllNonNull(clientIndex, status, deadline, fee);
        this.clientIndex = clientIndex;
        this.status = status;
        this.deadline = deadline;
        this.fee = fee;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        List<Client> displayedClients = model.getFilteredClientList();
        if (clientIndex.getZeroBased() >= displayedClients.size()) {
            throw new CommandException(Messages.MESSAGE_INVALID_CLIENT_DISPLAYED_INDEX);
        }

        Client client = displayedClients.get(clientIndex.getZeroBased());
        model.addGig(new Gig(client, status, deadline, fee));
        return new CommandResult(String.format(MESSAGE_SUCCESS, client.getName(), status, deadline, fee));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof AddGigCommand otherCommand)) {
            return false;
        }
        return clientIndex.equals(otherCommand.clientIndex)
                && status == otherCommand.status
                && deadline.equals(otherCommand.deadline)
                && fee.equals(otherCommand.fee);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("clientIndex", clientIndex)
                .add("status", status)
                .add("deadline", deadline)
                .add("fee", fee)
                .toString();
    }
}
