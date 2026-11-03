package gigabyte.model.client;

import static gigabyte.commons.util.AppUtil.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Represents a Client's phone number in the client list.
 * Guarantees: immutable; is valid as declared in {@link #isValidPhone(String)}.
 * Phone numbers entered by the user must also satisfy {@link #isWithinDigitLimit(String)}. That limit is not applied
 * when loading the data file, so an older file with a longer number still loads instead of being discarded.
 */
public class Phone {

    /** The most digits an international number can have under the ITU-T E.164 standard. */
    public static final int MAX_DIGITS = 15;
    public static final String MESSAGE_CONSTRAINTS =
            "Phone numbers should have at least 3 and at most " + MAX_DIGITS + " digits, and may start with a '+'. "
            + "Digits may be grouped with single spaces or hyphens, e.g. +65 9123 4567 or 6123-4567. "
            + "No other characters are allowed.";

    /*
     * The lookahead requires at least 3 digits in total. The rest allows an optional leading '+',
     * then groups of digits joined by a single space or hyphen.
     */
    public static final String VALIDATION_REGEX = "(?=(?:\\D*\\d){3})\\+?\\d+(?:[ -]\\d+)*";
    public final String value;

    /**
     * Constructs a {@code Phone}.
     *
     * @param phone A valid phone number.
     */
    public Phone(String phone) {
        requireNonNull(phone);
        checkArgument(isValidPhone(phone), MESSAGE_CONSTRAINTS);
        value = phone;
    }

    /**
     * Returns true if a given string is a valid phone number.
     */
    public static boolean isValidPhone(String test) {
        return test.matches(VALIDATION_REGEX);
    }

    /**
     * Returns true if a given string has at most {@link #MAX_DIGITS} digits.
     * Other characters, such as '+', spaces and hyphens, are not counted.
     */
    public static boolean isWithinDigitLimit(String test) {
        return test.replaceAll("\\D", "").length() <= MAX_DIGITS;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof Phone otherPhone)) {
            return false;
        }

        return value.equals(otherPhone.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

}
