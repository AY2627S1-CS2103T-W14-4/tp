package gigabyte.logic.commands;

import static java.util.Objects.requireNonNull;

import java.math.BigInteger;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

import gigabyte.model.Model;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.GigStatus;
import gigabyte.model.gig.PaymentObligation;

/** Displays a read-only summary of all stored clients, gigs, and payments. */
public class SummaryCommand extends Command {
    public static final String COMMAND_WORD = "summary";
    private final Clock clock;

    /** Creates a summary command using the system's default time zone. */
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
        String summary = "Clients: " + model.getGigabyteData().getClientList().size() + "\n"
                + formatGigSection(model.getGigList())
                + formatPaymentSection(model.getPaymentObligationList(), LocalDate.now(clock));
        return new CommandResult(summary);
    }

    /** Returns the total number of gigs, followed by the number of gigs in each status. */
    private static String formatGigSection(List<Gig> gigs) {
        Map<GigStatus, Long> gigCounts = new EnumMap<>(GigStatus.class);
        for (GigStatus status : GigStatus.values()) {
            gigCounts.put(status, 0L);
        }
        gigs.forEach(gig -> gigCounts.merge(gig.getStatus(), 1L, Long::sum));

        StringBuilder section = new StringBuilder("Gigs: ").append(gigs.size()).append(" total\n");
        for (GigStatus status : GigStatus.values()) {
            section.append("  ").append(status).append(": ").append(gigCounts.get(status)).append('\n');
        }
        return section.toString();
    }

    /** Returns the count and total amount of outstanding, overdue, and paid payments. */
    private static String formatPaymentSection(List<PaymentObligation> payments, LocalDate today) {
        PaymentTotal outstanding = sumPayments(payments, payment -> !payment.isPaid());
        PaymentTotal overdue = sumPayments(payments,
                payment -> !payment.isPaid() && payment.getDueDate().getValue().isBefore(today));
        PaymentTotal paid = sumPayments(payments, PaymentObligation::isPaid);
        return "Payments:\n"
                + "  Outstanding: " + outstanding.format() + "\n"
                + "  Of which overdue: " + overdue.format() + "\n"
                + "  Marked paid: " + paid.format();
    }

    private static PaymentTotal sumPayments(List<PaymentObligation> payments,
            Predicate<PaymentObligation> isIncluded) {
        PaymentTotal total = PaymentTotal.EMPTY;
        for (PaymentObligation payment : payments) {
            if (isIncluded.test(payment)) {
                total = total.add(payment.getAmount().getCents());
            }
        }
        return total;
    }

    private static String formatCents(BigInteger cents) {
        BigInteger whole = cents.divide(BigInteger.valueOf(100));
        BigInteger fraction = cents.remainder(BigInteger.valueOf(100)).abs();
        return String.format(Locale.ROOT, "$%,d.%02d", whole, fraction.intValue());
    }

    /** Number of payments in a group and the sum of their amounts in cents. */
    private record PaymentTotal(long count, BigInteger cents) {
        private static final PaymentTotal EMPTY = new PaymentTotal(0, BigInteger.ZERO);

        private PaymentTotal add(BigInteger amountCents) {
            return new PaymentTotal(count + 1, cents.add(amountCents));
        }

        private String format() {
            return count + " (" + formatCents(cents) + ")";
        }
    }
}
