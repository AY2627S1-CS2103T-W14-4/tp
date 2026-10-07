package gigabyte.model.gig.exceptions;

/**
 * Signals that a gig refers to a client that is not in Gigabyte.
 */
public class GigClientNotFoundException extends RuntimeException {
    public static final String MESSAGE =
            "Gig refers to a client that does not exist";

    public GigClientNotFoundException() {
        super(MESSAGE);
    }
}
