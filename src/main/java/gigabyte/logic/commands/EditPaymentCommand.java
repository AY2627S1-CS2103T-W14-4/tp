package gigabyte.logic.commands;

import static gigabyte.commons.util.CollectionUtil.requireAllNonNull;
import static gigabyte.logic.parser.CliSyntax.PREFIX_AMOUNT;
import static gigabyte.logic.parser.CliSyntax.PREFIX_DEADLINE;
import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Optional;

import gigabyte.commons.core.index.Index;
import gigabyte.commons.util.ToStringBuilder;
import gigabyte.logic.Messages;
import gigabyte.logic.commands.exceptions.CommandException;
import gigabyte.model.Model;
import gigabyte.model.client.Client;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.GigOrdering;
import gigabyte.model.gig.PaymentObligation;
import gigabyte.model.gig.PaymentObligationOrdering;

/** Edits a payment obligation using the displayed client, gig, and payment indexes. */
public class EditPaymentCommand extends Command {
    public static final String COMMAND_WORD = "editpayment";
    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Edits the amount and/or due date of a payment obligation.\n"
            + "Parameters: CLIENT_INDEX GIG_INDEX PAYMENT_INDEX "
            + "[" + PREFIX_AMOUNT + "AMOUNT] [" + PREFIX_DEADLINE + "DUE_DATE]\n"
            + "Provide at least one field. Indexes are positive integers in the displayed client list, "
            + "that client's gig list, and the gig's due-date-ordered payment list, respectively.\n"
            + "Example: " + COMMAND_WORD + " 1 2 1 a/650.00 d/2027-05-15";
    public static final String MESSAGE_NOT_EDITED = "At least one field to edit must be provided.";
    public static final String MESSAGE_PAYMENT_NOT_EDITED =
            "The payment obligation already has the specified details.";
    public static final String MESSAGE_SUCCESS = "Edited payment obligation for %1$s, gig %2$d, payment %3$d: "
            + "Amount: %4$s; Due date: %5$s";

    private final Index clientIndex;
    private final Index gigIndex;
    private final Index paymentIndex;
    private final Optional<Fee> amount;
    private final Optional<Deadline> dueDate;

    /** Creates a command that applies the provided payment details. */
    public EditPaymentCommand(Index clientIndex, Index gigIndex, Index paymentIndex,
                              Optional<Fee> amount, Optional<Deadline> dueDate) {
        requireAllNonNull(clientIndex, gigIndex, paymentIndex, amount, dueDate);
        if (amount.isEmpty() && dueDate.isEmpty()) {
            throw new IllegalArgumentException(MESSAGE_NOT_EDITED);
        }
        this.clientIndex = clientIndex;
        this.gigIndex = gigIndex;
        this.paymentIndex = paymentIndex;
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
        List<Gig> gigs = GigOrdering.forClient(model.getGigList(), client);
        if (gigIndex.getZeroBased() >= gigs.size()) {
            throw new CommandException(Messages.MESSAGE_INVALID_GIG_INDEX);
        }

        Gig gig = gigs.get(gigIndex.getZeroBased());
        List<PaymentObligation> payments = PaymentObligationOrdering.forGig(model.getPaymentObligationList(), gig);
        if (paymentIndex.getZeroBased() >= payments.size()) {
            throw new CommandException(Messages.MESSAGE_INVALID_PAYMENT_INDEX);
        }

        PaymentObligation payment = payments.get(paymentIndex.getZeroBased());
        PaymentObligation replacement = payment.withDetails(
                amount.orElse(payment.getAmount()), dueDate.orElse(payment.getDueDate()));
        if (payment.equals(replacement)) {
            throw new CommandException(MESSAGE_PAYMENT_NOT_EDITED);
        }

        model.setPaymentObligation(payment, replacement);
        return new CommandResult(String.format(MESSAGE_SUCCESS, client.getName(), gigIndex.getOneBased(),
                paymentIndex.getOneBased(), replacement.getAmount(), replacement.getDueDate()));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof EditPaymentCommand otherCommand)) {
            return false;
        }
        return clientIndex.equals(otherCommand.clientIndex)
                && gigIndex.equals(otherCommand.gigIndex)
                && paymentIndex.equals(otherCommand.paymentIndex)
                && amount.equals(otherCommand.amount)
                && dueDate.equals(otherCommand.dueDate);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("clientIndex", clientIndex)
                .add("gigIndex", gigIndex)
                .add("paymentIndex", paymentIndex)
                .add("amount", amount)
                .add("dueDate", dueDate)
                .toString();
    }
}
