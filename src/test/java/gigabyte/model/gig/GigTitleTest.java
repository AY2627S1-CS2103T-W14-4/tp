package gigabyte.model.gig;

import static gigabyte.testutil.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class GigTitleTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new GigTitle(null));
    }

    @Test
    public void constructor_blank_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new GigTitle("")); // Boundary value
        assertThrows(IllegalArgumentException.class, () -> new GigTitle("   \t  ")); // Whitespace only
    }

    @Test
    public void constructor_titleLongerThan100Characters_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new GigTitle("a".repeat(101))); // Boundary value
    }

    @Test
    public void constructor_validTitle_normalizesSurroundingWhitespace() {
        GigTitle title = new GigTitle("  Website redesign — phase 2!  ");

        assertEquals("Website redesign — phase 2!", title.toString());
    }

    @Test
    public void constructor_surroundingWhitespaceWith100NormalizedCharacters_acceptsAndNormalizes() {
        GigTitle title = new GigTitle("  " + "a".repeat(100) + "  ");

        assertEquals("a".repeat(100), title.toString());
    }

    @Test
    public void isValidTitle() {
        // EP: null title
        assertFalse(GigTitle.isValidTitle(null));

        // EP: blank title
        assertFalse(GigTitle.isValidTitle("")); // Boundary value
        assertFalse(GigTitle.isValidTitle("   "));

        // EP: title longer than 100 characters
        assertFalse(GigTitle.isValidTitle("a".repeat(101))); // Boundary value

        // EP: 1 to 100 characters, should return true
        assertTrue(GigTitle.isValidTitle("a")); // Boundary value
        assertTrue(GigTitle.isValidTitle("a".repeat(100))); // Boundary value
        assertTrue(GigTitle.isValidTitle("  Client website: phase 2 (mobile)  ")); // Symbols and surrounding spaces
    }

    @Test
    public void equals() {
        GigTitle title = new GigTitle("Website redesign");

        // same value, ignoring surrounding whitespace -> returns true
        assertEquals(title, new GigTitle("Website redesign"));
        assertEquals(title, new GigTitle("  Website redesign  "));

        // different value -> returns false
        assertFalse(title.equals(new GigTitle("Logo design")));

        // null or a different type -> returns false
        assertFalse(title.equals(null));
        assertFalse(title.equals("Website redesign"));

        assertEquals(title.hashCode(), new GigTitle("Website redesign").hashCode());
    }
}
