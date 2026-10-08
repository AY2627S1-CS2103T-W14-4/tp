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
import gigabyte.model.gig.PaymentObligation;

/**
 * Records an unpaid obligation for a gig in a displayed client's gig list.
 */
public class AddPaymentCommand extends Command {
    public static final String COMMAND_WORD = "addpayment";
    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Records an unpaid payment obligation for an existing gig.\n"
            + "Parameters: CLIENT_INDEX GIG_INDEX a/AMOUNT d/DUE_DATE\n"
            + "Indexes must be positive integers; GIG_INDEX is the position in this client's gig panel. "
            + "AMOUNT must be positive with at most two decimal places; DUE_DATE must use yyyy-MM-dd.\n"
            + "Example: " + COMMAND_WORD + " 1 2 a/500.00 d/2027-01-31";
    public static final String MESSAGE_INVALID_GIG_INDEX = "The gig index provided is invalid for this client.";
    public static final String MESSAGE_SUCCESS = "Recorded unpaid obligation for %1$s, gig %2$d: "
            + "Amount: %3$s; Due date: %4$s";

    private final Index clientIndex;
    private final Index gigIndex;
    private final Fee amount;
    private final Deadline dueDate;

    /**
     * Creates a command to record the given amount and due date against the indexed gig.
     */
    public AddPaymentCommand(Index clientIndex, Index gigIndex, Fee amount, Deadline dueDate) {
        requireAllNonNull(clientIndex, gigIndex, amount, dueDate);
        this.clientIndex = clientIndex;
        this.gigIndex = gigIndex;
        this.amount = amount;
        this.dueDate = dueDate;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        List<Client> clients = model.getFilteredClientList();
        if (clientIndex.getZeroBased() >= clients.size()) {
            throw new CommandException(Messages.MESSAGE_INVALID_CLIENT_DISPLAYED_INDEX);
        }
        Client client = clients.get(clientIndex.getZeroBased());
        List<Gig> gigs = model.getGigList().stream()
                .filter(gig -> gig.getClient().hasSameUid(client)).toList();
        if (gigIndex.getZeroBased() >= gigs.size()) {
            throw new CommandException(MESSAGE_INVALID_GIG_INDEX);
        }

        model.addPaymentObligation(new PaymentObligation(gigs.get(gigIndex.getZeroBased()), amount, dueDate, false));
        return new CommandResult(String.format(MESSAGE_SUCCESS, client.getName(), gigIndex.getOneBased(),
                amount, dueDate));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof AddPaymentCommand otherCommand)) {
            return false;
        }
        return clientIndex.equals(otherCommand.clientIndex)
                && gigIndex.equals(otherCommand.gigIndex)
                && amount.equals(otherCommand.amount)
                && dueDate.equals(otherCommand.dueDate);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("clientIndex", clientIndex)
                .add("gigIndex", gigIndex)
                .add("amount", amount)
                .add("dueDate", dueDate)
                .toString();
    }
}
