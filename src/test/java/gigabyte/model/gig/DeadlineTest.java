package gigabyte.model.gig;

import static gigabyte.testutil.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

public class DeadlineTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Deadline(null));
    }

    @Test
    public void constructor_invalidDate_throwsIllegalArgumentExceptionWithClearMessage() {
        assertThrows(IllegalArgumentException.class, Deadline.MESSAGE_CONSTRAINTS, () ->
                new Deadline("2025-02-29"));
    }

    @Test
    public void constructor_validDate_storesDate() {
        Deadline deadline = new Deadline("2027-01-31");

        assertEquals(LocalDate.of(2027, 1, 31), deadline.getValue());
        assertEquals("2027-01-31", deadline.toString());
    }

    @Test
    public void isValidDeadline() {
        assertThrows(NullPointerException.class, () -> Deadline.isValidDeadline(null));

        assertFalse(Deadline.isValidDeadline(""));
        assertFalse(Deadline.isValidDeadline("31-01-2027"));
        assertFalse(Deadline.isValidDeadline("2027-1-31"));
        assertFalse(Deadline.isValidDeadline("2027-02-29"));
        assertFalse(Deadline.isValidDeadline("not-a-date"));

        assertTrue(Deadline.isValidDeadline("2024-02-29"));
        assertTrue(Deadline.isValidDeadline("2027-01-31"));
    }
}
