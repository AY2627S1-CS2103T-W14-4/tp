package gigabyte.storage;

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import gigabyte.commons.exceptions.IllegalValueException;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.PaymentObligation;
import gigabyte.model.gig.exceptions.GigNotFoundException;

/** Stores a payment obligation linked to a gig's stable ID. */
class JsonAdaptedPaymentObligation {
    public static final String MISSING_FIELD_MESSAGE =
            "A payment obligation must have a gigUid, amountCents, dueDate, and paid state.";

    private final String gigUid;
    private final BigInteger amountCents;
    private final String dueDate;
    private final Boolean paid;

    /** Reads the current format with a stable gig ID and an amount in cents. */
    @JsonCreator
    public JsonAdaptedPaymentObligation(@JsonProperty("gigUid") String gigUid,
            @JsonProperty("amountCents") BigInteger amountCents, @JsonProperty("dueDate") String dueDate,
            @JsonProperty("paid") Boolean paid) {
        this.gigUid = gigUid;
        this.amountCents = amountCents;
        this.dueDate = dueDate;
        this.paid = paid;
    }

    /** Copies {@code obligation} for serialization, retaining its exact gig reference. */
    public JsonAdaptedPaymentObligation(PaymentObligation obligation, List<Gig> gigs) {
        if (gigs.stream().noneMatch(candidate -> candidate.hasSameUid(obligation.getGig()))) {
            throw new GigNotFoundException();
        }
        gigUid = obligation.getGig().getUid().toString();
        amountCents = obligation.getAmount().getCents();
        dueDate = obligation.getDueDate().toString();
        paid = obligation.isPaid();
    }

    /** Restores an obligation linked to its canonical saved gig. */
    public PaymentObligation toModelType(List<Gig> gigs) throws IllegalValueException {
        if (gigUid == null || amountCents == null || dueDate == null || paid == null) {
            throw new IllegalValueException(MISSING_FIELD_MESSAGE);
        }

        UUID modelGigUid;
        try {
            modelGigUid = UUID.fromString(gigUid);
        } catch (IllegalArgumentException exception) {
            throw new IllegalValueException(GigNotFoundException.MESSAGE);
        }
        Gig gig = gigs.stream()
                .filter(candidate -> candidate.getUid().equals(modelGigUid))
                .findFirst()
                .orElseThrow(() -> new IllegalValueException(GigNotFoundException.MESSAGE));
        if (amountCents.signum() <= 0) {
            throw new IllegalValueException(Fee.MESSAGE_CONSTRAINTS);
        }
        Fee amount = Fee.fromCents(amountCents);

        if (!Deadline.isValidDeadline(dueDate)) {
            throw new IllegalValueException(Deadline.MESSAGE_CONSTRAINTS);
        }
        return new PaymentObligation(gig, amount, new Deadline(dueDate), paid);
    }
}
