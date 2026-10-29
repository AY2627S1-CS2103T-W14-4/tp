package gigabyte.logic.commands;

import static gigabyte.commons.util.CollectionUtil.requireAllNonNull;
import static java.util.Objects.requireNonNull;

import gigabyte.commons.core.index.Index;
import gigabyte.commons.util.ToStringBuilder;
import gigabyte.logic.commands.exceptions.CommandException;
import gigabyte.model.Model;
import gigabyte.model.client.Client;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.PaymentObligation;

/** Marks a payment obligation as paid using the displayed client, gig, and payment indexes. */
public class MarkPaymentPaidCommand extends Command {
    public static final String COMMAND_WORD = "markpaid";
    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Marks a payment obligation as paid.\n"
            + "Parameters: CLIENT_INDEX GIG_INDEX PAYMENT_INDEX\n"
            + "Indexes are positive integers in the displayed client list, that client's gig list, and "
            + "the gig's payment list, respectively.\n"
            + "Example: " + COMMAND_WORD + " 2 1 3";
    public static final String MESSAGE_ALREADY_PAID = "This payment obligation has already been marked as paid.";
    public static final String MESSAGE_SUCCESS = "Marked payment obligation for %1$s, gig %2$d, payment %3$d "
            + "as paid: Amount: %4$s; Due date: %5$s";

    private final Index clientIndex;
    private final Index gigIndex;
    private final Index paymentIndex;

    /** Creates a command for the indexed payment obligation. */
    public MarkPaymentPaidCommand(Index clientIndex, Index gigIndex, Index paymentIndex) {
        requireAllNonNull(clientIndex, gigIndex, paymentIndex);
        this.clientIndex = clientIndex;
        this.gigIndex = gigIndex;
        this.paymentIndex = paymentIndex;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        Client client = CommandUtil.getClient(model, clientIndex);
        Gig gig = CommandUtil.getGig(model, client, gigIndex);
        PaymentObligation payment = CommandUtil.getPaymentObligation(model, gig, paymentIndex);
        if (payment.isPaid()) {
            throw new CommandException(MESSAGE_ALREADY_PAID);
        }

        model.setPaymentPaidState(payment, true);
        return new CommandResult(String.format(MESSAGE_SUCCESS, client.getName(), gigIndex.getOneBased(),
                paymentIndex.getOneBased(), payment.getAmount().toDisplayString(), payment.getDueDate()));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof MarkPaymentPaidCommand otherCommand)) {
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
