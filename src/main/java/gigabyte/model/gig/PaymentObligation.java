package gigabyte.model.gig;

import static gigabyte.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Objects;

import gigabyte.commons.util.ToStringBuilder;

/** Represents an amount due for a gig. */
public class PaymentObligation {
    private final Gig gig;
    private final Fee amount;
    private final Deadline dueDate;
    private final boolean isPaid;

    /** Creates a payment obligation. */
    public PaymentObligation(Gig gig, Fee amount, Deadline dueDate, boolean isPaid) {
        requireAllNonNull(gig, amount, dueDate);
        this.gig = gig;
        this.amount = amount;
        this.dueDate = dueDate;
        this.isPaid = isPaid;
    }
    public Gig getGig() {
        return gig;
    }
    public Fee getAmount() {
        return amount;
    }
    public Deadline getDueDate() {
        return dueDate;
    }
    public boolean isPaid() {
        return isPaid;
    }

    /** Returns a copy with the payment state changed. */
    public PaymentObligation withPaid(boolean isPaid) {
        return new PaymentObligation(gig, amount, dueDate, isPaid);
    }

    /** Returns a copy with the amount and due date changed. */
    public PaymentObligation withDetails(Fee amount, Deadline dueDate) {
        return new PaymentObligation(gig, amount, dueDate, isPaid);
    }

    /** Returns a copy linked to {@code replacementGig}. */
    public PaymentObligation withGig(Gig replacementGig) {
        return new PaymentObligation(replacementGig, amount, dueDate, isPaid);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof PaymentObligation otherPayment)) {
            return false;
        }
        return gig.equals(otherPayment.gig) && amount.equals(otherPayment.amount)
                && dueDate.equals(otherPayment.dueDate) && isPaid == otherPayment.isPaid;
    }
    @Override
    public int hashCode() {
        return Objects.hash(gig, amount, dueDate, isPaid);
    }
    @Override
    public String toString() {
        return new ToStringBuilder(this).add("gig", gig).add("amount", amount)
                .add("dueDate", dueDate).add("paid", isPaid).toString();
    }
}
