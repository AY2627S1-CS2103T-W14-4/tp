package gigabyte.model.gig;

import static gigabyte.testutil.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;

import org.junit.jupiter.api.Test;

public class FeeTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Fee(null));
    }

    @Test
    public void constructor_invalidFee_throwsIllegalArgumentExceptionWithClearMessage() {
        // EP: zero or negative fee
        assertThrows(IllegalArgumentException.class, Fee.MESSAGE_CONSTRAINTS, () -> new Fee("0")); // Boundary value
        assertThrows(IllegalArgumentException.class, Fee.MESSAGE_CONSTRAINTS, () -> new Fee("-10"));

        // EP: more than two decimal places
        assertThrows(IllegalArgumentException.class, Fee.MESSAGE_CONSTRAINTS, () -> new Fee("10.999"));

        // EP: not a number
        assertThrows(IllegalArgumentException.class, Fee.MESSAGE_CONSTRAINTS, () -> new Fee("ten dollars"));
    }

    @Test
    public void constructor_validFee_storesNormalizedAmount() {
        Fee fee = new Fee("1250.5");

        assertEquals(new BigDecimal("1250.50"), fee.getValue());
        assertEquals("1250.50", fee.toString());
    }

    @Test
    public void toDisplayString_formatsExactAmountsWithoutChangingPlainValues() {
        String[][] amounts = {
            {"0.01", "$0.01"},
            {"999.99", "$999.99"},
            {"1000.00", "$1,000.00"},
            {"1250.50", "$1,250.50"},
            {"999999999999.99", "$999,999,999,999.99"},
            {"99999999999999999999999999.99", "$99,999,999,999,999,999,999,999,999.99"}
        };
        for (String[] amount : amounts) {
            Fee fee = new Fee(amount[0]);
            BigInteger cents = fee.getCents();
            assertEquals(amount[1], fee.toDisplayString());
            assertEquals(amount[0], fee.toString());
            assertEquals(cents, fee.getCents());
        }
    }

    @Test
    public void formatCents_zeroAndSmallTotals_usesTwoDecimalPlaces() {
        assertEquals("$0.00", Fee.formatCents(BigInteger.ZERO));
        assertEquals("$0.01", Fee.formatCents(BigInteger.ONE));
        assertEquals("$0.10", Fee.formatCents(BigInteger.TEN));
        assertThrows(NullPointerException.class, () -> Fee.formatCents(null));
    }

    @Test
    public void toDisplayString_otherDefaultLocale_keepsDollarCommaAndDecimalPoint() {
        Locale original = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.GERMANY);
            assertEquals("$1,234.50", new Fee("1234.5").toDisplayString());
            assertEquals("$0.00", Fee.formatCents(BigInteger.ZERO));
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, original);
        }
    }

    @Test
    public void isValidFee() {
        // EP: null fee
        assertThrows(NullPointerException.class, () -> Fee.isValidFee(null));

        // EP: empty fee
        assertFalse(Fee.isValidFee(""));

        // EP: zero or negative fee
        assertFalse(Fee.isValidFee("0")); // Boundary value
        assertFalse(Fee.isValidFee("-1.00"));

        // EP: more than two decimal places
        assertFalse(Fee.isValidFee("1.234"));

        // EP: not a plain decimal number
        assertFalse(Fee.isValidFee("$100"));
        assertFalse(Fee.isValidFee("1e3"));

        // EP: positive amount with at most two decimal places, should return true
        assertTrue(Fee.isValidFee("0.01")); // Boundary value: smallest valid fee
        assertTrue(Fee.isValidFee("100"));
        assertTrue(Fee.isValidFee("100.5"));
        assertTrue(Fee.isValidFee("100.50"));
    }
}
