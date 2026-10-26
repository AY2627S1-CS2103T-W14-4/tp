package gigabyte.model.tag;

import static gigabyte.commons.util.AppUtil.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Represents a Tag in the client list.
 * Guarantees: immutable; name is valid as declared in {@link #isValidTagName(String)}.
 * Tags entered by the user must also satisfy {@link #isWithinLengthLimit(String)}. That limit is not applied
 * when loading the data file, so an older file with a longer tag still loads instead of being discarded.
 */
public class Tag {

    public static final int MAX_LENGTH = 25;
    public static final String MESSAGE_CONSTRAINTS =
            "Tag names should be alphanumeric and at most " + MAX_LENGTH + " characters long";
    public static final String VALIDATION_REGEX = "\\p{Alnum}+";

    public final String tagName;

    /**
     * Constructs a {@code Tag}.
     *
     * @param tagName A valid tag name.
     */
    public Tag(String tagName) {
        requireNonNull(tagName);
        checkArgument(isValidTagName(tagName), MESSAGE_CONSTRAINTS);
        this.tagName = tagName;
    }

    /**
     * Returns true if a given string is a valid tag name.
     */
    public static boolean isValidTagName(String test) {
        return test.matches(VALIDATION_REGEX);
    }

    /**
     * Returns true if a given string has at most {@link #MAX_LENGTH} characters.
     */
    public static boolean isWithinLengthLimit(String test) {
        return test.length() <= MAX_LENGTH;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof Tag otherTag)) {
            return false;
        }

        return tagName.equals(otherTag.tagName);
    }

    @Override
    public int hashCode() {
        return tagName.hashCode();
    }

    /**
     * Formats state as text for viewing.
     */
    public String toString() {
        return '[' + tagName + ']';
    }

}
