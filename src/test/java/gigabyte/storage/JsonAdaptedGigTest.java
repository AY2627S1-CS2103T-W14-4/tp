package gigabyte.storage;

import static gigabyte.testutil.Assert.assertThrows;
import static gigabyte.testutil.TypicalClients.ALICE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigInteger;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;

import gigabyte.commons.exceptions.IllegalValueException;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.GigStatus;
import gigabyte.model.gig.GigTitle;
import gigabyte.model.gig.exceptions.GigClientNotFoundException;

public class JsonAdaptedGigTest {
    private static final String UID = "00000000-0000-0000-0000-000000000001";
    private static final String CLIENT_UID = ALICE.getUid().toString();
    private static final JsonNode TITLE = TextNode.valueOf("Website redesign");
    private static final String STATUS = "NOT_STARTED";
    private static final String DEADLINE = "2027-01-31";
    private static final BigInteger FEE_CENTS = BigInteger.valueOf(125050);

    @Test
    public void toModelType_validFields_restoresGigWithCanonicalClient() throws Exception {
        Gig original = new Gig(ALICE, new GigTitle("Website redesign"), GigStatus.IN_PROGRESS,
                new Deadline(DEADLINE), Fee.fromCents(FEE_CENTS));
        Gig restored = new JsonAdaptedGig(original).toModelType(List.of(ALICE));
        assertEquals(original, restored);
        assertEquals(original.getUid(), restored.getUid());
        assertSame(ALICE, restored.getClient());
    }

    @Test
    public void toModelType_invalidPresentTitle_throwsIllegalValueException() {
        // EP: title present in the file but not text
        assertTitleRejected(NullNode.getInstance()); // JSON null
        assertTitleRejected(IntNode.valueOf(123));

        // EP: blank title
        assertTitleRejected(TextNode.valueOf("   "));

        // EP: title longer than 100 characters
        assertTitleRejected(TextNode.valueOf("a".repeat(101))); // Boundary value
    }

    @Test
    public void toModelType_missingFields_throwsIllegalValueException() {
        for (JsonAdaptedGig gig : List.of(
                new JsonAdaptedGig(null, CLIENT_UID, TITLE, STATUS, DEADLINE, FEE_CENTS), // UID
                new JsonAdaptedGig(UID, null, TITLE, STATUS, DEADLINE, FEE_CENTS), // client UID
                new JsonAdaptedGig(UID, CLIENT_UID, null, STATUS, DEADLINE, FEE_CENTS), // title
                new JsonAdaptedGig(UID, CLIENT_UID, TITLE, null, DEADLINE, FEE_CENTS), // status
                new JsonAdaptedGig(UID, CLIENT_UID, TITLE, STATUS, null, FEE_CENTS), // deadline
                new JsonAdaptedGig(UID, CLIENT_UID, TITLE, STATUS, DEADLINE, null))) { // fee
            assertThrows(IllegalValueException.class, JsonAdaptedGig.MISSING_FIELD_MESSAGE, () ->
                    gig.toModelType(List.of(ALICE)));
        }
    }

    @Test
    public void toModelType_invalidUid_throwsIllegalValueException() {
        assertThrows(IllegalValueException.class, JsonAdaptedGig.INVALID_UID_MESSAGE, () ->
                new JsonAdaptedGig("invalid", CLIENT_UID, TITLE, STATUS, DEADLINE, FEE_CENTS)
                        .toModelType(List.of(ALICE)));
    }

    @Test
    public void toModelType_invalidOrUnknownClientUid_throwsIllegalValueException() {
        // EP: client UID is not a valid UUID
        assertClientUidRejected("invalid");

        // EP: valid UUID that matches no stored client
        assertClientUidRejected("00000000-0000-0000-0000-000000000002");
    }

    @Test
    public void toModelType_unknownStatus_throwsIllegalValueException() {
        assertThrows(IllegalValueException.class, GigStatus.MESSAGE_CONSTRAINTS, () ->
                new JsonAdaptedGig(UID, CLIENT_UID, TITLE, "UNKNOWN", DEADLINE, FEE_CENTS)
                        .toModelType(List.of(ALICE)));
    }

    @Test
    public void toModelType_nonExistentDeadline_throwsIllegalValueException() {
        assertThrows(IllegalValueException.class, Deadline.MESSAGE_CONSTRAINTS, () ->
                new JsonAdaptedGig(UID, CLIENT_UID, TITLE, STATUS, "2027-02-29", FEE_CENTS)
                        .toModelType(List.of(ALICE))); // Not a leap year
    }

    @Test
    public void toModelType_zeroOrNegativeFee_throwsIllegalValueException() {
        assertFeeRejected(BigInteger.ZERO); // Boundary value
        assertFeeRejected(BigInteger.valueOf(-1));
    }

    private void assertTitleRejected(JsonNode title) {
        assertThrows(IllegalValueException.class, GigTitle.MESSAGE_CONSTRAINTS, () ->
                new JsonAdaptedGig(UID, CLIENT_UID, title, STATUS, DEADLINE, FEE_CENTS)
                        .toModelType(List.of(ALICE)));
    }

    private void assertClientUidRejected(String clientUid) {
        assertThrows(IllegalValueException.class, GigClientNotFoundException.MESSAGE, () ->
                new JsonAdaptedGig(UID, clientUid, TITLE, STATUS, DEADLINE, FEE_CENTS)
                        .toModelType(List.of(ALICE)));
    }

    private void assertFeeRejected(BigInteger feeCents) {
        assertThrows(IllegalValueException.class, Fee.MESSAGE_CONSTRAINTS, () ->
                new JsonAdaptedGig(UID, CLIENT_UID, TITLE, STATUS, DEADLINE, feeCents)
                        .toModelType(List.of(ALICE)));
    }
}
