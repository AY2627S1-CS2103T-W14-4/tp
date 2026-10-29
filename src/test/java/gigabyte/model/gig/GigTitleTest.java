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
        assertThrows(IllegalArgumentException.class, () -> new GigTitle("\u00A0")); // Non-breaking space only
        assertThrows(IllegalArgumentException.class, () -> new GigTitle("\u0085")); // Next line only
        assertThrows(IllegalArgumentException.class, () -> new GigTitle("\u001F")); // Unit separator only
    }

    @Test
    public void constructor_titleLongerThan100CodePoints_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new GigTitle("a".repeat(101))); // Boundary value
    }

    @Test
    public void constructor_emojiCountedAsSingleCharacters_accepts100CodePoints() {
        String titleValue = "😀".repeat(100);

        assertEquals(titleValue, new GigTitle(titleValue).toString());
    }

    @Test
    public void constructor_titleExceeding100CodePoints_rejectsEmojiTitle() {
        assertThrows(IllegalArgumentException.class, () -> new GigTitle("😀".repeat(101)));
    }

    @Test
    public void isValidTitle_skinToneEmojiCountedAsTwoCodePoints() {
        String titleAtLimit = "👍🏽".repeat(50);
        String titleOverLimit = "👍🏽".repeat(51);

        assertTrue(GigTitle.isValidTitle(titleAtLimit));
        assertFalse(GigTitle.isValidTitle(titleOverLimit));
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
    public void constructor_surroundingUnicodeSpaces_removesThemAndKeepsInternalOnes() {
        GigTitle title = new GigTitle("\u00A0\u2007\u0085\u001C Logo\u00A0design \u202F\u001F");

        assertEquals("Logo\u00A0design", title.toString());
    }

    @Test
    public void isValidTitle() {
        // EP: null title
        assertFalse(GigTitle.isValidTitle(null));

        // EP: blank title
        assertFalse(GigTitle.isValidTitle("")); // Boundary value
        assertFalse(GigTitle.isValidTitle("   "));

        // EP: only Unicode spaces that String#strip keeps, such as the non-breaking space
        assertFalse(GigTitle.isValidTitle("\u00A0"));
        assertFalse(GigTitle.isValidTitle(" \u00A0\u2007\u202F\t"));
        assertFalse(GigTitle.isValidTitle("\u0085")); // Next line

        // EP: only control separators that String#strip removes (regression)
        assertFalse(GigTitle.isValidTitle("\u001C\u001D\u001E\u001F"));

        // EP: title longer than 100 Unicode code points
        assertFalse(GigTitle.isValidTitle("a".repeat(101))); // Boundary value

        // EP: 1 to 100 Unicode code points, should return true
        assertTrue(GigTitle.isValidTitle("a")); // Boundary value
        assertTrue(GigTitle.isValidTitle("a".repeat(100))); // Boundary value
        assertTrue(GigTitle.isValidTitle("😀".repeat(100))); // Supplementary Unicode characters
        assertTrue(GigTitle.isValidTitle("  Client website: phase 2 (mobile)  ")); // Symbols and surrounding spaces
        assertTrue(GigTitle.isValidTitle("\u00A0" + "a".repeat(100) + "\u00A0")); // Boundary value
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
