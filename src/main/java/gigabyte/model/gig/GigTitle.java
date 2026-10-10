package gigabyte.model.gig;

import static gigabyte.commons.util.AppUtil.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.Objects;

/**
 * Represents a short descriptive title for a gig.
 * Guarantees: immutable; normalized with surrounding whitespace removed; contains 1–100 characters.
 */
public class GigTitle {
    public static final String MESSAGE_CONSTRAINTS =
            "Gig titles must contain at least one non-whitespace character and must not exceed 100 characters";
    public static final GigTitle UNTITLED = new GigTitle("Untitled gig");

    private static final int MAX_LENGTH = 100;

    private final String value;

    /**
     * Constructs a {@code GigTitle}.
     *
     * @param title A short descriptive title.
     */
    public GigTitle(String title) {
        requireNonNull(title);
        String normalizedTitle = title.strip();
        checkArgument(isValidTitle(normalizedTitle), MESSAGE_CONSTRAINTS);
        value = normalizedTitle;
    }

    /** Returns whether {@code test} is a valid title after surrounding whitespace is removed. */
    public static boolean isValidTitle(String test) {
        if (test == null) {
            return false;
        }
        String normalizedTitle = test.strip();
        return !normalizedTitle.isEmpty() && normalizedTitle.length() <= MAX_LENGTH;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof GigTitle otherTitle)) {
            return false;
        }
        return value.equals(otherTitle.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
