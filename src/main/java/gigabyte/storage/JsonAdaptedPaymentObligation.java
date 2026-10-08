package gigabyte.storage;

import java.util.List;
import java.util.stream.IntStream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import gigabyte.commons.exceptions.IllegalValueException;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.PaymentObligation;
import gigabyte.model.gig.exceptions.GigNotFoundException;

/**
 * Stores a payment obligation and a one-based reference to its gig in the saved gig list.
 */
class JsonAdaptedPaymentObligation {
    public static final String MISSING_FIELD_MESSAGE =
            "A payment obligation must have a gigIndex, amount, dueDate, and paid state.";

    private final Integer gigIndex;
    private final String amount;
    private final String dueDate;
    private final Boolean paid;

    /**
     * Creates a JSON-friendly payment obligation from its saved fields.
     */
    @JsonCreator
    public JsonAdaptedPaymentObligation(@JsonProperty("gigIndex") Integer gigIndex,
            @JsonProperty("amount") String amount, @JsonProperty("dueDate") String dueDate,
            @JsonProperty("paid") Boolean paid) {
        this.gigIndex = gigIndex;
        this.amount = amount;
        this.dueDate = dueDate;
        this.paid = paid;
    }

    /**
     * Copies {@code obligation} for serialization, retaining its exact gig reference.
     */
    public JsonAdaptedPaymentObligation(PaymentObligation obligation, List<Gig> gigs) {
        gigIndex = IntStream.range(0, gigs.size()).filter(index -> gigs.get(index) == obligation.getGig())
                .findFirst().orElseThrow(GigNotFoundException::new) + 1;
        amount = obligation.getAmount().toString();
        dueDate = obligation.getDueDate().toString();
        paid = obligation.isPaid();
    }

    /**
     * Restores an obligation linked to the canonical saved gig.
     *
     * @throws IllegalValueException if its fields are invalid or its gig does not exist.
     */
    public PaymentObligation toModelType(List<Gig> gigs) throws IllegalValueException {
        if (gigIndex == null || amount == null || dueDate == null || paid == null) {
            throw new IllegalValueException(MISSING_FIELD_MESSAGE);
        }
        if (gigIndex < 1 || gigIndex > gigs.size()) {
            throw new IllegalValueException(GigNotFoundException.MESSAGE);
        }
        if (!Fee.isValidFee(amount)) {
            throw new IllegalValueException(Fee.MESSAGE_CONSTRAINTS);
        }
        if (!Deadline.isValidDeadline(dueDate)) {
            throw new IllegalValueException(Deadline.MESSAGE_CONSTRAINTS);
        }
        return new PaymentObligation(gigs.get(gigIndex - 1), new Fee(amount), new Deadline(dueDate), paid);
    }
}
