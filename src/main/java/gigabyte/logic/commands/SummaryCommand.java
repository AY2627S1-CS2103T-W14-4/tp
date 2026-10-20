package gigabyte.logic.commands;

import static java.util.Objects.requireNonNull;

import java.math.BigInteger;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import gigabyte.model.Model;
import gigabyte.model.gig.GigStatus;
import gigabyte.model.gig.PaymentObligation;

/** Displays a read-only summary of all stored clients, gigs, and payments. */
public class SummaryCommand extends Command {
    public static final String COMMAND_WORD = "summary";
    private final Clock clock;

    public SummaryCommand() {
        this(Clock.systemDefaultZone());
    }

    /** Creates a summary command using {@code clock}, primarily for deterministic date-sensitive tests. */
    public SummaryCommand(Clock clock) {
        this.clock = requireNonNull(clock);
    }

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        Map<GigStatus, Long> gigCounts = new EnumMap<>(GigStatus.class);
        for (GigStatus status : GigStatus.values()) {
            gigCounts.put(status, 0L);
        }
        model.getGigList().forEach(gig -> gigCounts.compute(gig.getStatus(), (status, count) -> count + 1));

        BigInteger outstandingTotal = BigInteger.ZERO;
        BigInteger overdueTotal = BigInteger.ZERO;
        BigInteger paidTotal = BigInteger.ZERO;
        long outstandingCount = 0;
        long overdueCount = 0;
        long paidCount = 0;
        LocalDate today = LocalDate.now(clock);
        for (PaymentObligation payment : model.getPaymentObligationList()) {
            BigInteger cents = payment.getAmount().getCents();
            if (payment.isPaid()) {
                paidCount++;
                paidTotal = paidTotal.add(cents);
            } else {
                outstandingCount++;
                outstandingTotal = outstandingTotal.add(cents);
                if (payment.getDueDate().getValue().isBefore(today)) {
                    overdueCount++;
                    overdueTotal = overdueTotal.add(cents);
                }
            }
        }

        StringBuilder result = new StringBuilder()
                .append("Clients: ").append(model.getGigabyteData().getClientList().size()).append('\n')
                .append("Gigs: ").append(model.getGigList().size()).append(" total\n");
        for (GigStatus status : GigStatus.values()) {
            result.append("  ").append(status).append(": ").append(gigCounts.get(status)).append('\n');
        }
        return new CommandResult(result
                .append("Payments:\n")
                .append("  Outstanding: ").append(outstandingCount).append(" (").append(formatCents(outstandingTotal))
                        .append(")\n")
                .append("  Of which overdue: ").append(overdueCount).append(" (").append(formatCents(overdueTotal))
                        .append(")\n")
                .append("  Marked paid: ").append(paidCount).append(" (").append(formatCents(paidTotal)).append(")")
                .toString());
    }

    private static String formatCents(BigInteger cents) {
        BigInteger whole = cents.divide(BigInteger.valueOf(100));
        BigInteger fraction = cents.remainder(BigInteger.valueOf(100)).abs();
        return String.format(Locale.ROOT, "$%,d.%02d", whole, fraction.intValue());
    }
}
