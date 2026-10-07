package gigabyte.model.gig;

import static gigabyte.commons.util.AppUtil.checkArgument;
import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/**
 * Represents a gig deadline.
 * Guarantees: immutable; value is a valid ISO calendar date.
 */
public class Deadline {
    public static final String MESSAGE_CONSTRAINTS =
            "Deadlines must use yyyy-MM-dd and be valid calendar dates";

    private static final String VALIDATION_REGEX = "\\d{4}-\\d{2}-\\d{2}";

    private final LocalDate value;

    /**
     * Constructs a {@code Deadline}.
     *
     * @param deadline A date in yyyy-MM-dd format.
     */
    public Deadline(String deadline) {
        requireNonNull(deadline);
        checkArgument(isValidDeadline(deadline), MESSAGE_CONSTRAINTS);
        value = LocalDate.parse(deadline, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public LocalDate getValue() {
        return value;
    }

    /**
     * Returns whether {@code test} is a valid deadline.
     */
    public static boolean isValidDeadline(String test) {
        requireNonNull(test);

        if (!test.matches(VALIDATION_REGEX)) {
            return false;
        }

        try {
            LocalDate.parse(test, DateTimeFormatter.ISO_LOCAL_DATE);
            return true;
        } catch (DateTimeParseException exception) {
            return false;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof Deadline otherDeadline)) {
            return false;
        }

        return value.equals(otherDeadline.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
