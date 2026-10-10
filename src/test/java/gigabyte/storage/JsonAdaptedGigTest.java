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
import gigabyte.model.gig.GigTitle;
import gigabyte.model.gig.exceptions.GigClientNotFoundException;

public class JsonAdaptedGigTest {
    @Test
    public void toModelType_validFields_restoresGigWithCanonicalClient() throws Exception {
        Gig original = new Gig(ALICE, new GigTitle("Website redesign"), GigStatus.IN_PROGRESS,
                new Deadline("2027-01-31"), new Fee("1250.50"));
        Gig restored = new JsonAdaptedGig(original).toModelType(List.of(ALICE));
        assertEquals(original, restored);
        assertSame(ALICE, restored.getClient());
    }

    @Test
    public void toModelType_absentTitle_usesUntitledFallback() throws Exception {
        Gig restored = new JsonAdaptedGig(ALICE.getName().fullName, "NOT_STARTED", "2027-01-31", "100")
                .toModelType(List.of(ALICE));

        assertEquals(GigTitle.UNTITLED, restored.getTitle());
    }

    @Test
    public void toModelType_invalidPresentTitle_throwsIllegalValueException() {
        assertThrows(IllegalValueException.class, GigTitle.MESSAGE_CONSTRAINTS, () ->
                new JsonAdaptedGig(ALICE.getName().fullName, "   ", "NOT_STARTED", "2027-01-31", "100")
                        .toModelType(List.of(ALICE)));
        assertThrows(IllegalValueException.class, GigTitle.MESSAGE_CONSTRAINTS, () ->
                new JsonAdaptedGig(ALICE.getName().fullName, "a".repeat(101), "NOT_STARTED", "2027-01-31", "100")
                        .toModelType(List.of(ALICE)));
    }

    @Test
    public void toModelType_missingFields_throwsIllegalValueException() {
        for (JsonAdaptedGig gig : List.of(
                new JsonAdaptedGig(null, "NOT_STARTED", "2027-01-31", "100"),
                new JsonAdaptedGig(ALICE.getName().fullName, null, "2027-01-31", "100"),
                new JsonAdaptedGig(ALICE.getName().fullName, "NOT_STARTED", null, "100"),
                new JsonAdaptedGig(ALICE.getName().fullName, "NOT_STARTED", "2027-01-31", null))) {
            assertThrows(IllegalValueException.class, JsonAdaptedGig.MISSING_FIELD_MESSAGE, () ->
                    gig.toModelType(List.of(ALICE)));
        }
    }

    @Test
    public void toModelType_unknownClient_throwsIllegalValueException() {
        JsonAdaptedGig gig = new JsonAdaptedGig("Unknown Client", "NOT_STARTED", "2027-01-31", "100");
        assertThrows(IllegalValueException.class, GigClientNotFoundException.MESSAGE, () ->
                gig.toModelType(List.of(ALICE)));
    }

    @Test
    public void toModelType_invalidFields_throwsIllegalValueException() {
        assertThrows(IllegalValueException.class, GigStatus.MESSAGE_CONSTRAINTS, () ->
                new JsonAdaptedGig(ALICE.getName().fullName, "UNKNOWN", "2027-01-31", "100")
                        .toModelType(List.of(ALICE)));
        assertThrows(IllegalValueException.class, Deadline.MESSAGE_CONSTRAINTS, () ->
                new JsonAdaptedGig(ALICE.getName().fullName, "NOT_STARTED", "2027-02-29", "100")
                        .toModelType(List.of(ALICE)));
        assertThrows(IllegalValueException.class, Fee.MESSAGE_CONSTRAINTS, () ->
                new JsonAdaptedGig(ALICE.getName().fullName, "NOT_STARTED", "2027-01-31", "0")
                        .toModelType(List.of(ALICE)));
    }
}
