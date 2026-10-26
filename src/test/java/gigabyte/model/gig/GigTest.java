package gigabyte.model.gig;

import static gigabyte.testutil.Assert.assertThrows;
import static gigabyte.testutil.TypicalClients.ALICE;
import static gigabyte.testutil.TypicalClients.BENSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

public class GigTest {
    private static final GigTitle TITLE = new GigTitle("Website redesign");
    private static final Deadline DEADLINE = new Deadline("2027-01-31");
    private static final Fee FEE = new Fee("1250.00");

    @Test
    public void constructor_nullField_throwsNullPointerException() {
        // One null field at a time
        assertThrows(NullPointerException.class, () ->
                new Gig(null, TITLE, GigStatus.NOT_STARTED, DEADLINE, FEE)); // client
        assertThrows(NullPointerException.class, () ->
                new Gig(ALICE, null, GigStatus.NOT_STARTED, DEADLINE, FEE)); // title
        assertThrows(NullPointerException.class, () ->
                new Gig(ALICE, TITLE, null, DEADLINE, FEE)); // status
        assertThrows(NullPointerException.class, () ->
                new Gig(ALICE, TITLE, GigStatus.NOT_STARTED, null, FEE)); // deadline
        assertThrows(NullPointerException.class, () ->
                new Gig(ALICE, TITLE, GigStatus.NOT_STARTED, DEADLINE, null)); // fee
    }

    @Test
    public void constructor_validFields_createsGig() {
        Gig gig = new Gig(ALICE, TITLE, GigStatus.IN_PROGRESS, DEADLINE, FEE);

        assertSame(ALICE, gig.getClient());
        assertEquals(TITLE, gig.getTitle());
        assertEquals(GigStatus.IN_PROGRESS, gig.getStatus());
        assertEquals(DEADLINE, gig.getDeadline());
        assertEquals(FEE, gig.getAgreedFee());
    }

    @Test
    public void withClient_validClient_returnsCopyWithReplacementClient() {
        Gig original = new Gig(ALICE, TITLE, GigStatus.IN_PROGRESS, DEADLINE, FEE);

        Gig updated = original.withClient(BENSON);

        assertSame(BENSON, updated.getClient());
        assertEquals(TITLE, updated.getTitle());
        assertEquals(original.getStatus(), updated.getStatus());
        assertEquals(original.getDeadline(), updated.getDeadline());
        assertEquals(original.getAgreedFee(), updated.getAgreedFee());
    }

    @Test
    public void withStatus_validStatus_returnsCopyPreservingIdentityAndOtherFields() {
        Gig original = new Gig(ALICE, TITLE, GigStatus.NOT_STARTED, DEADLINE, FEE);

        Gig updated = original.withStatus(GigStatus.COMPLETED);

        assertEquals(original.getUid(), updated.getUid());
        assertSame(original.getClient(), updated.getClient());
        assertEquals(TITLE, updated.getTitle());
        assertEquals(GigStatus.COMPLETED, updated.getStatus());
        assertEquals(DEADLINE, updated.getDeadline());
        assertEquals(FEE, updated.getAgreedFee());
    }

    @Test
    public void withStatus_nullStatus_throwsNullPointerException() {
        Gig gig = new Gig(ALICE, TITLE, GigStatus.NOT_STARTED, DEADLINE, FEE);

        assertThrows(NullPointerException.class, () -> gig.withStatus(null));
    }

    @Test
    public void withDetails_validDetails_preservesIdentityClientAndStatus() {
        Gig original = new Gig(ALICE, TITLE, GigStatus.COMPLETED, DEADLINE, FEE);

        Gig edited = original.withDetails(new GigTitle("New title"), new Deadline("2027-04-30"), new Fee("1800"));

        // Unchanged: identity, client, and status
        assertEquals(original.getUid(), edited.getUid());
        assertSame(ALICE, edited.getClient());
        assertEquals(GigStatus.COMPLETED, edited.getStatus());

        // Changed: title, deadline, and fee
        assertEquals(new GigTitle("New title"), edited.getTitle());
        assertEquals(new Deadline("2027-04-30"), edited.getDeadline());
        assertEquals(new Fee("1800"), edited.getAgreedFee());
    }

    @Test
    public void withDetails_nullDetail_throwsNullPointerException() {
        Gig original = new Gig(ALICE, TITLE, GigStatus.COMPLETED, DEADLINE, FEE);

        // One null detail at a time
        assertThrows(NullPointerException.class, () -> original.withDetails(null, DEADLINE, FEE)); // title
        assertThrows(NullPointerException.class, () -> original.withDetails(TITLE, null, FEE)); // deadline
        assertThrows(NullPointerException.class, () -> original.withDetails(TITLE, DEADLINE, null)); // fee
    }

    @Test
    public void withNewUid_returnsCopyWithReplacementUidAndSameTitle() {
        Gig original = new Gig(ALICE, TITLE, GigStatus.IN_PROGRESS, DEADLINE, FEE);

        Gig updated = original.withNewUid();

        assertNotEquals(original.getUid(), updated.getUid());
        assertEquals(TITLE, updated.getTitle());
    }
}
