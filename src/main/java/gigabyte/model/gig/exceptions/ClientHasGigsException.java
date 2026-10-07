package gigabyte.model.gig.exceptions;

/**
 * Signals that a client cannot be deleted while gigs still refer to it.
 */
public class ClientHasGigsException extends RuntimeException {
    public static final String MESSAGE =
            "Client cannot be deleted while it has associated gigs";

    public ClientHasGigsException() {
        super(MESSAGE);
    }
}
