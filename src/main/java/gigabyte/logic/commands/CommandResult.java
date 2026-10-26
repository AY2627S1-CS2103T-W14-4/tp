package gigabyte.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

import gigabyte.commons.util.ToStringBuilder;
import gigabyte.model.gig.GigStatus;
import gigabyte.model.gig.GigTitleContainsKeywordsPredicate;

/**
 * Represents the result of a command execution.
 */
public class CommandResult {

    private final String feedbackToUser;

    /** Help information should be shown to the user. */
    private final boolean shouldShowHelp;

    /** The application should exit. */
    private final boolean shouldExit;

    /** Status to display across all clients, or null when the gig view should remain unchanged. */
    private final GigStatus gigStatusFilter;
    private final boolean shouldShowDeadlineOverview;

    /**
     * Unpaid obligations across all clients should be shown.
     */
    private final boolean shouldShowOutstandingPayments;
    private final GigTitleContainsKeywordsPredicate gigTitleSearch;

    /**
     * Constructs a {@code CommandResult} with the specified fields.
     */
    public CommandResult(String feedbackToUser, boolean shouldShowHelp, boolean shouldExit) {
        this(feedbackToUser, shouldShowHelp, shouldExit, null, false, false, null);
    }

    private CommandResult(String feedbackToUser, boolean shouldShowHelp, boolean shouldExit,
            GigStatus gigStatusFilter, boolean shouldShowDeadlineOverview,
            boolean shouldShowOutstandingPayments, GigTitleContainsKeywordsPredicate gigTitleSearch) {
        this.feedbackToUser = requireNonNull(feedbackToUser);
        this.shouldShowHelp = shouldShowHelp;
        this.shouldExit = shouldExit;
        this.gigStatusFilter = gigStatusFilter;
        this.shouldShowOutstandingPayments = shouldShowOutstandingPayments;
        this.shouldShowDeadlineOverview = shouldShowDeadlineOverview;
        this.gigTitleSearch = gigTitleSearch;
    }

    /**
     * Constructs a result requesting a gig-status view across all clients.
     */
    public CommandResult(String feedbackToUser, GigStatus gigStatusFilter) {
        this(feedbackToUser, false, false, requireNonNull(gigStatusFilter), false, false, null);
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
        return new CommandResult(feedbackToUser, false, false, null, false, true, null);
    }

    /**
     * Constructs a result requesting the active deadline overview.
     */
    public static CommandResult deadlineOverview(String feedbackToUser) {
        return new CommandResult(requireNonNull(feedbackToUser), false, false, null, true, false, null);
    }

    /**
     * Creates a result requesting a gig-title search view using {@code predicate}.
     */
    public static CommandResult createGigTitleSearchResult(String feedbackToUser,
            GigTitleContainsKeywordsPredicate predicate) {
        return new CommandResult(feedbackToUser, false, false, null, false, false, requireNonNull(predicate));
    }

    public String getFeedbackToUser() {
        return feedbackToUser;
    }

    public boolean shouldShowHelp() {
        return shouldShowHelp;
    }

    public boolean shouldExit() {
        return shouldExit;
    }

    public GigStatus getGigStatusFilter() {
        return gigStatusFilter;
    }

    public boolean shouldShowOutstandingPayments() {
        return shouldShowOutstandingPayments;
    }

    public boolean shouldShowDeadlineOverview() {
        return shouldShowDeadlineOverview;
    }

    public GigTitleContainsKeywordsPredicate getGigTitleSearch() {
        return gigTitleSearch;
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
                && shouldShowHelp == otherCommandResult.shouldShowHelp
                && shouldExit == otherCommandResult.shouldExit
                && gigStatusFilter == otherCommandResult.gigStatusFilter
                && shouldShowOutstandingPayments == otherCommandResult.shouldShowOutstandingPayments
                && shouldShowDeadlineOverview == otherCommandResult.shouldShowDeadlineOverview
                && Objects.equals(gigTitleSearch, otherCommandResult.gigTitleSearch);
    }

    @Override
    public int hashCode() {
        return Objects.hash(feedbackToUser, shouldShowHelp, shouldExit, gigStatusFilter,
                shouldShowOutstandingPayments, shouldShowDeadlineOverview, gigTitleSearch);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("feedbackToUser", feedbackToUser)
                .add("showHelp", shouldShowHelp)
                .add("exit", shouldExit)
                .add("gigStatusFilter", gigStatusFilter)
                .add("showOutstandingPayments", shouldShowOutstandingPayments)
                .add("showDeadlineOverview", shouldShowDeadlineOverview)
                .add("gigTitleSearch", gigTitleSearch)
                .toString();
    }

}
