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
    @JsonProperty(value = "gigIndex", access = JsonProperty.Access.WRITE_ONLY)
    private final Integer legacyGigIndex;
    private final BigInteger amountCents;
    @JsonProperty(value = "amount", access = JsonProperty.Access.WRITE_ONLY)
    private final String legacyAmount;
    private final String dueDate;
    private final Boolean paid;

    /** Reads the stable-ID format and the previous index-based format. */
    @JsonCreator
    public JsonAdaptedPaymentObligation(@JsonProperty("gigUid") String gigUid,
            @JsonProperty("gigIndex") Integer gigIndex, @JsonProperty("amountCents") BigInteger amountCents,
            @JsonProperty("amount") String amount, @JsonProperty("dueDate") String dueDate,
            @JsonProperty("paid") Boolean paid) {
        this.gigUid = gigUid;
        this.legacyGigIndex = gigIndex;
        this.amountCents = amountCents;
        this.legacyAmount = amount;
        this.dueDate = dueDate;
        this.paid = paid;
    }

    /** Retains the previous constructor for legacy data and tests. */
    public JsonAdaptedPaymentObligation(Integer gigIndex, String amount, String dueDate, Boolean paid) {
        this(null, gigIndex, null, amount, dueDate, paid);
    }

    /** Copies {@code obligation} for serialization, retaining its exact gig reference. */
    public JsonAdaptedPaymentObligation(PaymentObligation obligation, List<Gig> gigs) {
        if (gigs.stream().noneMatch(candidate -> candidate.hasSameUid(obligation.getGig()))) {
            throw new GigNotFoundException();
        }
        gigUid = obligation.getGig().getUid().toString();
        legacyGigIndex = null;
        amountCents = obligation.getAmount().getCents();
        legacyAmount = null;
        dueDate = obligation.getDueDate().toString();
        paid = obligation.isPaid();
    }

    /** Restores an obligation linked to its canonical saved gig. */
    public PaymentObligation toModelType(List<Gig> gigs) throws IllegalValueException {
        if (dueDate == null || paid == null) {
            throw new IllegalValueException(MISSING_FIELD_MESSAGE);
        }

        Gig gig;
        Fee amount;
        if (gigUid != null) {
            UUID modelGigUid;
            try {
                modelGigUid = UUID.fromString(gigUid);
            } catch (IllegalArgumentException exception) {
                throw new IllegalValueException(GigNotFoundException.MESSAGE);
            }
            if (amountCents == null) {
                throw new IllegalValueException(MISSING_FIELD_MESSAGE);
            }
            gig = gigs.stream()
                    .filter(candidate -> candidate.getUid().equals(modelGigUid))
                    .findFirst()
                    .orElseThrow(() -> new IllegalValueException(GigNotFoundException.MESSAGE));
            if (amountCents.signum() <= 0) {
                throw new IllegalValueException(Fee.MESSAGE_CONSTRAINTS);
            }
            amount = Fee.fromCents(amountCents);
        } else {
            if (legacyGigIndex == null || legacyAmount == null) {
                throw new IllegalValueException(MISSING_FIELD_MESSAGE);
            }
            if (legacyGigIndex < 1 || legacyGigIndex > gigs.size()) {
                throw new IllegalValueException(GigNotFoundException.MESSAGE);
            }
            if (!Fee.isValidFee(legacyAmount)) {
                throw new IllegalValueException(Fee.MESSAGE_CONSTRAINTS);
            }
            gig = gigs.get(legacyGigIndex - 1);
            amount = new Fee(legacyAmount);
        }

        if (!Deadline.isValidDeadline(dueDate)) {
            throw new IllegalValueException(Deadline.MESSAGE_CONSTRAINTS);
        }
        return new PaymentObligation(gig, amount, new Deadline(dueDate), paid);
    }
}
