package gigabyte.model.gig;

import static gigabyte.testutil.Assert.assertThrows;
import static gigabyte.testutil.TypicalClients.ALICE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class PaymentObligationTest {
    private static final Gig GIG = new Gig(ALICE, GigStatus.IN_PROGRESS,
            new Deadline("2026-12-31"), new Fee("100"));
    private static final Fee AMOUNT = new Fee("50");
    private static final Deadline DUE_DATE = new Deadline("2026-11-30");

    @Test
    public void constructor_nullField_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new PaymentObligation(null, AMOUNT, DUE_DATE, false));
        assertThrows(NullPointerException.class, () -> new PaymentObligation(GIG, null, DUE_DATE, false));
        assertThrows(NullPointerException.class, () -> new PaymentObligation(GIG, AMOUNT, null, false));
    }

    @Test
    public void paymentState_canBeChanged() {
        PaymentObligation obligation = new PaymentObligation(GIG, AMOUNT, DUE_DATE, false);
        assertFalse(obligation.isPaid());
        assertTrue(obligation.withPaid(true).isPaid());
    }
}
