package gigabyte.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.List;

import gigabyte.commons.core.index.Index;
import gigabyte.logic.Messages;
import gigabyte.logic.commands.exceptions.CommandException;
import gigabyte.model.Model;
import gigabyte.model.client.Client;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.GigOrdering;
import gigabyte.model.gig.PaymentObligation;
import gigabyte.model.gig.PaymentObligationOrdering;

/** Shared lookup operations for commands that use the indexes shown in the UI. */
final class CommandUtil {
    private CommandUtil() {}

    /** Returns the client at the displayed index. */
    static Client getClient(Model model, Index clientIndex) throws CommandException {
        requireNonNull(model);
        requireNonNull(clientIndex);
        List<Client> clients = model.getFilteredClientList();
        if (clientIndex.getZeroBased() >= clients.size()) {
            throw new CommandException(Messages.MESSAGE_INVALID_CLIENT_DISPLAYED_INDEX);
        }
        return clients.get(clientIndex.getZeroBased());
    }

    /** Returns the gig at {@code gigIndex} in the client's displayed gig order. */
    static Gig getGig(Model model, Client client, Index gigIndex) throws CommandException {
        requireNonNull(model);
        requireNonNull(client);
        requireNonNull(gigIndex);
        List<Gig> gigs = GigOrdering.forClient(model.getGigList(), client);
        if (gigIndex.getZeroBased() >= gigs.size()) {
            throw new CommandException(Messages.MESSAGE_INVALID_GIG_INDEX);
        }
        return gigs.get(gigIndex.getZeroBased());
    }

    /** Returns the payment at {@code paymentIndex} in the gig's displayed payment order. */
    static PaymentObligation getPaymentObligation(Model model, Gig gig, Index paymentIndex)
            throws CommandException {
        requireNonNull(model);
        requireNonNull(gig);
        requireNonNull(paymentIndex);
        List<PaymentObligation> payments = PaymentObligationOrdering.forGig(model.getPaymentObligationList(), gig);
        if (paymentIndex.getZeroBased() >= payments.size()) {
            throw new CommandException(Messages.MESSAGE_INVALID_PAYMENT_INDEX);
        }
        return payments.get(paymentIndex.getZeroBased());
    }
}
