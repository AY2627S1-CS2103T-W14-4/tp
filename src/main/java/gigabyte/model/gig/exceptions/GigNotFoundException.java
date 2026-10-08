package gigabyte.model.gig.exceptions;

/** Signals that a payment obligation refers to a gig not in Gigabyte. */
public class GigNotFoundException extends RuntimeException {
    public static final String MESSAGE = "Payment obligation refers to a gig that does not exist";
    public GigNotFoundException() { super(MESSAGE); }
}
