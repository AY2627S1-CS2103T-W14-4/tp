package gigabyte.model.client;

import static gigabyte.commons.util.AppUtil.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.Locale;

/**
 * Represents a Client's name in the client list.
 * Guarantees: immutable; is valid as declared in {@link #isValidName(String)}
 */
public class Name {

    public static final String MESSAGE_CONSTRAINTS =
            "Names should only contain alphanumeric characters and spaces, and should not be blank";

    /*
     * The first character of the name must not be a whitespace,
     * otherwise " " (a blank string) becomes a valid input.
     */
    public static final String VALIDATION_REGEX = "[\\p{Alnum}][\\p{Alnum} ]*";

    public final String fullName;

    /**
     * Constructs a {@code Name}.
     *
     * @param name A valid name.
     */
    public Name(String name) {
        requireNonNull(name);
        checkArgument(isValidName(name), MESSAGE_CONSTRAINTS);
        fullName = name;
    }

    /**
     * Returns true if a given string is a valid name.
     */
    public static boolean isValidName(String test) {
        return test.matches(VALIDATION_REGEX);
    }

    /**
     * Returns this name in lower case, with leading and trailing spaces removed
     * and repeated internal spaces collapsed into one.
     * Used to detect names that differ only in case or spacing.
     */
    public String getNormalizedName() {
        return fullName.trim().replaceAll(" +", " ").toLowerCase(Locale.ROOT);
    }

    /**
     * Returns whether both normalized names have at least five characters and differ by exactly one
     * insertion, deletion, replacement, or adjacent swap. Exact normalized matches are handled separately.
     * Runs in linear time without changing client identity or rejecting either name.
     */
    public boolean isSimilarTo(Name other) {
        requireNonNull(other);
        String first = getNormalizedName();
        String second = other.getNormalizedName();
        int shorterLength = Math.min(first.length(), second.length());
        if (shorterLength < 5 || Math.abs(first.length() - second.length()) > 1 || first.equals(second)) {
            return false;
        }

        int mismatch = 0;
        while (mismatch < shorterLength && first.charAt(mismatch) == second.charAt(mismatch)) {
            mismatch++;
        }
        if (first.length() != second.length()) {
            String shorter = first.length() < second.length() ? first : second;
            String longer = first.length() < second.length() ? second : first;
            return shorter.regionMatches(mismatch, longer, mismatch + 1, shorterLength - mismatch);
        }
        return first.regionMatches(mismatch + 1, second, mismatch + 1, first.length() - mismatch - 1)
                || mismatch + 1 < first.length()
                && first.charAt(mismatch) == second.charAt(mismatch + 1)
                && first.charAt(mismatch + 1) == second.charAt(mismatch)
                && first.regionMatches(mismatch + 2, second, mismatch + 2, first.length() - mismatch - 2);
    }

    @Override
    public String toString() {
        return fullName;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof Name otherName)) {
            return false;
        }

        return fullName.equals(otherName.fullName);
    }

    @Override
    public int hashCode() {
        return fullName.hashCode();
    }

}
