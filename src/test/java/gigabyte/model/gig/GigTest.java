package gigabyte.model.gig;

import static gigabyte.testutil.Assert.assertThrows;
import static gigabyte.testutil.TypicalClients.ALICE;
import static gigabyte.testutil.TypicalClients.BENSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

public class GigTest {
    private static final Deadline DEADLINE = new Deadline("2027-01-31");
    private static final Fee FEE = new Fee("1250.00");

    @Test
    public void constructor_nullField_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () ->
                new Gig(null, GigStatus.NOT_STARTED, DEADLINE, FEE));
        assertThrows(NullPointerException.class, () ->
                new Gig(ALICE, null, DEADLINE, FEE));
        assertThrows(NullPointerException.class, () ->
                new Gig(ALICE, GigStatus.NOT_STARTED, null, FEE));
        assertThrows(NullPointerException.class, () ->
                new Gig(ALICE, GigStatus.NOT_STARTED, DEADLINE, null));
    }

    @Test
    public void constructor_validFields_createsGig() {
        Gig gig = new Gig(ALICE, GigStatus.IN_PROGRESS, DEADLINE, FEE);

        assertSame(ALICE, gig.getClient());
        assertEquals(GigStatus.IN_PROGRESS, gig.getStatus());
        assertEquals(DEADLINE, gig.getDeadline());
        assertEquals(FEE, gig.getAgreedFee());
    }

    @Test
    public void withClient_validClient_returnsCopyWithReplacementClient() {
        Gig original = new Gig(ALICE, GigStatus.IN_PROGRESS, DEADLINE, FEE);

        Gig updated = original.withClient(BENSON);

        assertSame(BENSON, updated.getClient());
        assertEquals(original.getStatus(), updated.getStatus());
        assertEquals(original.getDeadline(), updated.getDeadline());
        assertEquals(original.getAgreedFee(), updated.getAgreedFee());
    }
}
