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
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.GigOrdering;
import gigabyte.model.gig.PaymentObligation;
import gigabyte.model.gig.PaymentObligationOrdering;

/** Deletes one payment obligation using the displayed client, gig, and payment indexes. */
public class DeletePaymentCommand extends Command {
    public static final String COMMAND_WORD = "deletepayment";
    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Permanently deletes a paid or unpaid payment obligation without confirmation.\n"
            + "Parameters: CLIENT_INDEX GIG_INDEX PAYMENT_INDEX\n"
            + "Indexes are positive integers in the displayed client list, that client's gig list, and "
            + "the gig's payment list, respectively.\n"
            + "Example: " + COMMAND_WORD + " 1 2 3";
    public static final String MESSAGE_INVALID_GIG_INDEX = "The gig index provided is invalid for this client.";
    public static final String MESSAGE_INVALID_PAYMENT_INDEX =
            "The payment index provided is invalid for this gig.";
    public static final String MESSAGE_SUCCESS = "Deleted payment obligation for %1$s, gig %2$d (%3$s), "
            + "payment %4$d: Amount: %5$s; Due date: %6$s; Previous state: %7$s";

    private final Index clientIndex;
    private final Index gigIndex;
    private final Index paymentIndex;

    /** Creates a command for the indexed payment obligation. */
    public DeletePaymentCommand(Index clientIndex, Index gigIndex, Index paymentIndex) {
        requireAllNonNull(clientIndex, gigIndex, paymentIndex);
        this.clientIndex = clientIndex;
        this.gigIndex = gigIndex;
        this.paymentIndex = paymentIndex;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        List<Client> clients = model.getFilteredClientList();
        if (clientIndex.getZeroBased() >= clients.size()) {
            throw new CommandException(Messages.MESSAGE_INVALID_CLIENT_DISPLAYED_INDEX);
        }

        Client client = clients.get(clientIndex.getZeroBased());
        List<Gig> gigs = GigOrdering.forClient(model.getGigList(), client);
        if (gigIndex.getZeroBased() >= gigs.size()) {
            throw new CommandException(MESSAGE_INVALID_GIG_INDEX);
        }

        Gig gig = gigs.get(gigIndex.getZeroBased());
        List<PaymentObligation> payments = PaymentObligationOrdering.forGig(model.getPaymentObligationList(), gig);
        if (paymentIndex.getZeroBased() >= payments.size()) {
            throw new CommandException(MESSAGE_INVALID_PAYMENT_INDEX);
        }

        PaymentObligation payment = payments.get(paymentIndex.getZeroBased());
        model.deletePaymentObligation(payment);
        return new CommandResult(String.format(MESSAGE_SUCCESS, client.getName(), gigIndex.getOneBased(),
                gig.getTitle(), paymentIndex.getOneBased(), payment.getAmount(), payment.getDueDate(),
                payment.isPaid() ? "PAID" : "UNPAID"));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof DeletePaymentCommand otherCommand)) {
            return false;
        }
        return clientIndex.equals(otherCommand.clientIndex)
                && gigIndex.equals(otherCommand.gigIndex)
                && paymentIndex.equals(otherCommand.paymentIndex);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("clientIndex", clientIndex)
                .add("gigIndex", gigIndex)
                .add("paymentIndex", paymentIndex)
                .toString();
    }
}
