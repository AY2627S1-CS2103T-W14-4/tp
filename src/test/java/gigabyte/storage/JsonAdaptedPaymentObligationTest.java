package gigabyte.storage;

import static gigabyte.testutil.Assert.assertThrows;
import static gigabyte.testutil.TypicalClients.ALICE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

import gigabyte.commons.exceptions.IllegalValueException;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.GigStatus;
import gigabyte.model.gig.PaymentObligation;
import gigabyte.model.gig.exceptions.GigNotFoundException;

public class JsonAdaptedPaymentObligationTest {
    private static final Gig GIG = new Gig(ALICE, GigStatus.IN_PROGRESS,
            new Deadline("2027-01-31"), new Fee("100"));

    @Test
    public void toModelType_validFields_preservesExactGigAndPaymentState() throws Exception {
        Gig equalGig = new Gig(ALICE, GIG.getStatus(), GIG.getDeadline(), GIG.getAgreedFee());
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
                new JsonAdaptedPaymentObligation(null, "50", "2027-01-31", false),
                new JsonAdaptedPaymentObligation(1, null, "2027-01-31", false),
                new JsonAdaptedPaymentObligation(1, "50", null, false),
                new JsonAdaptedPaymentObligation(1, "50", "2027-01-31", null))) {
            assertThrows(IllegalValueException.class, JsonAdaptedPaymentObligation.MISSING_FIELD_MESSAGE, () ->
                    obligation.toModelType(List.of(GIG)));
        }
    }

    @Test
    public void toModelType_invalidGigIndex_throwsIllegalValueException() {
        for (int index : new int[] {-1, 0, 2}) {
            assertThrows(IllegalValueException.class, GigNotFoundException.MESSAGE, () ->
                    new JsonAdaptedPaymentObligation(index, "50", "2027-01-31", false).toModelType(List.of(GIG)));
        }
    }

    @Test
    public void toModelType_invalidAmountOrDueDate_throwsIllegalValueException() {
        assertThrows(IllegalValueException.class, Fee.MESSAGE_CONSTRAINTS, () ->
                new JsonAdaptedPaymentObligation(1, "0", "2027-01-31", false).toModelType(List.of(GIG)));
        assertThrows(IllegalValueException.class, Deadline.MESSAGE_CONSTRAINTS, () ->
                new JsonAdaptedPaymentObligation(1, "50", "2027-02-29", false).toModelType(List.of(GIG)));
    }
}
