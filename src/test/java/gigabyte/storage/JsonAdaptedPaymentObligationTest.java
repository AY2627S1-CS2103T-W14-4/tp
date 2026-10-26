package gigabyte.storage;

import static gigabyte.testutil.Assert.assertThrows;
import static gigabyte.testutil.TypicalClients.ALICE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigInteger;
import java.util.List;

import org.junit.jupiter.api.Test;

import gigabyte.commons.exceptions.IllegalValueException;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.GigStatus;
import gigabyte.model.gig.GigTitle;
import gigabyte.model.gig.PaymentObligation;
import gigabyte.model.gig.exceptions.GigNotFoundException;

public class JsonAdaptedPaymentObligationTest {
    private static final Gig GIG = new Gig(ALICE, new GigTitle("Website redesign"), GigStatus.IN_PROGRESS,
            new Deadline("2027-01-31"), new Fee("100"));

    @Test
    public void toModelType_validFields_preservesExactGigAndPaymentState() throws Exception {
        Gig equalGig = new Gig(ALICE, GIG.getTitle(), GIG.getStatus(), GIG.getDeadline(), GIG.getAgreedFee());
        List<Gig> gigs = List.of(GIG, equalGig);
        for (boolean isPaid : new boolean[] {false, true}) {
            PaymentObligation original = new PaymentObligation(equalGig, new Fee("50.25"),
                    new Deadline("2027-02-28"), isPaid);
            PaymentObligation restored = new JsonAdaptedPaymentObligation(original, gigs).toModelType(gigs);
            assertEquals(original, restored);
            assertSame(equalGig, restored.getGig());
        }
    }

    @Test
    public void toModelType_missingFields_throwsIllegalValueException() {
        for (JsonAdaptedPaymentObligation obligation : List.of(
                new JsonAdaptedPaymentObligation(null, BigInteger.valueOf(5000), "2027-01-31", false), // gig UID
                new JsonAdaptedPaymentObligation(GIG.getUid().toString(), null, "2027-01-31", false), // amount
                new JsonAdaptedPaymentObligation(GIG.getUid().toString(), BigInteger.valueOf(5000), null,
                        false), // due date
                new JsonAdaptedPaymentObligation(GIG.getUid().toString(), BigInteger.valueOf(5000),
                        "2027-01-31", null))) { // paid state
            assertThrows(IllegalValueException.class, JsonAdaptedPaymentObligation.MISSING_FIELD_MESSAGE, () ->
                    obligation.toModelType(List.of(GIG)));
        }
    }

    @Test
    public void toModelType_invalidOrUnknownGigUid_throwsIllegalValueException() {
        // EP: gig UID is not a valid UUID
        assertGigUidRejected("invalid");

        // EP: valid UUID that matches no stored gig
        assertGigUidRejected("00000000-0000-0000-0000-000000000001");
    }

    @Test
    public void toModelType_invalidAmountOrDueDate_throwsIllegalValueException() {
        // EP: zero amount
        assertThrows(IllegalValueException.class, Fee.MESSAGE_CONSTRAINTS, () ->
                new JsonAdaptedPaymentObligation(GIG.getUid().toString(), BigInteger.ZERO,
                        "2027-01-31", false).toModelType(List.of(GIG))); // Boundary value

        // EP: due date that does not exist
        assertThrows(IllegalValueException.class, Deadline.MESSAGE_CONSTRAINTS, () ->
                new JsonAdaptedPaymentObligation(GIG.getUid().toString(), BigInteger.valueOf(5000),
                        "2027-02-29", false).toModelType(List.of(GIG))); // Not a leap year
    }

    private void assertGigUidRejected(String gigUid) {
        assertThrows(IllegalValueException.class, GigNotFoundException.MESSAGE, () ->
                new JsonAdaptedPaymentObligation(gigUid, BigInteger.valueOf(5000),
                        "2027-01-31", false).toModelType(List.of(GIG)));
    }
}
