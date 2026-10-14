package gigabyte.logic.commands;

import static java.util.Objects.requireNonNull;

import gigabyte.model.Model;

/**
 * Displays unpaid payment obligations across all gigs and clients.
 */
public class OutstandingCommand extends Command {
    public static final String COMMAND_WORD = "outstanding";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Lists unpaid obligations by due date.\n"
            + "Parameters: none\nExample: " + COMMAND_WORD;

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        long count = model.getPaymentObligationList().stream().filter(payment -> !payment.isPaid()).count();
        return CommandResult.forOutstandingPayments(String.format("%d outstanding payment obligations.", count));
    }
}
