package gigabyte.logic.commands;

import static java.util.Objects.requireNonNull;

import gigabyte.commons.util.ToStringBuilder;
import gigabyte.model.Model;
import gigabyte.model.gig.GigStatus;

/**
 * Displays gigs with the requested status across all clients without changing records.
 */
public class FilterGigsCommand extends Command {
    public static final String COMMAND_WORD = "gigs";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Lists gigs across all clients by status.\n"
            + "Parameters: s/STATUS\nExample: " + COMMAND_WORD + " s/IN_PROGRESS";

    private final GigStatus status;

    /**
     * Creates a command filtering gigs by {@code status}.
     */
    public FilterGigsCommand(GigStatus status) {
        this.status = requireNonNull(status);
    }

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        long count = model.getGigList().stream().filter(gig -> gig.getStatus() == status).count();
        return new CommandResult(String.format(
                "%d gig%s with status %s.", count, count == 1 ? "" : "s", status), status);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        return other instanceof FilterGigsCommand otherCommand && status == otherCommand.status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("status", status).toString();
    }
}
