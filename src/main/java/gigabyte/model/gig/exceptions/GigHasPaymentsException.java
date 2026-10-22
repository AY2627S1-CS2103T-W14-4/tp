package gigabyte.model.gig.exceptions;

/**
 * Signals that a gig cannot be deleted while payment obligations refer to it.
 */
public class GigHasPaymentsException extends RuntimeException {
    public static final String MESSAGE =
            "This gig has payment obligations. Delete those obligations first using deletepayment.";

    public GigHasPaymentsException() {
        super(MESSAGE);
    }
}
