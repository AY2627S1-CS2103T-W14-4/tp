package gigabyte.model.gig;

import java.util.Comparator;
import java.util.List;

/** Provides the display order for payment obligations. */
public final class PaymentObligationOrdering {
    private PaymentObligationOrdering() {}

    /**
     * Returns the payment panel order: due date first, then the payment's position in the source list.
     */
    public static Comparator<PaymentObligation> forSourceList(List<PaymentObligation> source) {
        return Comparator.comparing((PaymentObligation payment) -> payment.getDueDate().getValue())
                .thenComparingInt(payment -> getSourceIndex(source, payment));
    }

    /** Returns the obligations belonging to {@code gig} in the same order as the payment panel. */
    public static List<PaymentObligation> forGig(List<PaymentObligation> source, Gig gig) {
        return source.stream()
                .filter(payment -> isForGig(payment, gig))
                .sorted(forSourceList(source))
                .toList();
    }

    /** Returns whether {@code payment} belongs to {@code gig}. */
    public static boolean isForGig(PaymentObligation payment, Gig gig) {
        return gig != null && payment.getGig().hasSameUid(gig);
    }

    private static int getSourceIndex(List<PaymentObligation> source, PaymentObligation target) {
        for (int index = 0; index < source.size(); index++) {
            if (source.get(index) == target) {
                return index;
            }
        }
        return Integer.MAX_VALUE;
    }
}
