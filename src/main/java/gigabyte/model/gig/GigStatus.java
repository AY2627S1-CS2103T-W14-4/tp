package gigabyte.model.gig;

import static java.util.Objects.requireNonNull;

import java.util.Locale;

/**
 * Describes the current progress of a gig.
 */
public enum GigStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED;

    public static final String MESSAGE_CONSTRAINTS = "Statuses must be NOT_STARTED, IN_PROGRESS, or COMPLETED";

    /**
     * Parses a status, ignoring case and surrounding whitespace.
     *
     * @throws IllegalArgumentException if {@code status} is not a supported status.
     */
    public static GigStatus parse(String status) {
        requireNonNull(status);
        try {
            return valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(MESSAGE_CONSTRAINTS, exception);
        }
    }
}
