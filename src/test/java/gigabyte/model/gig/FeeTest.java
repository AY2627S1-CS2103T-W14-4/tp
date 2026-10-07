package gigabyte.model.gig;

import static gigabyte.testutil.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

public class FeeTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Fee(null));
    }

    @Test
    public void constructor_invalidFee_throwsIllegalArgumentExceptionWithClearMessage() {
        assertThrows(IllegalArgumentException.class, Fee.MESSAGE_CONSTRAINTS, () ->
                new Fee("0"));
        assertThrows(IllegalArgumentException.class, Fee.MESSAGE_CONSTRAINTS, () ->
                new Fee("-10"));
        assertThrows(IllegalArgumentException.class, Fee.MESSAGE_CONSTRAINTS, () ->
                new Fee("10.999"));
        assertThrows(IllegalArgumentException.class, Fee.MESSAGE_CONSTRAINTS, () ->
                new Fee("ten dollars"));
    }

    @Test
    public void constructor_validFee_storesNormalisedAmount() {
        Fee fee = new Fee("1250.5");

        assertEquals(new BigDecimal("1250.50"), fee.getValue());
        assertEquals("1250.50", fee.toString());
    }

    @Test
    public void isValidFee() {
        assertThrows(NullPointerException.class, () -> Fee.isValidFee(null));

        assertFalse(Fee.isValidFee(""));
        assertFalse(Fee.isValidFee("0"));
        assertFalse(Fee.isValidFee("-1.00"));
        assertFalse(Fee.isValidFee("1.234"));
        assertFalse(Fee.isValidFee("$100"));
        assertFalse(Fee.isValidFee("1e3"));

        assertTrue(Fee.isValidFee("0.01"));
        assertTrue(Fee.isValidFee("100"));
        assertTrue(Fee.isValidFee("100.5"));
        assertTrue(Fee.isValidFee("100.50"));
    }
}
