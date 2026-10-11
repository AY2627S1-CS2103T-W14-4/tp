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
        assertThrows(IllegalArgumentException.class, () -> new GigTitle(""));
        assertThrows(IllegalArgumentException.class, () -> new GigTitle("   \t  "));
    }

    @Test
    public void constructor_titleLongerThan100Characters_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new GigTitle("a".repeat(101)));
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
        assertFalse(GigTitle.isValidTitle(null));
        assertFalse(GigTitle.isValidTitle(""));
        assertFalse(GigTitle.isValidTitle("   "));
        assertFalse(GigTitle.isValidTitle("a".repeat(101)));
        assertTrue(GigTitle.isValidTitle("a"));
        assertTrue(GigTitle.isValidTitle("a".repeat(100)));
        assertTrue(GigTitle.isValidTitle("  Client website: phase 2 (mobile)  "));
    }

    @Test
    public void equals() {
        GigTitle title = new GigTitle("Website redesign");

        assertEquals(title, new GigTitle("Website redesign"));
        assertEquals(title, new GigTitle("  Website redesign  "));
        assertFalse(title.equals(new GigTitle("Logo design")));
        assertFalse(title.equals(null));
        assertFalse(title.equals("Website redesign"));
        assertEquals(title.hashCode(), new GigTitle("Website redesign").hashCode());
    }
}
