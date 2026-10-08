package gigabyte.model.gig;

import static gigabyte.commons.util.AppUtil.checkArgument;
import static java.util.Objects.requireNonNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

/**
 * Represents an agreed gig fee.
 * Guarantees: immutable; value is positive and has at most two decimal places.
 */
public class Fee {
    public static final String MESSAGE_CONSTRAINTS =
            "Fees must be positive numbers with at most two decimal places";

    private static final String VALIDATION_REGEX = "\\d+(\\.\\d{1,2})?";

    private final BigDecimal value;

    /**
     * Constructs a {@code Fee}.
     *
     * @param fee A positive decimal amount.
     */
    public Fee(String fee) {
        requireNonNull(fee);
        checkArgument(isValidFee(fee), MESSAGE_CONSTRAINTS);
        value = new BigDecimal(fee).setScale(2);
    }

    public BigDecimal getValue() {
        return value;
    }

    /** Returns this fee as an exact integer count of cents. */
    public BigInteger getCents() {
        return value.movePointRight(2).toBigIntegerExact();
    }

    /** Creates a fee from a positive integer count of cents. */
    public static Fee fromCents(BigInteger cents) {
        requireNonNull(cents);
        checkArgument(cents.signum() > 0, MESSAGE_CONSTRAINTS);
        return new Fee(new BigDecimal(cents, 2).toPlainString());
    }

    /**
     * Returns whether {@code test} is a valid fee.
     */
    public static boolean isValidFee(String test) {
        requireNonNull(test);

        if (!test.matches(VALIDATION_REGEX)) {
            return false;
        }

        return new BigDecimal(test).compareTo(BigDecimal.ZERO) > 0;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof Fee otherFee)) {
            return false;
        }

        return value.equals(otherFee.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value.toPlainString();
    }
}
