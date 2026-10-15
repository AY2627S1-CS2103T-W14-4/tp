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
    private final boolean showDeadlineOverview;

    /**
     * Constructs a {@code CommandResult} with the specified fields.
     */
    public CommandResult(String feedbackToUser, boolean showHelp, boolean exit) {
        this(feedbackToUser, showHelp, exit, null, false);
    }

    private CommandResult(String feedbackToUser, boolean showHelp, boolean exit, GigStatus gigStatusFilter,
                          boolean showDeadlineOverview) {
        this.feedbackToUser = requireNonNull(feedbackToUser);
        this.showHelp = showHelp;
        this.exit = exit;
        this.gigStatusFilter = gigStatusFilter;
        this.showDeadlineOverview = showDeadlineOverview;
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

    /** Constructs a result requesting the active deadline overview. */
    public static CommandResult deadlineOverview(String feedbackToUser) {
        return new CommandResult(requireNonNull(feedbackToUser), false, false, null, true);
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

    public boolean isShowDeadlineOverview() {
        return showDeadlineOverview;
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
                && showDeadlineOverview == otherCommandResult.showDeadlineOverview;
    }

    @Override
    public int hashCode() {
        return Objects.hash(feedbackToUser, showHelp, exit, gigStatusFilter, showDeadlineOverview);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("feedbackToUser", feedbackToUser)
                .add("showHelp", showHelp)
                .add("exit", exit)
                .add("gigStatusFilter", gigStatusFilter)
                .toString();
    }

}
