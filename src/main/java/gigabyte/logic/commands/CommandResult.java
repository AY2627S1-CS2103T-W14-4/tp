package gigabyte.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

import gigabyte.commons.util.ToStringBuilder;
import gigabyte.model.gig.GigStatus;

/**
 * Represents the result of a command execution.
 */
public class CommandResult {

    private final String feedbackToUser;

    /** Help information should be shown to the user. */
    private final boolean showHelp;

    /** The application should exit. */
    private final boolean exit;

    /** Status to display across all clients, or null when the gig view should remain unchanged. */
    private final GigStatus gigStatusFilter;

    /**
     * Unpaid obligations across all clients should be shown.
     */
    private final boolean showOutstandingPayments;

    /**
     * Constructs a {@code CommandResult} with the specified fields.
     */
    public CommandResult(String feedbackToUser, boolean showHelp, boolean exit) {
        this(feedbackToUser, showHelp, exit, null, false);
    }

    private CommandResult(String feedbackToUser, boolean showHelp, boolean exit, GigStatus gigStatusFilter,
            boolean showOutstandingPayments) {
        this.feedbackToUser = requireNonNull(feedbackToUser);
        this.showHelp = showHelp;
        this.exit = exit;
        this.gigStatusFilter = gigStatusFilter;
        this.showOutstandingPayments = showOutstandingPayments;
    }

    /**
     * Constructs a result requesting a gig-status view across all clients.
     */
    public CommandResult(String feedbackToUser, GigStatus gigStatusFilter) {
        this(feedbackToUser, false, false, requireNonNull(gigStatusFilter), false);
    }

    /**
     * Constructs a {@code CommandResult} with the specified {@code feedbackToUser},
     * and other fields set to their default value.
     */
    public CommandResult(String feedbackToUser) {
        this(feedbackToUser, false, false);
    }

    /**
     * Creates a result requesting an outstanding-payment view across all clients.
     */
    public static CommandResult forOutstandingPayments(String feedbackToUser) {
        return new CommandResult(feedbackToUser, false, false, null, true);
    }

    public String getFeedbackToUser() {
        return feedbackToUser;
    }

    public boolean isShowHelp() {
        return showHelp;
    }

    public boolean isExit() {
        return exit;
    }

    public GigStatus getGigStatusFilter() {
        return gigStatusFilter;
    }

    public boolean isShowOutstandingPayments() {
        return showOutstandingPayments;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof CommandResult otherCommandResult)) {
            return false;
        }

        return feedbackToUser.equals(otherCommandResult.feedbackToUser)
                && showHelp == otherCommandResult.showHelp
                && exit == otherCommandResult.exit
                && gigStatusFilter == otherCommandResult.gigStatusFilter
                && showOutstandingPayments == otherCommandResult.showOutstandingPayments;
    }

    @Override
    public int hashCode() {
        return Objects.hash(feedbackToUser, showHelp, exit, gigStatusFilter, showOutstandingPayments);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("feedbackToUser", feedbackToUser)
                .add("showHelp", showHelp)
                .add("exit", exit)
                .add("gigStatusFilter", gigStatusFilter)
                .add("showOutstandingPayments", showOutstandingPayments)
                .toString();
    }

}
