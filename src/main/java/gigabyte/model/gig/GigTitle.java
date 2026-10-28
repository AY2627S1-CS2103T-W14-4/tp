package gigabyte.model.gig;

import static gigabyte.commons.util.AppUtil.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Represents a short descriptive title for a gig.
 * Guarantees: immutable; normalized with surrounding whitespace removed; contains 1–100 characters.
 */
public class GigTitle {
    public static final String MESSAGE_CONSTRAINTS =
            "Gig titles must contain at least one non-whitespace character and must not exceed 100 characters";

    private static final int MAX_LENGTH = 100;

    /**
     * Matches one whitespace character: anything {@link String#strip()} removes ({@code javaWhitespace}), plus the
     * Unicode whitespace it keeps ({@code IsWhite_Space}), such as the non-breaking space and U+0085 (next line).
     */
    private static final String WHITESPACE = "[\\p{javaWhitespace}\\p{IsWhite_Space}]";

    private static final Pattern SURROUNDING_WHITESPACE =
            Pattern.compile("^" + WHITESPACE + "+|" + WHITESPACE + "+$");

    private final String value;

    /**
     * Constructs a {@code GigTitle}.
     *
     * @param title A short descriptive title.
     */
    public GigTitle(String title) {
        requireNonNull(title);
        String normalizedTitle = normalize(title);
        checkArgument(isValidTitle(normalizedTitle), MESSAGE_CONSTRAINTS);
        value = normalizedTitle;
    }

    /** Returns whether {@code test} is a valid title after surrounding whitespace is removed. */
    public static boolean isValidTitle(String test) {
        if (test == null) {
            return false;
        }
        String normalizedTitle = normalize(test);
        return !normalizedTitle.isEmpty() && normalizedTitle.length() <= MAX_LENGTH;
    }

    /** Returns {@code title} with surrounding whitespace, including non-breaking spaces, removed. */
    private static String normalize(String title) {
        return SURROUNDING_WHITESPACE.matcher(title).replaceAll("");
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
