package gigabyte.model.client;

import static gigabyte.testutil.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class PhoneTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Phone(null));
    }

    @Test
    public void constructor_invalidPhone_throwsIllegalArgumentException() {
        String invalidPhone = "";
        assertThrows(IllegalArgumentException.class, () -> new Phone(invalidPhone));
    }

    @Test
    public void constructor_validPhone_storesValueAsTyped() {
        assertEquals("+65 9123 4567", new Phone("+65 9123 4567").value);
        assertEquals("6123-4567", new Phone("6123-4567").value);
    }

    @Test
    public void isValidPhone() {
        // null phone number
        assertThrows(NullPointerException.class, () -> Phone.isValidPhone(null));

        // EP: empty or blank -> invalid
        assertFalse(Phone.isValidPhone("")); // empty string
        assertFalse(Phone.isValidPhone(" ")); // spaces only

        // EP: fewer than 3 digits -> invalid
        assertFalse(Phone.isValidPhone("12")); // boundary: 2 digits
        assertFalse(Phone.isValidPhone("1-2")); // boundary: 2 digits with a separator
        assertFalse(Phone.isValidPhone("+12")); // boundary: 2 digits with a '+'
        assertFalse(Phone.isValidPhone("+")); // '+' only, no digits

        // EP: letters or other symbols -> invalid
        assertFalse(Phone.isValidPhone("phone")); // letters only
        assertFalse(Phone.isValidPhone("91a34567")); // letter within digits
        assertFalse(Phone.isValidPhone("(65) 9123 4567")); // brackets
        assertFalse(Phone.isValidPhone("9123.4567")); // full stop
        assertFalse(Phone.isValidPhone("9123_4567")); // underscore

        // EP: '+' not at the start, or more than one '+' -> invalid
        assertFalse(Phone.isValidPhone("65+91234567")); // '+' in the middle
        assertFalse(Phone.isValidPhone("++6591234567")); // two leading '+'

        // EP: leading or trailing separator -> invalid
        assertFalse(Phone.isValidPhone("--123")); // leading hyphens
        assertFalse(Phone.isValidPhone("-123")); // leading hyphen
        assertFalse(Phone.isValidPhone("123-")); // trailing hyphen
        assertFalse(Phone.isValidPhone("+ 65 9123 4567")); // separator right after '+'
        assertFalse(Phone.isValidPhone("+-65")); // separator right after '+'

        // EP: two separators in a row -> invalid
        assertFalse(Phone.isValidPhone("9123  4567")); // two spaces
        assertFalse(Phone.isValidPhone("9123--4567")); // two hyphens
        assertFalse(Phone.isValidPhone("9123 -4567")); // space then hyphen

        // EP: digits only -> valid
        assertTrue(Phone.isValidPhone("911")); // boundary: exactly 3 digits
        assertTrue(Phone.isValidPhone("91234567"));
        assertTrue(Phone.isValidPhone("124293842033123")); // long phone number

        // EP: leading '+' then digits -> valid
        assertTrue(Phone.isValidPhone("+123")); // boundary: '+' with exactly 3 digits
        assertTrue(Phone.isValidPhone("+6591234567"));

        // EP: digits grouped by single spaces or hyphens -> valid
        assertTrue(Phone.isValidPhone("1-23")); // boundary: 3 digits split by a separator
        assertTrue(Phone.isValidPhone("6123-4567")); // hyphen
        assertTrue(Phone.isValidPhone("9123 4567")); // space
        assertTrue(Phone.isValidPhone("+65 9123 4567")); // '+' with spaces
        assertTrue(Phone.isValidPhone("+1-202-555-0143")); // '+' with hyphens
        assertTrue(Phone.isValidPhone("+65 9123-4567")); // mixed separators
    }

    @Test
    public void equals() {
        Phone phone = new Phone("999");

        // same values -> returns true
        assertTrue(phone.equals(new Phone("999")));

        // same object -> returns true
        assertTrue(phone.equals(phone));

        // null -> returns false
        assertFalse(phone.equals(null));

        // different types -> returns false
        assertFalse(phone.equals(5.0f));

        // different values -> returns false
        assertFalse(phone.equals(new Phone("995")));

        // same digits, different formatting -> returns false, as numbers are stored as typed
        assertFalse(new Phone("+65 9123 4567").equals(new Phone("+6591234567")));
    }
}
