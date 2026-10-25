package gigabyte.model.tag;

import static gigabyte.testutil.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TagTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Tag(null));
    }

    @Test
    public void constructor_invalidTagName_throwsIllegalArgumentException() {
        String invalidTagName = "";
        assertThrows(IllegalArgumentException.class, () -> new Tag(invalidTagName));
    }

    @Test
    public void isValidTagName() {
        // null tag name
        assertThrows(NullPointerException.class, () -> Tag.isValidTagName(null));
    }

    @Test
    public void isWithinLengthLimit() {
        // null tag name
        assertThrows(NullPointerException.class, () -> Tag.isWithinLengthLimit(null));

        // within limit
        assertTrue(Tag.isWithinLengthLimit(""));
        assertTrue(Tag.isWithinLengthLimit("friend"));
        assertTrue(Tag.isWithinLengthLimit("a".repeat(Tag.MAX_LENGTH))); // exactly at limit

        // over limit
        assertFalse(Tag.isWithinLengthLimit("a".repeat(Tag.MAX_LENGTH + 1)));
    }

    @Test
    public void constructor_tagOverLengthLimit_success() {
        // Length is only enforced on user input, so tags from older data files still load.
        new Tag("a".repeat(Tag.MAX_LENGTH + 1));
    }

}
